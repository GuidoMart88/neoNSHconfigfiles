package eu.neonsh.nshmanager.impl.DataListeners;

import eu.neonsh.nshmanager.impl.Entities.node;
import eu.neonsh.nshmanager.impl.Entities.sfcManagerLists;
import eu.neonsh.nshmanager.impl.Entities.vtnServices;
import eu.neonsh.nshmanager.impl.NSHManagerProvider;
import eu.neonsh.registryhandler.impl.NSHManagerRegistryImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.OtherLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.LocatorType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SfEntryDataListener extends AbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfEntryDataListener.class);

    public static final String _SERVICE_FUNCTION = "service-function";
    public static final String _REST_URI = "rest-uri";
    public static final String _IP_MGMT_ADDRESS = "ip-mgmt-address";
    public static final String _SF_DATA_PLANE_LOCATOR = "sf-data-plane-locator";
    public static final String _SERVICE_FUNCTION_FORWARDER = "service-function-forwarder";

    public static final String SERVICE_FUNCTION_TYPE_PREFIX = "service-function-type:";
    private vtnServices vtnServices;

    private sfcManagerLists lists;

    private DataBroker db;

    private NSHManagerProvider sfcManager;
    private NSHManagerRegistryImpl nri;

    public SfEntryDataListener(NSHManagerProvider sfcManagerProvider, OpendaylightSfc opendaylightSfc,
                               vtnServices vtnServsices, sfcManagerLists lists, DataBroker db) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SF_ENTRY_IID);
        this.lists = lists;
        this.vtnServices = vtnServsices;
        this.db = db;
        this.sfcManager = sfcManagerProvider;
        registerAsDataChangeListener();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        // printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
                ServiceFunction originalServiceFunction = (ServiceFunction) entry.getValue();
                LOG.info("\nOriginal Service Function Name: {}", originalServiceFunction.getName());

            }
        }

        // SF CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunction) {
//                if (!NSHManagerProvider.initialized) {
//                    this.sfcManager.initialize();
//                }
                ServiceFunction sf = (ServiceFunction) entry.getValue();
                nri = this.getNSHManagerRegistryImpl();
                if (nri != null) {
                    LOG.info("adding new SF {} to neo-nsh registry",sf.getName().getValue());
                    nri.addServiceFunctionTypeToDatastore(sf.getName().getValue(),sf.getType().getValue());
                }
                else {
                    LOG.error("cannot enter SF {} to neo-nsh registry because NSHManagerRegistryImpl is null!",sf.getName());
                }
                node node = new node();
                if (sf.getName() != null && sf.getName().getValue() != null) {
                    node.name = sf.getName().getValue();
                }

                if (sf.getIpMgmtAddress()!=null && sf.getIpMgmtAddress().getIpv4Address() != null) {
                    node.ip = sf.getIpMgmtAddress().getIpv4Address().getValue();
                    LOG.info("NODE_IP {}\n", node.ip);
                } else if (sf.getIpMgmtAddress()!=null && sf.getIpMgmtAddress().getIpv6Address() != null) {
                    node.ip = sf.getIpMgmtAddress().getIpv6Address().getValue();
                }
                if (sf.getSfDataPlaneLocator() != null) {
//					System.out.println("DataPlane locator ");
                    List<SfDataPlaneLocator> locators = sf.getSfDataPlaneLocator();
                    if (locators.size() == 1) {
                        node.hasOneInterface = true;
                    }
                    Iterator<SfDataPlaneLocator> it = locators.iterator();
                    while (it.hasNext()) {
                        SfDataPlaneLocator i = it.next();
//						System.out.println(i.getLocatorType());
                        if (i.getLocatorType() instanceof OtherLocator) {
                            if ("input".equalsIgnoreCase(i.getName().getValue())) {
                                LocatorType locType = i.getLocatorType();
                                String port =((OtherLocator) locType).getOtherName();
                                node.inputInterfaceName = port;
//								node.inputInterfaceName=((OtherLocator) locType).getOtherName();
                                LOG.info("inputPort {}\n", node.inputInterfaceName);
                            } else if ("output".equalsIgnoreCase(i.getName().getValue())) {
                                LocatorType locType = i.getLocatorType();
                                String port =((OtherLocator) locType).getOtherName();
                                node.outputInterfaceName = port;
                                LOG.info("outputPort {}\n", node.outputInterfaceName);
                            }
                        }
                    }
                }
                if (sf.getRestUri() != null) {
                    node.restUri = sf.getRestUri().getValue();
                }
                if (sf.getType() != null) {
                    node.type = sf.getType().getValue().toLowerCase();

                }
                lists.insertServiceFunction(node);
                LOG.info("Created Service Function Name: {}", sf.getName());

            }
        }

        // SF UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunction) && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunction updatedServiceFunction = (ServiceFunction) entry.getValue();
                LOG.info("\nModified Service Function Name: {}", updatedServiceFunction.getName());

                // Runnable task = new SbRestSfTask(RestOperation.PUT,
                // updatedServiceFunction, opendaylightSfc.getExecutor());
                // opendaylightSfc.getExecutor().submit(task);
            }
        }

        // SF DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunction) {

                ServiceFunction originalServiceFunction = (ServiceFunction) dataObject;
                LOG.info("\nDeleted Service Function Name: {}", originalServiceFunction.getName());
                lists.deleteServiceFunctionByName(originalServiceFunction.getName().getValue());
                // Runnable task = new SbRestSfTask(RestOperation.DELETE,
                // originalServiceFunction, opendaylightSfc.getExecutor());
                // opendaylightSfc.getExecutor().submit(task);
            }
        }
        // printTraceStop(LOG);
    }

//    private String findPortFromInterfaceID(String otherName) {
//        String switchPort = AnSshConnector.executeCommand("sudo ovs-ofctl show " + NSHManagerUtils.SwitchName + " | grep " + otherName);
//        System.out.println("sudo ovs-ofctl show " + NSHManagerUtils.SwitchName + " | grep " + otherName);
//        String outcome = null;
//        if (switchPort != null && switchPort.length() != 0) {
//            try {
//                outcome = switchPort.substring(0, switchPort.indexOf('(')).trim();
//            } catch (Exception e) {
//                LOG.error("Could not find where this Service Function in switch {}  is connected by the interface name  {} given", NSHManagerUtils.SwitchName, otherName);
//            }
//
//        }
//        return outcome;
//    }

    public NSHManagerRegistryImpl getNSHManagerRegistryImpl() {
        if (this.nri == null) {
            return NSHManagerRegistryImpl.getInstance();
        } else {
            return this.nri;
        }
    }

}
