package eu.neonsh.registryhandler.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.NshManagerDM;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.NshManagerDMBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.access.lists.grouping.AccessLists;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.Classifications;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.FlowsToPortMappings;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.metadata.classification.mappings.grouping.MetadataClassificationMappings;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.metadata.grouping.MetadataElements;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.*;
//import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ClassificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.service.function.chains.SfType;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.service.function.chains.SfTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.instances.grouping.ServiceFunctionInstances;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.types.grouping.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The class containing all methods related to storing the NSH Manager Registry
 * configuration in the distributed MD-SAL data-store.
 */
public class NSHManagerRegistryImpl {
    private static final Logger logger = LoggerFactory.getLogger(NSHManagerRegistryImpl.class);

    private static NSHManagerRegistryImpl nshManagerRegistry;
    private DataBroker dataBroker;
    public static long transactionIDCounter = 0L;

    /**
     * Default constructor - TODO: Make consistent across all modules.
     */
    private NSHManagerRegistryImpl() {
    }

    /**
     * Return an instance of the OrchestratorRegistryImpl.
     *
     * @return Singleton instance
     */
    public static NSHManagerRegistryImpl getInstance() {
        if (nshManagerRegistry == null)
            nshManagerRegistry = new NSHManagerRegistryImpl();

        return nshManagerRegistry;
    }

    /**
     * Sets the OrchestratorRegistry DB Broker and automatically initializes the data trees (nshmanager-status, nshmanager-stats and admin-config).
     * In addition, for testing purposes some info about nshmanager and qosorchestrator is initialized on datastore. Notice that nshmanager ip is
     * initialized with the IP value returned by the method java.net.InetAddress.getLocalHost().
     */
    public void setDb(DataBroker db) {
        this.dataBroker = db;
        initializeNSHManagerRegistryDataTree();
    }


    public void initializeNSHManagerRegistryDataTree() {
        logger.info("Preparing to initialize the NSH Manager Registry Datatree");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
        } else {

            // Initialize the NSH Manager data model on registry
            WriteTransaction nshManagerDataStoreTransaction = dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<NshManagerDM> nshManagerDataStoreIId = InstanceIdentifier.create(NshManagerDM.class);

            List<ServiceFunctionInstances> serviceFunctionInstancesList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances serviceFunctionInstances = new ServiceFunctionInstancesBuilder()
                    .setServiceFunctionInstances(serviceFunctionInstancesList)
                    .build();
            List<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.ServiceFunctionChains> serviceFunctionChainsList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains serviceFunctionChains = new ServiceFunctionChainsBuilder()
                    .setServiceFunctionChains(serviceFunctionChainsList)
                    .build();
            List<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.types.grouping.ServiceFunctionTypes> serviceFunctionTypesList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes serviceFunctionTypes = new ServiceFunctionTypesBuilder()
                    .setServiceFunctionTypes(serviceFunctionTypesList)
                    .build();
            List<AccessLists> accessListList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists accessLists = new AccessListsBuilder()
                    .setAccessLists(accessListList)
                    .build();
            List<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.Classifications> classificationList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications classifications = new ClassificationsBuilder()
                    .setClassifications(classificationList)
                    .build();
            List<MetadataElements> metadataList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Metadata metadata = new MetadataBuilder()
                    .setMetadataElements(metadataList)
                    .build();
            List<MetadataClassificationMappings> metadataClassificationMappingList = new ArrayList<>();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings metadataClassificationMappings = new MetadataClassificationMappingsBuilder()
                    .setMetadataClassificationMappings(metadataClassificationMappingList)
                    .build();
            List<FlowsToPortMappings> flowsToPortMappings = new ArrayList<>();
            MatchFlowsPerPort matchFlowsPerPort = new MatchFlowsPerPortBuilder()
                    .setFlowsToPortMappings(flowsToPortMappings)
                    .build();
            NshManagerDM nshManagerDM = new NshManagerDMBuilder()
                    .setTopologyInfo(new TopologyInfoBuilder().build())
                    .setServiceFunctionChains(serviceFunctionChains)
                    .setServiceFunctionTypes(serviceFunctionTypes)
                    .setServiceFunctionInstances(serviceFunctionInstances)
                    .setAccessLists(accessLists)
                    .setClassifications(classifications)
                    .setMetadata(metadata)
                    .setMetadataClassificationMappings(metadataClassificationMappings)
                    .setMatchFlowsPerPort((matchFlowsPerPort))
                    .build();

            nshManagerDataStoreTransaction.put(LogicalDatastoreType.OPERATIONAL, nshManagerDataStoreIId, nshManagerDM);
            CheckedFuture<Void, TransactionCommitFailedException> futureNetworkGraphStore = nshManagerDataStoreTransaction.submit();

            Futures.addCallback(futureNetworkGraphStore, new FutureCallback<Void>() {
                        public void onSuccess(Void v) {
                            logger.info("NSH Manager Registry Datatree initialized");
                        }

                        public void onFailure(Throwable thrown) {
                            logger.info("NSH Manager Registry Datatree initialization failed.");
                        }
                    }
            );
        }
    }


    public boolean writeTopologyInfoToDatastore(TopologyInfo topologyInfo) {
        logger.info("Writing topology info on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<TopologyInfo> topologyInfoInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(TopologyInfo.class);
        transaction.put(LogicalDatastoreType.OPERATIONAL, topologyInfoInstanceIdentifier, topologyInfo);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();

        Futures.addCallback(future, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("topology info successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting topology info to registry failed.");
            }
        });

        // wait to finish writing
        while (!future.isDone()) {
            continue;
        }

        boolean isCompleted = !future.isCancelled() && future.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }


    public TopologyInfo getTopologyInfoFromDatastore() {
        logger.info("Retrieving Topology Info from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        TopologyInfo topologyInfo = null;
        InstanceIdentifier<TopologyInfo> topologyInfoInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(TopologyInfo.class);
        ;
        CheckedFuture<Optional<TopologyInfo>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, topologyInfoInstanceIdentifier);
        Optional<TopologyInfo> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading Topology Info failed:", e);
        }
        if (optional.isPresent()) {
            topologyInfo = optional.get();
        } else {
            logger.info("Tried to retrieve Topology Info from datastore but not found");
            return null;
        }

        return topologyInfo;
    }


    public Metadata getMetadataFromDatastore() {
        logger.info("Retrieving metadata from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<Metadata> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(Metadata.class);
        CheckedFuture<Optional<Metadata>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<Metadata> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading metadata failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve metadata from datastore but not found");
            return null;
        }
    }


    public boolean addMetadataToDatastore(MetadataElements metadata) {
        logger.info("adding metadata on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Metadata metadata_new = null;
        InstanceIdentifier<Metadata> metadataInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(Metadata.class);
        CheckedFuture<Optional<Metadata>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, metadataInstanceIdentifier);
        Optional<Metadata> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading metadata failed:", e);
        }
        NshManagerDMBuilder networkGraphBuilder = new NshManagerDMBuilder();
        if (optional.isPresent()) {
            metadata_new = optional.get();
            metadata_new.getMetadataElements().add(metadata);
        } else {
            logger.warn("Tried to update metadata on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, metadataInstanceIdentifier, metadata_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("metadata successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding metadata to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMetadataFromDatastore(String metadataName) {
        logger.info("removing metadata from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        Metadata metadata_new = null;
        InstanceIdentifier<Metadata> metadataInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(Metadata.class);
        CheckedFuture<Optional<Metadata>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, metadataInstanceIdentifier);
        Optional<Metadata> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading metadata failed:", e);
        }
        if (optional.isPresent()) {
            metadata_new = optional.get();
            int ind = 0;
            boolean found = false;
            for (MetadataElements p : metadata_new.getMetadataElements()) {
                if (p.getName().equals(metadataName)) {
                    found = true;
                    break;
                }
                ind++;
            }
            if (!found) {
                logger.warn("Tried to remove metadata element with name {} from metadata but not found", metadataName);
                return false;
            }
            metadata_new.getMetadataElements().remove(ind);
        } else {
            logger.warn("Tried to remove metadata element on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, metadataInstanceIdentifier, metadata_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("metadata element successfully removed from registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting metadata to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances getServiceFunctionInstancesFromDatastore() {
        logger.info("Retrieving serviceFunctionInstances from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionInstances failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve serviceFunctionInstances from datastore but not found");
            return null;
        }
    }

    public boolean addServiceFunctionTypeToDatastore(String sfName, String sfType) {

        logger.info("adding SFT on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes serviceFunctionTypes_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes> serviceFunctionTypesInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, serviceFunctionTypesInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionTypes failed:", e);
        }
        if (optional.isPresent()) {
            serviceFunctionTypes_new = optional.get();
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.types.grouping.ServiceFunctionTypes serviceFunctionTypes = new org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.types.grouping.ServiceFunctionTypesBuilder()
                    .setName(sfName)
                    .setSfType(sfType)
                    .build();
            serviceFunctionTypes_new.getServiceFunctionTypes().add(serviceFunctionTypes);
        } else {
            logger.warn("Tried to update serviceFunctionTypes on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, serviceFunctionTypesInstanceIdentifier, serviceFunctionTypes_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("serviceFunctionTypes successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding serviceFunctionTypes to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes getServiceFunctionTypesFromDatastore() {
        logger.info("Retrieving serviceFunctionTypes from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionTypes> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionTypes failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve serviceFunctionTypes from datastore but not found");
            return null;
        }
    }

    public boolean addServiceFunctionChainToDatastore(String sfcName, List<String> serviceFunctions) {
        logger.info("adding SFC on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains serviceFunctionChains_new = null;
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains serviceFunctionChains_new2 = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains> serviceFunctionChainsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, serviceFunctionChainsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionChains failed:", e);
        }
        if (optional.isPresent()) {
            serviceFunctionChains_new = optional.get();
            Long nextId = 0L;
            if (serviceFunctionChains_new.getCurrId() != null) {
                nextId = new Long(serviceFunctionChains_new.getCurrId() + 1);
            }
            List<SfType> sfTypes = new ArrayList<>();
            int i = 0;
            for (String sf : serviceFunctions) {
                SfType t = new SfTypeBuilder()
                        .setName(sf)
                        .setIndex(i++)
                        .build();
                sfTypes.add(t);
            }
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.ServiceFunctionChains serviceFunctionChains = new org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.ServiceFunctionChainsBuilder()
                    .setName(sfcName)
                    .setId(nextId)
                    .setSfType(sfTypes)
                    .build();
            List<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.service.function.chains.grouping.ServiceFunctionChains> newChains = serviceFunctionChains_new.getServiceFunctionChains();
            newChains.add(serviceFunctionChains);
            serviceFunctionChains_new2 = new
                    org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChainsBuilder()
                    .setServiceFunctionChains(newChains)
                    .setCurrId(nextId)
                    .build();
        } else {
            logger.warn("Tried to update serviceFunctionChains on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, serviceFunctionChainsInstanceIdentifier, serviceFunctionChains_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("serviceFunctionChains successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding serviceFunctionChains to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains getServiceFunctionChainsFromDatastore() {
        logger.info("Retrieving serviceFunctionChains from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionChains> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionChains failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve serviceFunctionChains from datastore but not found");
            return null;
        }
    }

    public boolean addServiceFunctionInstancesToDatastore(ServiceFunctionInstances serviceFunctionInstances) {
        logger.info("adding SFI on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances serviceFunctionInstances_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances> serviceFunctionInstancesInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, serviceFunctionInstancesInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionInstances failed:", e);
        }
        if (optional.isPresent()) {
            serviceFunctionInstances_new = optional.get();
            serviceFunctionInstances_new.getServiceFunctionInstances().add(serviceFunctionInstances);
        } else {
            logger.warn("Tried to update serviceFunctionInstances on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, serviceFunctionInstancesInstanceIdentifier, serviceFunctionInstances_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("serviceFunctionInstances successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding serviceFunctionInstances to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeServiceFunctionInstancesFromDatastore(String serviceFunctionInstancesName) {
        logger.info("removing serviceFunctionInstances from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances serviceFunctionInstances_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances> serviceFunctionInstancesInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, serviceFunctionInstancesInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ServiceFunctionInstances> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading serviceFunctionInstances failed:", e);
        }
        if (optional.isPresent()) {
            serviceFunctionInstances_new = optional.get();
            int ind = 0;
            boolean found = false;
            for (ServiceFunctionInstances p : serviceFunctionInstances_new.getServiceFunctionInstances()) {
                if (p.getName().equals(serviceFunctionInstancesName)) {
                    found = true;
                    break;
                }
                ind++;
            }
            if (!found) {
                logger.warn("Tried to remove serviceFunctionInstances element with name {} from serviceFunctionInstances but not found", serviceFunctionInstancesName);
                return false;
            }
            serviceFunctionInstances_new.getServiceFunctionInstances().remove(ind);
        } else {
            logger.warn("Tried to remove serviceFunctionInstances element on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, serviceFunctionInstancesInstanceIdentifier, serviceFunctionInstances_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("serviceFunctionInstances element successfully removed from registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting serviceFunctionInstances to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists getAccessListsFromDatastore() {
        logger.info("Retrieving accessLists from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading accessLists failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve accessLists from datastore but not found");
            return null;
        }
    }


    public boolean addAccessListsToDatastore(AccessLists accessLists) {
        logger.info("adding ACL on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists accessLists_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists> accessListsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, accessListsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading accessLists failed:", e);
        }
        if (optional.isPresent()) {
            accessLists_new = optional.get();
            accessLists_new.getAccessLists().add(accessLists);
        } else {
            logger.warn("Tried to update accessLists on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, accessListsInstanceIdentifier, accessLists_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("accessLists successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding accessLists to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAccessListsFromDatastore(String accessListsName) {
        logger.info("removing accessLists from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists accessLists_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists> accessListsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, accessListsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.AccessLists> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading accessLists failed:", e);
        }
        if (optional.isPresent()) {
            accessLists_new = optional.get();
            int ind = 0;
            boolean found = false;
            for (AccessLists p : accessLists_new.getAccessLists()) {
                if (p.getName().equals(accessListsName)) {
                    found = true;
                    break;
                }
                ind++;
            }
            if (!found) {
                logger.warn("Tried to remove accessLists element with name {} from accessLists but not found", accessListsName);
                return false;
            }
            accessLists_new.getAccessLists().remove(ind);
        } else {
            logger.warn("Tried to remove accessLists element on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, accessListsInstanceIdentifier, accessLists_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("accessLists element successfully removed from registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting accessLists to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications getClassificationsFromDatastore() {
        logger.info("Retrieving classifications from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading classifications failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve classifications from datastore but not found");
            return null;
        }
    }

    public Long getClassificationsCurrIdFromDatastore() {
        logger.info("Retrieving classifications currId from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading classifications failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get().getCurrId();
        } else {
            logger.info("Tried to retrieve classifications currId from datastore but not found");
            return null;
        }
    }


    public boolean addClassificationsToDatastore(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.Classifications classifications) {
        logger.info("adding classification on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications classifications_new = null;
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications classifications_new2 = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> classificationsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, classificationsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading classifications failed:", e);
        }
        if (optional.isPresent()) {
            classifications_new = optional.get();
            //classifications_new.getClassifications().add(classifications);
            Long nextId = 0L;
            if (classifications_new.getCurrId() != null) {
                nextId = new Long(classifications_new.getCurrId() + 1);
            }
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.Classifications classificationsWithId = new org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.ClassificationsBuilder()
                    .setAccessListName(classifications.getAccessListName())
                    .setSfcName(classifications.getSfcName())
                    .setId(nextId)
                    .build();
            classifications_new.getClassifications().add(classificationsWithId);
            classifications_new2 = new
                    org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.ClassificationsBuilder()
                    .setClassifications(classifications_new.getClassifications())
                    .setCurrId(nextId)
                    .build();
        } else {
            logger.warn("Tried to update classifications on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, classificationsInstanceIdentifier, classifications_new2);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("classifications successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding classifications to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean setClassificationsToDatastore(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications classifications) {
        logger.info("setting classifications on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> classificationsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications.class);

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, classificationsInstanceIdentifier, classifications);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("Classifications successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting classifications to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeClassificationsFromDatastore(Long classificationsId) {
        logger.info("removing classifications from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications classifications_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> classificationsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, classificationsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.Classifications> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading classifications failed:", e);
        }
        if (optional.isPresent()) {
            classifications_new = optional.get();
            int ind = 0;
            boolean found = false;
            for (org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.classifications.grouping.Classifications p : classifications_new.getClassifications()) {
                if (p.getId().equals(classificationsId)) {
                    found = true;
                    break;
                }
                ind++;
            }
            if (!found) {
                logger.warn("Tried to remove classifications element with name {} from classifications but not found", classificationsId);
                return false;
            }
            classifications_new.getClassifications().remove(ind);
        } else {
            logger.warn("Tried to remove classifications element on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, classificationsInstanceIdentifier, classifications_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("classifications element successfully removed from registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting classifications to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings getMetadataClassificationMappingsFromDatastore() {
        logger.info("Retrieving metadataClassificationMappings from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading metadataClassificationMappings failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve metadataClassificationMappings from datastore but not found");
            return null;
        }
    }


    public boolean addMetadataClassificationMappingsToDatastore(MetadataClassificationMappings metadataClassificationMappings) {
        logger.info("adding metadata classification mapping on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings metadataClassificationMappings_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings> metadataClassificationMappingsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, metadataClassificationMappingsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading metadataClassificationMappings failed:", e);
        }
        if (optional.isPresent()) {
            metadataClassificationMappings_new = optional.get();
            metadataClassificationMappings_new.getMetadataClassificationMappings().add(metadataClassificationMappings);
        } else {
            logger.warn("Tried to update metadataClassificationMappings on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, metadataClassificationMappingsInstanceIdentifier, metadataClassificationMappings_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("metadataClassificationMappings successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding metadataClassificationMappings to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeMetadataClassificationMappingsFromDatastore(String metadataClassificationMappingsId) {
        logger.info("removing metadataClassificationMappings from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings metadataClassificationMappings_new = null;
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings> metadataClassificationMappingsInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings.class);
        CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, metadataClassificationMappingsInstanceIdentifier);
        Optional<org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MetadataClassificationMappings> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading metadataClassificationMappings failed:", e);
        }
        if (optional.isPresent()) {
            metadataClassificationMappings_new = optional.get();
            int ind = 0;
            boolean found = false;
            for (MetadataClassificationMappings p : metadataClassificationMappings_new.getMetadataClassificationMappings()) {
                if (p.getId().equals(metadataClassificationMappingsId)) {
                    found = true;
                    break;
                }
                ind++;
            }
            if (!found) {
                logger.warn("Tried to remove metadataClassificationMappings element with name {} from metadataClassificationMappings but not found", metadataClassificationMappingsId);
                return false;
            }
            metadataClassificationMappings_new.getMetadataClassificationMappings().remove(ind);
        } else {
            logger.warn("Tried to remove metadataClassificationMappings element on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, metadataClassificationMappingsInstanceIdentifier, metadataClassificationMappings_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("metadataClassificationMappings element successfully removed from registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting metadataClassificationMappings to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public MatchFlowsPerPort getMatchFlowsPerPortFromDatastore() {
        logger.info("Retrieving matchFlowsPerPort from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return null;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<MatchFlowsPerPort> apInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(MatchFlowsPerPort.class);
        CheckedFuture<Optional<MatchFlowsPerPort>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, apInstanceIdentifier);
        Optional<MatchFlowsPerPort> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading matchFlowsPerPort failed:", e);
        }
        if (optional.isPresent()) {
            return optional.get();
        } else {
            logger.info("Tried to retrieve matchFlowsPerPort from datastore but not found");
            return null;
        }
    }


    public boolean addFlowsToPortMappingsToDatastore(FlowsToPortMappings flowsToPortMappings) {
        logger.info("adding flows to port mapping on datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        MatchFlowsPerPort matchFlowsPerPort_new = null;
        InstanceIdentifier<MatchFlowsPerPort> MatchFlowsPerPortInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(MatchFlowsPerPort.class);
        CheckedFuture<Optional<MatchFlowsPerPort>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, MatchFlowsPerPortInstanceIdentifier);
        Optional<MatchFlowsPerPort> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading matchFlowsPerPort failed:", e);
        }
        if (optional.isPresent()) {
            matchFlowsPerPort_new = optional.get();
            matchFlowsPerPort_new.getFlowsToPortMappings().add(flowsToPortMappings);
        } else {
            logger.warn("Tried to update matchFlowsPerPort on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, MatchFlowsPerPortInstanceIdentifier, matchFlowsPerPort_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("matchFlowsPerPort successfully set to registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Adding matchFlowsPerPort to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }

    public boolean removeFlowsToPortMappingsFromDatastore(String switchPortId) {
        logger.info("removing flowsToPortMappings from datastore...");
        if (!this.isSetDataBroker()) {
            logger.warn("Data Broker is not set!");
            return false;
        }
        ReadOnlyTransaction transaction = dataBroker.newReadOnlyTransaction();
        MatchFlowsPerPort matchFlowsPerPort_new = null;
        InstanceIdentifier<MatchFlowsPerPort> matchFlowsPerPortInstanceIdentifier = InstanceIdentifier.create(NshManagerDM.class).child(MatchFlowsPerPort.class);
        CheckedFuture<Optional<MatchFlowsPerPort>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, matchFlowsPerPortInstanceIdentifier);
        Optional<MatchFlowsPerPort> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            logger.error("Reading flowsToPortMappings failed:", e);
        }
        if (optional.isPresent()) {
            matchFlowsPerPort_new = optional.get();
            int ind = 0;
            boolean found = false;
            for (FlowsToPortMappings p : matchFlowsPerPort_new.getFlowsToPortMappings()) {
                if (p.getSwitchPortId().equals(switchPortId)) {
                    found = true;
                    break;
                }
                ind++;
            }
            if (found == false) {
                logger.warn("Tried to remove FlowsToPortMapping element with switchPortId {} from FlowsToPortMappings but not found", switchPortId);
                return false;
            }
            matchFlowsPerPort_new.getFlowsToPortMappings().remove(ind);
        } else {
            logger.warn("Tried to remove FlowsToPortMappings element on datastore but not found");
            return false;
        }

        WriteTransaction wTransaction = dataBroker.newWriteOnlyTransaction();
        wTransaction.put(LogicalDatastoreType.OPERATIONAL, matchFlowsPerPortInstanceIdentifier, matchFlowsPerPort_new);
        CheckedFuture<Void, TransactionCommitFailedException> wFuture = wTransaction.submit();

        Futures.addCallback(wFuture, new FutureCallback<Void>() {
            public void onSuccess(Void v) {
                logger.info("FlowsToPortMapping element successfully removed from registry.");
            }

            public void onFailure(Throwable thrown) {
                logger.warn("Setting FlowsToPortMappings to registry failed.");
            }
        });

        // wait to finish writing
        while (!wFuture.isDone()) {
            continue;
        }

        boolean isCompleted = !wFuture.isCancelled() && wFuture.isDone();
        if (isCompleted) {
            return true;
        } else {
            return false;
        }
    }


    private long generateTransactionId() {
        return ++transactionIDCounter;
    }

    /**
     * Checks if data broker is set. It could be the case that some module(s) have not yet loaded when methods of this class are called, e.g., during junit testing
     *
     * @return Boolean indicating whether data broker is set (true) or not (false)
     */
    public boolean isSetDataBroker() {
        if (this.dataBroker == null) {
            return false;
        } else {
            return true;
        }
    }
}
