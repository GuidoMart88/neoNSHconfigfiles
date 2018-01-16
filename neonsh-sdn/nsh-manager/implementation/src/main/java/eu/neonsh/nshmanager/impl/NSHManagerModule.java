package eu.neonsh.nshmanager.impl;

import eu.neonsh.nshmanager.impl.Entities.AnSshConnector;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;


public class NSHManagerModule extends eu.neonsh.nshmanager.impl.AbstractNSHManagerModule {
    private static final Logger logger = LoggerFactory.getLogger(NSHManagerModule.class);

    public NSHManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NSHManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, eu.neonsh.nshmanager.impl.NSHManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        logger.info("Creating the NSH Manager Instance...");
        DataBroker dataBrokerService = getDataBrokerDependency();
        RpcProviderRegistry rpcProviderRegistry = getRpcRegistryDependency();
        NotificationProviderService notificationService = getNotificationServiceDependency();
        final OpendaylightSfc opendaylightSfc = OpendaylightSfc.getOpendaylightSfcObj();
        opendaylightSfc.setDataProvider(dataBrokerService);
        NSHManagerProvider provider = new NSHManagerProvider(dataBrokerService, rpcProviderRegistry, notificationService,opendaylightSfc);
        InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("flow:1"))).child(Link.class).build();
        dataBrokerService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, linkInstance,
                new TopologyListener(dataBrokerService, notificationService),
                AsyncDataBroker.DataChangeScope.BASE);
//        System.out.println("SSH username "+AnSshConnector.sshinfo.getUsername());
//        System.out.println("SSH IP "+AnSshConnector.sshinfo.getIp());
        return provider;


    }

}
