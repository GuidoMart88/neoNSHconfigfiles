package eu.neonsh.nshmanager.impl.DataListeners;

import eu.neonsh.nshmanager.impl.Entities.*;
import eu.neonsh.nshmanager.impl.SFCManagerUtils;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.Actions1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.SfcAction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.acl.rev151001.access.lists.acl.access.list.entries.ace.actions.sfc.action.AclRenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Future;

public class AclEntryDataListener extends AbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(AclEntryDataListener.class);
    private vtnServices vtnServices;
    private sfcManagerLists lists;
    private HashMap<String,String> portmaps;

    org.opendaylight.controller.md.sal.binding.api.DataBroker databroker;

    public AclEntryDataListener(OpendaylightSfc opendaylightSfc, vtnServices vtnServices, sfcManagerLists lists) {
        this.portmaps=new HashMap<>();
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.ACL_ENTRY_IID);
        this.databroker = opendaylightSfc.getDataProvider();
        this.vtnServices = vtnServices;
        this.lists = lists;
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof Acl) {
                Acl originalAcl = (Acl) entry.getValue();
                LOG.info("\nOriginal Access List Name: {}", originalAcl.getAclName());
            }
        }

        // ACL CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof Acl) {
                Acl createdAcl = (Acl) entry.getValue();
                AccessList ACL = handleAcl(createdAcl);

//                try {
//                    //installRules(ACL);
//                } catch (NoSuchAlgorithmException e) {
//                    e.printStackTrace();
//                }
                lists.insertACL(ACL);

                LOG.info("\nCreated Access List Name: {}", createdAcl.getAclName());

            }
        }

        // ACL UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof Acl) && (!dataCreatedObject.containsKey(entry.getKey()))) {
                Acl updatedAcl = (Acl) entry.getValue();
                LOG.info("\nModified Access List Name: {}", updatedAcl.getAclName());

            }
        }

        // ACL DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof Acl) {

                Acl originalAcl = (Acl) dataObject;
                LOG.info("\nDeleted Access List Name: {}", originalAcl.getAclName());
                lists.deleteACLByName(originalAcl.getAclName());
            }
        }

    }

    private AccessList handleAcl(Acl createdAcl) {
        AccessList acl = new AccessList();
        acl.Name = createdAcl.getAclName();
//        acl.Type=createdAcl.getAclType().getCanonicalName();
        if (createdAcl.getAccessListEntries() != null) {

            List<Ace> aceList = createdAcl.getAccessListEntries().getAce();
            if (aceList != null) {
                for (Ace ace : aceList) {
                    acl.aces.add(handleAce(ace));

                }

            }
        }
        return acl;
    }

    private AccessListEntry handleAce(Ace ace) {
        AccessListEntry ACE = new AccessListEntry();
        ACE.aceName = ace.getRuleName();
        LOG.info("ACE name {}\n", ace.getRuleName());
        // tmpAce.Name=ace.getRuleName();
        Matches matches = ace.getMatches();
        if (matches.getAceType() != null) {
            String aceType = matches.getAceType().getImplementedInterface().getSimpleName();

            switch (aceType) {
                case "AceIp":
                    ACE.isIP = true;
                    LOG.info("ACE_IP\n");
                    AceIp aceIp = (AceIp) matches.getAceType();

                    if (aceIp.getDscp() != null) {
                        ACE.aceIPDscp = aceIp.getDscp().getValue();
                        LOG.info("ACE_IP DESCR {}\n", aceIp.getDscp().getValue());
                    }
                    ACE.aceIPProtocol = aceIp.getProtocol();
                    ACE.flowRepresentation+="dl_type=0x0800,nw_proto="+aceIp.getProtocol()+",";
//                    ACE.flowRepresentation=+
                    LOG.info("ACE_IP Protocol {}\n", aceIp.getProtocol());
                    SourcePortRange sourcePortRange = aceIp.getSourcePortRange();
                    if (sourcePortRange != null) {
                        if (sourcePortRange.getLowerPort() != null) {
                            ACE.aceIPSourceLowerPort = sourcePortRange.getLowerPort().getValue();
                            if(ACE.aceIPSourceLowerPort!=0){
                                ACE.flowRepresentation+="tp_src="+sourcePortRange.getLowerPort().getValue()+",";
                            }
                            LOG.info("ACE_IP source Lower port {}\n", sourcePortRange.getLowerPort().getValue());
                        }
                        if (sourcePortRange.getUpperPort() != null) {
                            ACE.aceIPSourceUpperPort = sourcePortRange.getUpperPort().getValue();
                            LOG.info("ACE_IP source Upper port {}\n", sourcePortRange.getUpperPort().getValue());
                        }
                    }
                    DestinationPortRange destinationPortRange = aceIp.getDestinationPortRange();
                    if (destinationPortRange != null) {

                        if (destinationPortRange.getLowerPort() != null) {
                            ACE.aceIPDestLowerPort = destinationPortRange.getLowerPort().getValue();
                            if(ACE.aceIPDestLowerPort!=0)
                            {
                                ACE.flowRepresentation+="tp_dst="+destinationPortRange.getLowerPort().getValue()+",";
                            }
                            LOG.info("ACE_IP destination Lower port {}\n", destinationPortRange.getLowerPort().getValue());
                        }
                        if (destinationPortRange.getUpperPort() != null) {
                            ACE.aceIPDestUpperPort = destinationPortRange.getUpperPort().getValue();
                            LOG.info("ACE_IP destination Lower port {}\n", destinationPortRange.getUpperPort().getValue());
                        }
                    }

                    if (aceIp.getAceIpVersion() != null) {
                        String aceIpVersion = aceIp.getAceIpVersion().getImplementedInterface().getSimpleName();

                        switch (aceIpVersion) {
                            case "AceIpv4":
                                ACE.isIPv4 = true;
                                AceIpv4 aceIpv4 = (AceIpv4) aceIp.getAceIpVersion();
                                if (aceIpv4.getDestinationIpv4Network() != null) {
                                    ACE.aceIPv4DestIP = aceIpv4.getDestinationIpv4Network().getValue();
                                    ACE.flowRepresentation+="nw_dst="+ACE.aceIPv4DestIP.substring(0, ACE.aceIPv4DestIP.lastIndexOf('/'))+",";
                                    LOG.info("ACE IPv4 Destination{}\n", aceIpv4.getDestinationIpv4Network().getValue());
                                }
                                if (aceIpv4.getSourceIpv4Network() != null) {
                                    ACE.aceIPv4SourceIP = aceIpv4.getSourceIpv4Network().getValue();
                                    ACE.flowRepresentation+="nw_src="+ACE.aceIPv4SourceIP.substring(0, ACE.aceIPv4SourceIP.lastIndexOf('/'))+",";
                                    LOG.info("ACE IPv4 source {}\n", aceIpv4.getSourceIpv4Network().getValue());
                                }
                                break;
                            case "AceIpv6":
                                ACE.isIPv4 = false;
                                AceIpv6 aceIpv6 = (AceIpv6) aceIp.getAceIpVersion();
                                if (aceIpv6.getDestinationIpv6Network() != null) {
                                    ACE.aceIPv6DestIP = aceIpv6.getDestinationIpv6Network().getValue();
                                    LOG.info("ACE IPv6 Destination {}\n", aceIpv6.getDestinationIpv6Network().getValue());
                                }
                                if (aceIpv6.getSourceIpv6Network() != null) {
                                    ACE.aceIPv6SourceIP = aceIpv6.getSourceIpv6Network().getValue();
                                    LOG.info("ACE IPv6 source	 {}\n", aceIpv6.getSourceIpv6Network().getValue());
                                }
                                if (aceIpv6.getFlowLabel() != null) {
                                    ACE.aceIPv6Label = aceIpv6.getFlowLabel().getValue();
                                    LOG.info("ACE IPv6 Flow Label {}\n", aceIpv6.getFlowLabel().getValue());
                                }
                                break;
                        }
                    }
                    break;
                case "AceEth":
                    ACE.isIP = false;
                    String source=null;
                    String destin=null;
                    AceEth aceEth = (AceEth) matches.getAceType();
                    if (aceEth.getDestinationMacAddress() != null) {
                        ACE.aceEthDestMAC = aceEth.getDestinationMacAddress().getValue();
                        destin="dl_dst="+ACE.aceEthDestMAC.trim();
                        LOG.info("ACE ETH Destination {}\n", aceEth.getDestinationMacAddress().getValue());
                    }
                    if (aceEth.getDestinationMacAddressMask() != null) {
                        ACE.aceEthDestMask = aceEth.getDestinationMacAddressMask().getValue();
                        destin+="/"+ACE.aceEthDestMask.trim()+",";
                        LOG.info("ACE ETH mask Destination{}\n", aceEth.getDestinationMacAddressMask().getValue());
                    }
                    else{
                        destin+=",";
                    }
                    if (aceEth.getSourceMacAddress() != null) {
                        ACE.aceEthSourceMAC = aceEth.getSourceMacAddress().getValue();
                        source="dl_src="+ACE.aceEthSourceMAC;
                        LOG.info("ACE ETH source {}\n", aceEth.getSourceMacAddress().getValue());
                    }
                    if (aceEth.getSourceMacAddressMask() != null) {
                        ACE.aceEthSourceMask = aceEth.getSourceMacAddressMask().getValue();
                        source+="/"+ACE.aceEthSourceMask+",";
                        LOG.info("ACE ETH  mask Destination {}\n", aceEth.getSourceMacAddressMask().getValue());
                    }
                    if(source!=null){
                        ACE.flowRepresentation+=source;
                    }
                    if(destin!=null){
                        ACE.flowRepresentation+=destin;
                    }
                    break;
            }
        }

        Actions actions = ace.getActions();
        if (actions.getPacketHandling() != null) {
            String actionType = actions.getPacketHandling().getImplementedInterface().getSimpleName();

            switch (actionType) {
                case "deny":
                    ACE.aceAction = "deny";
                    break;
                case "permit":
                    ACE.aceAction = "permit";
                    break;
                default:

                    break;
            }
        }

        Actions1 actions1 = actions.getAugmentation(Actions1.class);

        if (actions1 != null) {
            SfcAction sfcAction = actions1.getSfcAction();

            if (sfcAction != null) {
                String sfcActionType = sfcAction.getImplementedInterface().getSimpleName();

                switch (sfcActionType) {
                    case "AclRenderedServicePath":
                        AclRenderedServicePath aclRenderedServicePath = (AclRenderedServicePath) sfcAction;
                        ACE.aceRSP = aclRenderedServicePath.getRenderedServicePath();
                        LOG.info("ACE RSP {}\n", aclRenderedServicePath.getRenderedServicePath());
                        break;
                }
            }
        }
        return ACE;
    }

}
