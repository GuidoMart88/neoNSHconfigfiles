package eu.neonsh.registryhandler.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryHandlerProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RegistryHandlerProvider.class);

    /**
     * Instantiates the various registry handler singleton implementations
     * Also implements some basic RPC methods for REST-based evaluation of the data-store functionalities.
     * @param dataBroker The data broker used to apply read- and write transactions in the distributed data-store.
     * @param rpcProviderRegistry The registry of implemented RPCs, allowing the Registry Handler to register its own
     *                            implementation of various RPCs used for testing.
     */
    public RegistryHandlerProvider(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry) {
        NSHManagerRegistryImpl.getInstance().setDb(dataBroker);
    }

    /**
     * Never called.
     */
    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("RegistryHandlerProvider Session Initiated");
    }

    /**
     * Called when Registry Handler is closed.
     * @throws Exception
     */
    @Override
    public void close() throws Exception { LOG.info("RegistryHandlerProvider Closed"); }
}
