package eu.neonsh.nshmanager.impl;

import eu.neonsh.nshmanager.impl.DataListeners.*;
import eu.neonsh.nshmanager.impl.Entities.sfcManagerLists;
import eu.neonsh.nshmanager.impl.Entities.vtnServices;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.NshmanagerService;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;

public class NSHManagerProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NSHManagerProvider.class);
    private BindingAwareBroker.RpcRegistration<NshmanagerService> negotiatorService;

    public static String Enviroment;
    //Members related to MD-SAL operations
    private DataBroker dataBroker;
    private NotificationProviderService notificationService;
    private SalFlowService salFlowService;
    final SfEntryDataListener SfEntryDataListener;
    final SfcEntryDataListener SfcEntryDataListener;
    final AclEntryDataListener AclEntryDataListener;
    final ScfEntryDataListener ScfEntryDataListener;
    public static vtnServices vtnServices;
    public sfcManagerLists lists;

    public NSHManagerProvider(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry,
                              NotificationProviderService notificationService, OpendaylightSfc opendaylightSfc) {
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
        this.salFlowService = rpcProviderRegistry.getRpcService(SalFlowService.class);
        this.negotiatorService = rpcProviderRegistry.addRpcImplementation(NshmanagerService.class,
                new NSHManagerImpl(this.dataBroker, this.notificationService, this.salFlowService));
        this.lists = new sfcManagerLists();

        //vtnServices = new vtnServices(rpcProviderRegistry);
        SfEntryDataListener = new SfEntryDataListener(this, opendaylightSfc, vtnServices, lists, dataBroker);
        SfcEntryDataListener = new SfcEntryDataListener(this, opendaylightSfc, vtnServices, lists, dataBroker);
        AclEntryDataListener = new AclEntryDataListener(opendaylightSfc, vtnServices, lists);
        ScfEntryDataListener = new	 ScfEntryDataListener(opendaylightSfc,vtnServices);
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NSHManagerProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("NSHManagerProvider Closed");
    }

}
