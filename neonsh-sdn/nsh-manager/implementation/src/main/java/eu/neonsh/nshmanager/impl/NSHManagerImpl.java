package eu.neonsh.nshmanager.impl;

/**
 * Created by theovas on 19.11.17.
 */

import eu.neonsh.nshmanager.impl.Entities.AccessList;
import eu.neonsh.nshmanager.impl.Entities.AccessListEntry;
import eu.neonsh.registryhandler.impl.NSHManagerRegistryImpl;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.FlowStatus;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.access.lists.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.ClassificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.FlowsToPortMappings;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.flows.to.port.mappings.Flows;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.metadata.grouping.MetadataElements;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.metadata.grouping.MetadataElementsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.service.function.chains.SfType;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.topology.info.grouping.Switches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Future;

public class NSHManagerImpl implements NshmanagerService, NshmanagerListener {

    private static final Logger LOG = LoggerFactory.getLogger(NSHManagerImpl.class);
    private DataBroker db;
    private SalFlowService salFlowService;
    private NotificationProviderService notificationProviderService;
    public static NSHManagerImpl nshmanager;
    // set some default implementation for the EdgePointCombinator
    // set some dummy variables for the path manager methods
    private double rate = 0, burst = 0, maxPacketSize = 0, maxDelay = 100;
    Ipv4Address srcIp, dstIp;
    int srcPort = 0, dstPort = 0;
    short protocol = 0;
    private String orchestratorToken;
    private boolean monitoringJobActive = false;
    public static Long transactionIDCounter = 0L;
    private NSHManagerRegistryImpl nri;

    public NSHManagerImpl(DataBroker db, NotificationProviderService notificationProviderService, SalFlowService salFlowService) {
        this.db = db;
        this.salFlowService = salFlowService;
        this.notificationProviderService = notificationProviderService;
        this.notificationProviderService.registerNotificationListener(this);
        nshmanager = this;
    }

    public NSHManagerImpl() {
    }

    public static NSHManagerImpl getInstance() {
        if (nshmanager == null) {
            nshmanager = new NSHManagerImpl();
        }
        return nshmanager;
    }

    @Override
    public Future<RpcResult<ManuallyEnterTopologyInfoOutput>> manuallyEnterTopologyInfo(ManuallyEnterTopologyInfoInput input) {
        ManuallyEnterTopologyInfoOutputBuilder outputB = new ManuallyEnterTopologyInfoOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            TopologyInfo topologyInfo = new TopologyInfoBuilder()
                    .setSwitches(input.getTopologyInfo().getSwitches())
                    .build();
            nri.writeTopologyInfoToDatastore(topologyInfo);
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. Topology info was not updated on registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        ManuallyEnterTopologyInfoOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<InitSetupOutput>> initSetup() {
        InitSetupOutputBuilder outputB = new InitSetupOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            TopologyInfo topologyInfo = nri.getTopologyInfoFromDatastore();
            // reading from registry the name of the first and only switch
            Switches s = topologyInfo.getSwitches().get(0);
            String switchName = s.getSwitchId();
            String username = s.getSshInfo().getUsername();
            String password = s.getSshInfo().getPassword();
            String IP = s.getSshInfo().getIp().getValue();
            // removing all flows from switch
            SFCManagerUtils.deleteAllFlows(switchName, username, password, IP);
            LOG.info("removed all flows from switch");
            nri.initializeNSHManagerRegistryDataTree();
            LOG.info("initialized Data Model on registry");

            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. Setup not initialized.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        InitSetupOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();

    }

    @Override
    public Future<RpcResult<AddMetadataOutput>> addMetadata(AddMetadataInput input) {
        AddMetadataOutputBuilder outputB = new AddMetadataOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            Metadata metadata = nri.getMetadataFromDatastore();
            String duplicateEs = "";
            for (MetadataElements me2 : input.getMetadata().getMetadataElements()) {
                boolean found = false;
                for (MetadataElements me : metadata.getMetadataElements()) {
                    if (me2.getName().equals(me.getName())) {
                        LOG.warn("Metadata element with same name already exists in registry. Please choose another name or delete the existing metadata element.");
                        duplicateEs += ":" + me2.getName();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    MetadataElements metadataElement = new MetadataElementsBuilder()
                            .setName(me2.getName())
                            .setNshMetadata(me2.getNshMetadata())
                            .build();
                    nri.addMetadataToDatastore(metadataElement);
                }
            }
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("Already existing elements that will not be added: " + duplicateEs);
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. Metadata were not updated on registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        AddMetadataOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<AddSFIsOutput>> addSFIs(AddSFIsInput input) {
        AddSFIsOutputBuilder outputB = new AddSFIsOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            ServiceFunctionInstances sfis = nri.getServiceFunctionInstancesFromDatastore();
            String duplicateEs = "";
            for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances me2 : input.getSFIs().getServiceFunctionInstances()) {
                boolean found = false;
                for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances me : sfis.getServiceFunctionInstances()) {
                    if (me2.getName().equals(me.getName())) {
                        LOG.warn("ServiceFunctionInstances element with same name already exists in registry. Please choose another name or delete the existing element.");
                        duplicateEs += ":" + me2.getName();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances sfiElement = new ServiceFunctionInstancesBuilder()
                            .setName(me2.getName())
                            .setTopologyInfo(me2.getTopologyInfo())
                            .setManagementInfo(me2.getManagementInfo())
                            .setSfType(me2.getSfType())
                            .build();
                    nri.addServiceFunctionInstancesToDatastore(sfiElement);
                }
            }
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("Already existing elements that will not be added: " + duplicateEs);
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. SFIs were not updated on registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        AddSFIsOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RemoveSFIOutput>> removeSFI(RemoveSFIInput input) {
        RemoveSFIOutputBuilder outputB = new RemoveSFIOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            nri.removeServiceFunctionInstancesFromDatastore(input.getSfiName());
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. SFI was not removed from registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        RemoveSFIOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RemoveAccessListOutput>> removeAccessList(RemoveAccessListInput input) {
        RemoveAccessListOutputBuilder outputB = new RemoveAccessListOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            nri.removeAccessListsFromDatastore(input.getAclName());
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. AccessList was not removed from registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        RemoveAccessListOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RemoveMetadataOutput>> removeMetadata(RemoveMetadataInput input) {
        RemoveMetadataOutputBuilder outputB = new RemoveMetadataOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            nri.removeMetadataFromDatastore(input.getMetadataName());
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. Metadata was not removed from registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        RemoveMetadataOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RemoveClassificationOutput>> removeClassification(RemoveClassificationInput input) {
        RemoveClassificationOutputBuilder outputB = new RemoveClassificationOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            nri.removeClassificationsFromDatastore(input.getClassificationId());
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. Classification was not removed from registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        RemoveClassificationOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<AddAccessListsOutput>> addAccessLists(AddAccessListsInput input) {
        AddAccessListsOutputBuilder outputB = new AddAccessListsOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        if (nri != null) {
            AccessLists metadata = nri.getAccessListsFromDatastore();
            String duplicateEs = "";
            for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessLists me2 : input.getAccessLists().getAccessLists()) {
                boolean found = false;
                for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessLists me : metadata.getAccessLists()) {
                    if (me2.getName().equals(me.getName())) {
                        LOG.warn("AccessLists element with same name already exists in registry. Please choose another name or delete the existing element.");
                        duplicateEs += ":" + me2.getName();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessLists aclElement = new AccessListsBuilder()
                            .setName(me2.getName())
                            .setAccessListEntries(me2.getAccessListEntries())
                            .build();
                    nri.addAccessListsToDatastore(aclElement);
                }
            }
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("Already existing elements that will not be added: " + duplicateEs);
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. AccessLists were not updated on registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        AddAccessListsOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    /**
     * This method takes as input one access list entry and one service function chain and creates i) classification rules on the corresponding switch(es), ii) SFF rules on the switch(es)
     * and iii) corresponding entries on the registry.
     * <p>
     * For i), it reads the topology information in the registry to get the input port (i.e., source port of incoming traffic) and output port (i.e., destination port for outgoing traffic) and for each
     * access list entry of the access list, it creates a classification flow (in Table 1) with specified source IP, SFC id and metadata information (i.e., NSH context header data). It also creates the corresponding
     * egress flow rules that pop NSH header and deliver packets to the output port.
     * <p>
     * For ii), for each SFT in the SFC, it decreases the NSI and then it searches all SFIs in registry to find the ones of same SFT. For those, for each metadata element to which they are associated, if
     * this metadata is relevant to the provided access list elements, it creates a SFF flow (in Table 1) that matches the NSP, NSI and metadata information (i.e., NSH context header data) and forwards packets to the port
     * of the SFI.
     * <p>
     * For iii), if everything goes successfully, it adds an entry to the registry containing the specified mapping.
     * If specified input or inferred relations do not correspond to data stored in registry, it returns corresponding message. In addition, it creates the default rule to send traffic from any port to table 1
     * (where all SFF rules ar present), if it does not already exist.
     *
     * @param input
     * @return
     */

    @Override
    public Future<RpcResult<AddClassificationOutput>> addClassification(AddClassificationInput input) {
        AddClassificationOutputBuilder outputB = new AddClassificationOutputBuilder();
        nri = this.getNSHManagerRegistryImpl();
        boolean has2switches = false;
        if (nri != null) {
            TopologyInfo topologyInfo = nri.getTopologyInfoFromDatastore();
            // get info of first switch
            Switches s1 = topologyInfo.getSwitches().get(0);
            String switchName1 = s1.getSwitchId();
            String username1 = s1.getSshInfo().getUsername();
            String password1 = s1.getSshInfo().getPassword();
            String IP1 = s1.getSshInfo().getIp().getValue();
            // get info of second switch

            String switchName2 = null;
            String username2 = null;
            String password2 = null;
            String IP2 = null;
            Switches s2 = null;
            try {
                s2 = topologyInfo.getSwitches().get(1);
                if (s2 != null) {
                    has2switches = true;
                    switchName2 = s2.getSwitchId();
                    username2 = s2.getSshInfo().getUsername();
                    password2 = s2.getSshInfo().getPassword();
                    IP2 = s2.getSshInfo().getIp().getValue();
                }
            } catch (Exception e) {
                //LOG.error(e.);
            }

            Classifications classifications = nri.getClassificationsFromDatastore();
            Classifications classifications_new2 = null;

// getting corresponding accesslist from datastore
            boolean found = false;
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessLists accessList = null;
            AccessLists accessLists = nri.getAccessListsFromDatastore();
            for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessLists acl : accessLists.getAccessLists()) {
                if (acl.getName().equals(input.getAccessListName())) {
                    accessList = acl;
                    found = true;
                    break;
                }
            }

            if (found != true) {
                outputB.setRequestOutcome(RequestOutcome.FAILURE)
                        .setOutcomeDetails("Provided access list does not correspond to any access list currently stored in registry");
                AddClassificationOutput output = outputB
                        .build();

                return RpcResultBuilder.success(output).buildFuture();
            }
// getting corresponding service function chain from datastore
            found = false;
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.ServiceFunctionChains serviceFunctionChain = null;
            ServiceFunctionChains sfcs = nri.getServiceFunctionChainsFromDatastore();
            for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.ServiceFunctionChains sfc : sfcs.getServiceFunctionChains()) {
                if (sfc.getName().equals(input.getSfcName())) {
                    serviceFunctionChain = sfc;
                    found = true;
                    break;
                }
            }

            if (found != true) {
                outputB.setRequestOutcome(RequestOutcome.FAILURE)
                        .setOutcomeDetails("Provided service function chain does not correspond to any access list currently stored in registry");
                AddClassificationOutput output = outputB
                        .build();

                return RpcResultBuilder.success(output).buildFuture();
            }

            // the nsp value of the specified sfc in HEX format
            String nspHEX = Long.toHexString(serviceFunctionChain.getId());
            // embedding default flows for both switches, if not already embedded
            if ((classifications.getDefaultFlowStatus() == null) || (classifications.getDefaultFlowStatus() == FlowStatus.NOTEMBEDDED)) {
                try {
                    SFCManagerUtils.createDefaultFlow(switchName1, username1, password1, IP1, nspHEX);
                    if (has2switches) {
                        SFCManagerUtils.createDefaultFlow(switchName2, username2, password2, IP2, nspHEX);
                    }
                    classifications_new2 = new
                            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ClassificationsBuilder()
                            .setClassifications(classifications.getClassifications())
                            .setCurrId(classifications.getCurrId())
                            .setDefaultFlowStatus(FlowStatus.EMBEDDED)
                            .build();
                    nri.setClassificationsToDatastore(classifications_new2);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

            }


// create one classification flow rule on the first switch (which is the classifier) for every access list entry of the mapping
            Metadata metadatas = nri.getMetadataFromDatastore();
            Set<String> relevantMetadatas = new HashSet();
            for (AccessListEntries acle : accessList.getAccessListEntries()) {
                found = false;
                MetadataElements metadata = null;
                for (MetadataElements md : metadatas.getMetadataElements()) {
                    if (md.getName().equals(acle.getMetadataName())) {
                        metadata = md;
                        relevantMetadatas.add(metadata.getName());
                        found = true;
                        break;
                    }
                }

                if (found == true) {
                    try {
                        if (has2switches) {
                            String metadataHEX = Integer.toHexString(metadata.getNshMetadata().getNpc());
                            // create classification flow

                            SFCManagerUtils.createClassificationFlow(Math.toIntExact(s1.getSrcPort()), createflowRuleFromACL(acle), nspHEX, metadataHEX, switchName1, username1, password1, IP1);
                            // create corresponding pop nsh egress flow on the second switch
                            int nsiMin = 255 - serviceFunctionChain.getSfType().size();
                            String nsiHEX = Integer.toHexString(nsiMin);
                            SFCManagerUtils.createPopNSHEgressFlow(switchName2, nspHEX, nsiHEX, metadataHEX, Math.toIntExact(s2.getDestPort()), username2, password2, IP2);
                        } else {
                            String metadataHEX = Integer.toHexString(metadata.getNshMetadata().getNpc());
                            // create classification flow
                            SFCManagerUtils.createClassificationFlow(Math.toIntExact(s1.getSrcPort()), createflowRuleFromACL(acle), nspHEX, metadataHEX, switchName1, username1, password1, IP1);
                            // create corresponding pop nsh egress flow on the second switch
                            int nsiMin = 255 - serviceFunctionChain.getSfType().size();
                            String nsiHEX = Integer.toHexString(nsiMin);
                            SFCManagerUtils.createPopNSHEgressFlow(switchName1, nspHEX, nsiHEX, metadataHEX, Math.toIntExact(s1.getDestPort()), username1, password1, IP1);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    LOG.error("Metadata {} specified for access list entry {} was not found in registry!", acle.getMetadataName(), acle);
                }

            }

            // Creating the nsh match rules
            Switches currSwitch = null;
            List<SfType> sfList = serviceFunctionChain.getSfType();
            Collections.sort(sfList, new SortbyIndex());
            int nsi = 255;
            for (SfType sfType : sfList) {
                nsi = 255 - sfType.getIndex();
                found = false;
                String nsiHEX = Integer.toHexString(nsi);
                String metadataHEX = null;
                ServiceFunctionInstances sfis = nri.getServiceFunctionInstancesFromDatastore();
                for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances sfi : sfis.getServiceFunctionInstances()) {
                    for (String metadataName : sfi.getManagementInfo().getAssociatedMetadata().getMetadataNames()) {
                        if ((relevantMetadatas.contains(metadataName)) && (sfi.getSfType().equals(sfType.getName()))) {
                            found = true;
                            for (MetadataElements md : metadatas.getMetadataElements()) {
                                if (md.getName().equals(metadataName)) {
                                    metadataHEX = Integer.toHexString(md.getNshMetadata().getNpc());
                                    break;
                                }
                            }
                            // get relevant info for the switch of this sfi
                            String switchName = sfi.getTopologyInfo().getSwitchId();
                            // if the sfi switch is different than the switch of the previous sfi
                            if ((currSwitch != null) && (!currSwitch.getSwitchId().equals(switchName))) {
                                // exit the current switch
                                SFCManagerUtils.createExitSwitchFlow(currSwitch.getSwitchId(), nspHEX, nsiHEX, Math.toIntExact(currSwitch.getDestPort()), currSwitch.getSshInfo().getUsername(), currSwitch.getSshInfo().getPassword(), currSwitch.getSshInfo().getIp().getValue());

                            }
                            int nsiMin = 255 - serviceFunctionChain.getSfType().size();

                            Switches sfiSwitch = null;
                            for (Switches s : topologyInfo.getSwitches()) {
                                if (s.getSwitchId().equals(switchName)) {
                                    sfiSwitch = s;
                                    currSwitch = s;
                                    break;
                                }
                            }
                            String username = null;
                            String password = null;
                            String ip = null;
                            if (sfiSwitch != null) {
                                username = sfiSwitch.getSshInfo().getUsername();
                                password = sfiSwitch.getSshInfo().getPassword();
                                ip = sfiSwitch.getSshInfo().getIp().getValue();
                            }

                            try {
                                SFCManagerUtils.createMatchNSHFlow(switchName, nspHEX, nsiHEX, metadataHEX, Math.toIntExact(sfi.getTopologyInfo().getPort()), username, password, ip);
                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }

                        }

                    }
                }

                if (found != true) {
                    outputB.setRequestOutcome(RequestOutcome.FAILURE)
                            .setOutcomeDetails("There was no SFI with relevant metadata in the registry corresponding to SFT " + sfType.getName() + " of the provided service function chain");
                    AddClassificationOutput output = outputB
                            .build();

                    return RpcResultBuilder.success(output).buildFuture();
                }


            }

            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.Classifications classification = new ClassificationsBuilder()
                    .setAccessListName(input.getAccessListName())
                    .setSfcName(input.getSfcName())
                    .build();
            nri.addClassificationsToDatastore(classification);
            outputB.setRequestOutcome(RequestOutcome.SUCCESS)
                    .setOutcomeDetails("OK");
        } else {
            LOG.warn("NSHManagerRegistryImpl is null. Classification was not updated on registry.");
            outputB.setRequestOutcome(RequestOutcome.FAILURE)
                    .setOutcomeDetails("NSHManagerRegistryImpl is null");
        }
        AddClassificationOutput output = outputB
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }


    public String getOrchestratorToken() {
        return orchestratorToken;
    }

    public void setOrchestratorToken(String orchestratorToken) {
        this.orchestratorToken = orchestratorToken;
    }

    public boolean isMonitoringJobActive() {
        return monitoringJobActive;
    }

    public void setMonitoringJobActive(boolean monitoringJobActive) {
        this.monitoringJobActive = monitoringJobActive;
    }

    public NSHManagerRegistryImpl getNSHManagerRegistryImpl() {
        if (this.nri == null) {
            return NSHManagerRegistryImpl.getInstance();
        } else {
            return this.nri;
        }
    }

    private String createflowRuleFromACL(AccessListEntries acle) {
        String representation = "";
        representation += "nw_src=" + acle.getSrcIp().getValue();
        if (acle.getDstPort() != null) {
            representation += ",tp_dst=" + acle.getDstPort();
        }
        if (acle.getIPprotocol() != null) {
            if (acle.getIPprotocol() == 1) {
                representation += ",icmp";
            } else if (acle.getIPprotocol() == 6) {
                representation += ",tcp";
            } else if (acle.getIPprotocol() == 17) {
                representation += ",udp";
            }
        }
        return representation;
    }

    @Override
    public void onLinkChanged(LinkChanged notification) {
        if (notification.getTopologyUpdate() == LinkChanged.TopologyUpdate.LinkRemoved) {
            TopologyInfo topologyInfo = nri.getTopologyInfoFromDatastore();
            LOG.info("A link was removed from the topology!");
            String linkId = notification.getLink().getLinkId().getValue();
            LOG.info("The corresponding link id is: " + linkId);
            if ((linkId.startsWith("openflow"))&&(linkId.contains("host"))){
                String firstpart = linkId.substring(0,linkId.indexOf("/"));
                linkId=firstpart;
            }

            Long port = Long.parseLong(linkId.substring(linkId.lastIndexOf(":") + 1));
            // linkId will be something like "...openflow:2:3" so we want to turn this to "s2"
            String switchId = "s"+ linkId.substring(linkId.lastIndexOf(":") - 1).charAt(0);
            String switchPortId = switchId+"@@"+port.toString();
            LOG.info("Port of link down is: " + port);
            // getting the SFIs from registry
            nri = this.getNSHManagerRegistryImpl();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances downSFI = null;
            if (nri != null) {
                ServiceFunctionInstances sfis = nri.getServiceFunctionInstancesFromDatastore();
                for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances sfi : sfis.getServiceFunctionInstances()) {
                    if ((sfi.getTopologyInfo().getSwitchId().equals(switchId))&&(sfi.getTopologyInfo().getPort().equals(port))) {
                        // found the SFI that went down!
                        downSFI = sfi;
                        break;
                    }
                }
                if (downSFI != null) {
                    // remove the SFI that went down from the registry
                    nri.removeServiceFunctionInstancesFromDatastore(downSFI.getName());
                    // get the SFT of the SFI that went down
                    String sfType = downSFI.getSfType();
                    // find a SFI of the same type
                    org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances sfiAlt = null;
                    for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances sfi : sfis.getServiceFunctionInstances()) {
                        // if it is not the SFI that went down but it has the same sfType and it is adjacent to the same switch
                        if ((sfi.getName().equals(downSFI.getName()) == false) && (sfi.getSfType().equals(sfType))) {
                            // found an sfi of same sfType
                            sfiAlt = sfi;
                            break;
                        }
                    }
                    if (sfiAlt != null) {
                        Switches currSwitch=null;
                        sfiAlt.getManagementInfo().getAssociatedMetadata().getMetadataNames().addAll(downSFI.getManagementInfo().getAssociatedMetadata().getMetadataNames());
                        if (!sfiAlt.getTopologyInfo().getSwitchId().equals(downSFI.getTopologyInfo().getSwitchId())) {

                            // Delete all flows that are associated with the sfi that went down (i.e., with that port)
                            MatchFlowsPerPort flowsPerPorts = nri.getMatchFlowsPerPortFromDatastore();
                            // find the ones that match the port of our sfi
                            if (flowsPerPorts != null) {
                                boolean found = false;
                                FlowsToPortMappings fppInReg = null;
                                for (FlowsToPortMappings fPerPort : flowsPerPorts.getFlowsToPortMappings()) {
                                    if (fPerPort.getSwitchPortId().equals(switchPortId)) {
                                        fppInReg = fPerPort;
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    for (Flows flow : fppInReg.getFlows()) {
                                        for (Switches s : topologyInfo.getSwitches()) {
                                            if (s.getSwitchId().equals(flow.getSwitchName())) {
                                                currSwitch = s;
                                                break;
                                            }
                                        }
                                        String flowString = String.format("nsh_mdtype=1,nsp=0x%s,nsi=0x%s,nshc1=0x%s", flow.getNspHEX(), flow.getNsiHEX(), flow.getMetadataHEX());
                                        SFCManagerUtils.deleteFlow(flow.getSwitchName(), flowString, flow.getUsername(), flow.getPassword(), flow.getIP());

                                        SFCManagerUtils.createExitSwitchFlow(flow.getSwitchName(), flow.getNspHEX(), flow.getNsiHEX(),currSwitch.getDestPort().intValue(), flow.getUsername(), flow.getPassword(), flow.getIP());
                                    }
                                    // remove flows from registry
                                    nri.removeFlowsToPortMappingsFromDatastore(switchPortId);

                                    String sfiAltSwitchPortId = sfiAlt.getTopologyInfo().getSwitchId() + "@@" + Long.toString(sfiAlt.getTopologyInfo().getPort());
                                    // find the flows associated with the alt port
                                    FlowsToPortMappings fppAltInReg = null;
                                    Boolean Altfound = false;
                                    for (FlowsToPortMappings fPerPort : flowsPerPorts.getFlowsToPortMappings()) {
                                        if (fPerPort.getSwitchPortId().equals(sfiAltSwitchPortId)) {
                                            fppAltInReg = fPerPort;
                                            Altfound = true;
                                            break;
                                        }
                                    }
                                    for (Flows flow : fppInReg.getFlows()) {
                                        for (Switches s : topologyInfo.getSwitches()) {
                                            if (s.getSwitchId().equals(sfiAlt.getTopologyInfo().getSwitchId())) {
                                                currSwitch = s;
                                                break;
                                            }
                                        }
                                        // embed the actual flow to the switch
                                        try {
//                                                SFCManagerUtils.createUpdateMatchNSHFlow(flow.getSwitchName(), flow.getNspHEX(), flow.getNsiHEX(), flow.getMetadataHEX(), Math.toIntExact(sfiAlt.getTopologyInfo().getPort()), flow.getUsername(), flow.getPassword(), flow.getIP());
                                            SFCManagerUtils.createMatchNSHFlow(currSwitch.getSwitchId(), flow.getNspHEX(), flow.getNsiHEX(), flow.getMetadataHEX(), Math.toIntExact(sfiAlt.getTopologyInfo().getPort()), currSwitch.getSshInfo().getUsername(), currSwitch.getSshInfo().getPassword(), currSwitch.getSshInfo().getIp().getValue());
                                        } catch (NoSuchAlgorithmException e) {
                                            e.printStackTrace();
                                        }
                                    }


                                    if (Altfound) {

                                        fppAltInReg.getFlows().addAll(fppInReg.getFlows());
                                        // remove flows from registry
                                        nri.removeFlowsToPortMappingsFromDatastore(sfiAltSwitchPortId);
                                        // add updated flows to registry
                                        nri.addFlowsToPortMappingsToDatastore(fppAltInReg);
                                    }
                                    else{
                                        System.err.println("Could not find alternative function");
                                    }

                                }
                            }
                        }
                        else{
                            // Delete all flows that are associated with the sfi that went down (i.e., with that port)
                            MatchFlowsPerPort flowsPerPorts = nri.getMatchFlowsPerPortFromDatastore();
                            // find the ones that match the port of our sfi
                            if (flowsPerPorts != null) {
                                boolean found = false;
                                FlowsToPortMappings fppInReg = null;
                                for (FlowsToPortMappings fPerPort : flowsPerPorts.getFlowsToPortMappings()) {
                                    if (fPerPort.getSwitchPortId().equals(switchPortId)) {
                                        fppInReg = fPerPort;
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) {
                                    for (Flows flow : fppInReg.getFlows()) {
                                        String flowString = String.format("nsh_mdtype=1,nsp=0x%s,nsi=0x%s,nshc1=0x%s", flow.getNspHEX(), flow.getNsiHEX(), flow.getMetadataHEX());
                                        SFCManagerUtils.deleteFlow(flow.getSwitchName(), flowString, flow.getUsername(), flow.getPassword(), flow.getIP());
                                    }
                                    // remove flows from registry
                                    nri.removeFlowsToPortMappingsFromDatastore(switchPortId);

                                    String sfiAltSwitchPortId = sfiAlt.getTopologyInfo().getSwitchId()+"@@"+Long.toString(sfiAlt.getTopologyInfo().getPort());
                                    // find the flows associated with the alt port
                                    FlowsToPortMappings fppAltInReg = null;
                                    boolean Altfound = false;
                                    for (FlowsToPortMappings fPerPort : flowsPerPorts.getFlowsToPortMappings()) {
                                        if (fPerPort.getSwitchPortId().equals(sfiAltSwitchPortId)) {
                                            fppAltInReg = fPerPort;
                                            found = true;
                                            break;
                                        }
                                    }
                                    for (Flows flow:fppInReg.getFlows()) {
                                        // embed the actual flow to the switch
                                        try {
                                            SFCManagerUtils.createMatchNSHFlow(flow.getSwitchName(), flow.getNspHEX(), flow.getNsiHEX(), flow.getMetadataHEX(), Math.toIntExact(sfiAlt.getTopologyInfo().getPort()), flow.getUsername(), flow.getPassword(), flow.getIP());
                                        } catch (NoSuchAlgorithmException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (Altfound){

                                        fppAltInReg.getFlows().addAll(fppInReg.getFlows());
                                        // remove flows from registry
                                        nri.removeFlowsToPortMappingsFromDatastore(sfiAltSwitchPortId);
                                        // add updated flows to registry
                                        nri.addFlowsToPortMappingsToDatastore(fppAltInReg);
                                    }

                                }
                            }
                            else {LOG.warn("There were no flows associated with the port of the failed SFI");}



                        }
                    } else {
                        LOG.error("Could not find SFI of the same type as the one that went down!");
                    }
                } else {
                    LOG.warn("There was no active SFI associated with the link that went down!");
                }
            }
        } else if (notification.getTopologyUpdate() == LinkChanged.TopologyUpdate.LinkAdded) {
            LOG.info("A link was added to the topology!");
            LOG.info("The corresponding link id is: " + notification.getLink().getLinkId().getValue());
        } else if (notification.getTopologyUpdate() == LinkChanged.TopologyUpdate.LinkUpdated) {
            LOG.info("A link was updated in the topology!");
            LOG.info("The corresponding link id is: " + notification.getLink().getLinkId().getValue());
        }

}

class SortbyIndex implements Comparator<SfType> {
    // Used for sorting in ascending order of
    // roll number
    public int compare(SfType a, SfType b) {
        return a.getIndex() - b.getIndex();
    }
}
}
