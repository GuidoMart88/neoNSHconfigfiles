package eu.neonsh.nshmanager.impl.DataListeners;

import eu.neonsh.nshmanager.impl.Entities.vtnServices;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class ScfEntryDataListener extends AbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(ScfEntryDataListener.class);
	private vtnServices vtnServices;

    public ScfEntryDataListener(OpendaylightSfc opendaylightSfc, vtnServices vtnServices) {
        setOpendaylightSfc(opendaylightSfc);
        setDataBroker(opendaylightSfc.getDataProvider());
        setInstanceIdentifier(OpendaylightSfc.SCF_ENTRY_IID);
        this.vtnServices=vtnServices;
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        printTraceStart(LOG);

        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier originalServiceClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\nOriginal Service Classifier Name: {}", originalServiceClassifier.getName());
            }
        }

        // Classifier CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionClassifier) {
                ServiceFunctionClassifier createdServiceClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\nCreated Service Classifier Name: {}", createdServiceClassifier.getName());

                if (createdServiceClassifier.getAcl() != null ) {
                    Acl accessList = SfcProviderAclAPI.readAccessList(createdServiceClassifier.getAcl().getName(),
                        createdServiceClassifier.getAcl().getType());

                }
            }
        }

        // Classifier UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionClassifier)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
                ServiceFunctionClassifier updatedServiceClassifier = (ServiceFunctionClassifier) entry.getValue();
                LOG.debug("\nModified Service Classifier Name: {}", updatedServiceClassifier.getName());

                if (updatedServiceClassifier.getAcl() != null) {
                    Acl accessList = SfcProviderAclAPI.readAccessList(updatedServiceClassifier.getAcl().getName(),
                        updatedServiceClassifier.getAcl().getType());

//                    Runnable task = new SbRestAclTask(RestOperation.PUT, accessList,
//                            updatedServiceClassifier.getSclServiceFunctionForwarder(), opendaylightSfc.getExecutor());
//                    opendaylightSfc.getExecutor().submit(task);
                }
            }
        }

        // Classifier DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionClassifier) {

                ServiceFunctionClassifier deletedServiceClassifier = (ServiceFunctionClassifier) dataObject;
                LOG.debug("\nDeleted Service Classifier Name: {}", deletedServiceClassifier.getName());

//                if (deletedServiceClassifier.getAcl() != null) {
//                    Runnable task = new SbRestAclTask(RestOperation.DELETE, deletedServiceClassifier.getAcl().getName(),
//                            deletedServiceClassifier.getAcl().getType(),
//                            deletedServiceClassifier.getSclServiceFunctionForwarder(), opendaylightSfc.getExecutor());
//                    opendaylightSfc.getExecutor().submit(task);
//                }
            }
        }
        printTraceStop(LOG);
    }


}
