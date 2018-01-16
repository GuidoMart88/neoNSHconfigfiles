package eu.neonsh.nshmanager.impl.DataListeners;

import eu.neonsh.nshmanager.impl.Entities.Chain;
import eu.neonsh.nshmanager.impl.Entities.sfcManagerLists;
import eu.neonsh.nshmanager.impl.Entities.vtnServices;
import eu.neonsh.nshmanager.impl.NSHManagerProvider;
import eu.neonsh.registryhandler.impl.NSHManagerRegistryImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.service.function.chain.SfcServiceFunction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SfcEntryDataListener extends AbstractDataListener {
    private static final Logger LOG = LoggerFactory.getLogger(SfcEntryDataListener.class);
	private vtnServices vtnServices;
	private sfcManagerLists lists;
	private DataBroker db;
	private NSHManagerProvider sfcManager;
    private NSHManagerRegistryImpl nri;

    public SfcEntryDataListener(NSHManagerProvider sfcManagerProvider, OpendaylightSfc opendaylightSfc,
			vtnServices vtnServsices, sfcManagerLists lists, DataBroker db){
    	setOpendaylightSfc(opendaylightSfc);
		setDataBroker(db);
        setInstanceIdentifier(OpendaylightSfc.SFC_IID);
        this.lists = lists;
		this.vtnServices = vtnServsices;
		this.db = db;
		this.sfcManager = sfcManagerProvider;
        registerAsDataChangeListener();
    }


    @Override
    public void onDataChanged(
            final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
//    	if(!NSHManagerProvider.initialized){
//    		sfcManager.initialize();
//    	}
//        printTraceStart(LOG);
    	String OriginalChainName = null;
        Map<InstanceIdentifier<?>, DataObject> dataOriginalDataObject = change.getOriginalData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataOriginalDataObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionChain) {
            	ServiceFunctionChain originalServiceChain = (ServiceFunctionChain) entry.getValue();
            	OriginalChainName=originalServiceChain.getName().getValue();
                LOG.info("\nOriginal Service function chain Name: {}", originalServiceChain.getName());

            }
        }

        // Chain CREATION
        Map<InstanceIdentifier<?>, DataObject> dataCreatedObject = change.getCreatedData();

        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataCreatedObject.entrySet()) {
            if (entry.getValue() instanceof ServiceFunctionChain) {
                ServiceFunctionChain createdServiceChain = (ServiceFunctionChain) entry.getValue();
                Chain chain=new Chain();
                chain.name=createdServiceChain.getName().getValue();
                chain.symmetric=createdServiceChain.isSymmetric();
                List<SfcServiceFunction> list = createdServiceChain.getSfcServiceFunction();
                for(SfcServiceFunction i:list){            	
//                	System.out.println("Chain index "+i.getOrder()+" node type "+i.getType().getValue());
                	chain.functions.add(i.getName());
                }
                lists.insertChain(chain);

                LOG.info("\nCreated Service Function Chain Name: {}", createdServiceChain.getName());
                nri = this.getNSHManagerRegistryImpl();
                if (nri != null) {
                    LOG.info("adding new SFC {} to neo-nsh registry",createdServiceChain.getName().getValue());
                    List<String> sfs = new ArrayList<>();
                    for (SfcServiceFunction sf:createdServiceChain.getSfcServiceFunction()){
                        sfs.add(sf.getName());
                    }

                    nri.addServiceFunctionChainToDatastore(createdServiceChain.getName().getValue(),sfs);
                }
                else {
                    LOG.error("cannot enter SF {} to neo-nsh registry because NSHManagerRegistryImpl is null!",createdServiceChain.getName());
                }

            }
        }

        // chain UPDATE
        Map<InstanceIdentifier<?>, DataObject> dataUpdatedObject = change.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : dataUpdatedObject.entrySet()) {
            if ((entry.getValue() instanceof ServiceFunctionChain)
                    && (!dataCreatedObject.containsKey(entry.getKey()))) {
            	ServiceFunctionChain updatedServiceChain = (ServiceFunctionChain) entry.getValue();
                LOG.info("\nModified Service Function Chain Name: {}", updatedServiceChain.getName());
                Chain chain=new Chain();
                chain.name=updatedServiceChain.getName().getValue();
                chain.symmetric=updatedServiceChain.isSymmetric();
                List<SfcServiceFunction> list = updatedServiceChain.getSfcServiceFunction();
                for(SfcServiceFunction i:list){            	
//                	System.out.println("Chain index "+i.getOrder()+" node type "+i.getType().getValue());
                	chain.functions.add(i.getName());
                }
                lists.updateChain(chain);

//                    Runnable task = new SbRestAclTask(RestOperation.PUT, accessList,
//                            updatedServiceClassifier.getSclServiceFunctionForwarder(), opendaylightSfc.getExecutor());
//                    opendaylightSfc.getExecutor().submit(task);
                
            }
        }

        // chain DELETION
        Set<InstanceIdentifier<?>> dataRemovedConfigurationIID = change.getRemovedPaths();
        for (InstanceIdentifier instanceIdentifier : dataRemovedConfigurationIID) {
            DataObject dataObject = dataOriginalDataObject.get(instanceIdentifier);
            if (dataObject instanceof ServiceFunctionChain) {

            	ServiceFunctionChain deletedServiceChain = (ServiceFunctionChain) dataObject;
                LOG.info("\nDeleted Service Function Chain Name: {}", deletedServiceChain.getName());
                for(Chain i:lists.chains)
                {
                	if(i.name.equalsIgnoreCase(deletedServiceChain.getName().getValue())){
                		lists.removeChainByName(i.name);
                	}
                }
            }
        }
//        printTraceStop(LOG);
    }

    public NSHManagerRegistryImpl getNSHManagerRegistryImpl() {
        if (this.nri == null) {
            return NSHManagerRegistryImpl.getInstance();
        } else {
            return this.nri;
        }
    }
}
