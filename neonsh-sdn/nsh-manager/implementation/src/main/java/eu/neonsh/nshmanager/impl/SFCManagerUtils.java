package eu.neonsh.nshmanager.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import eu.neonsh.nshmanager.impl.Entities.AccessList;
import eu.neonsh.nshmanager.impl.Entities.AccessListEntry;
import eu.neonsh.nshmanager.impl.Entities.AnSshConnector;
import eu.neonsh.registryhandler.impl.NSHManagerRegistryImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.FlowsToPortMappings;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.FlowsToPortMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.flows.to.port.mappings.Flows;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.match.flows.per.port.grouping.flows.to.port.mappings.FlowsBuilder;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.registryhandler.nshmanager.registry.rev161017.nsh.manager.dm.MatchFlowsPerPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanPcp;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;


public class SFCManagerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(SFCManagerUtils.class);
    public static AtomicInteger FlowsCreated = new AtomicInteger(1);
    public static String SwitchName;
    public static int inputPort;
    public static NSHManagerRegistryImpl nri;

    public static String createClassificationFlow(int inputPort, String srcIP, String nspHEX, String metadataHEX, String SwitchName, String username, String password, String IP) throws NoSuchAlgorithmException {
        String flow;
        String command;
        Random rand = new Random();
        int cookie = rand.nextInt();
        cookie = cookie >= 0 ? cookie : -cookie;
        flow = String.format("in_port=%d,%s,actions= push_nsh, " +
                "load:0x%s->NXM_NX_NSP[0..23], load:0xFF->NXM_NX_NSI[], " + // setting NSP to input nsp hex value and NSI to 255
                "load:0x1->NXM_NX_NSH_MDTYPE[], load:0x3->NXM_NX_NSH_NP[], " + // setting md type to 1 (i.e., mandatory context fields) and NSH NP to 3 (denoting L2 Frame)
                "load:0x%s->NXM_NX_NSH_C1[], load:0x55667788->NXM_NX_NSH_C2[], " + // setting first 2 context headers
                "load:0x99aabbcc->NXM_NX_NSH_C3[], load:0xddeeff00->NXM_NX_NSH_C4[]," + // setting last 2 context headers
                " resubmit(,1)", inputPort, srcIP, nspHEX, metadataHEX);

        command = String.format("sudo ovs-ofctl add-flow %s \"cookie=%d, table=0, priority=65535,idle_timeout=50000,%s \"", SwitchName, cookie, flow);

        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);
        return flow.substring(0, flow.indexOf(",actions="));
    }

    /**
     * Creates flow on table 0 that sends all (unmatched by higher priority flows) packets to table 1
     *
     * @param SwitchName
     * @param username
     * @param password
     * @param IP
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String createDefaultFlow(String SwitchName, String username, String password, String IP,String nspHEX) throws NoSuchAlgorithmException {
        String flow;
        String command;
        flow = String.format(",actions=resubmit(,1)");

//        command = String.format("sudo ovs-ofctl add-flow %s \",table=0,priority=65000,idle_timeout=50000,%s\"", SwitchName, flow);
        command = String.format("sudo ovs-ofctl add-flow %s \",priority=65000,idle_timeout=50000,%s\"", SwitchName, flow);

        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);

        return flow.substring(0, flow.indexOf(",actions="));
    }

    public static String createMatchNSHFlow(String SwitchName, String nspHEX, String nsiHEX, String metadataHEX, int outPort, String username, String password, String IP) throws NoSuchAlgorithmException {
        String flow;
        String command;
        Random rand = new Random();
        int cookie = rand.nextInt();
        cookie = cookie >= 0 ? cookie : -cookie;
        flow = String.format("nsh_mdtype=1,nsp=0x%s,nsi=0x%s,nshc1=0x%s,actions=output:%d", nspHEX, nsiHEX, metadataHEX, outPort);

        command = String.format("sudo ovs-ofctl add-flow  %s \"cookie=%d,table=1,priority=65535,idle_timeout=50000,%s\"", SwitchName, cookie, flow);
        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);

        // adding flow to registry under specific port (outPort)
        String switchPortId = SwitchName+"@@"+Integer.toString(outPort);
        nri = SFCManagerUtils.getNSHManagerRegistryImpl();
        if (nri != null) {
            // get all match flows per port
            MatchFlowsPerPort flowsPerPorts = nri.getMatchFlowsPerPortFromDatastore();
            // find the ones that match our outPort
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

                Flows newFlow = new FlowsBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setSwitchName(SwitchName)
                        .setNspHEX(nspHEX)
                        .setNsiHEX(nsiHEX)
                        .setMetadataHEX(metadataHEX)
                        .setUsername(username)
                        .setPassword(password)
                        .setIP(IP)
                        .build();

                // if it is the first mapping entry for this port
                if (found == false) {
                    List<Flows> flows = new ArrayList<>();
                    flows.add(newFlow);
                    FlowsToPortMappings ftpm = new FlowsToPortMappingsBuilder()
                            .setSwitchPortId(switchPortId)
                            .setSwitchId(SwitchName)
                            .setPort(Integer.toString(outPort))
                            .setFlows(flows)
                            .build();
                    nri.addFlowsToPortMappingsToDatastore(ftpm);
                } else {
                    fppInReg.getFlows().add(newFlow);
                    nri.removeFlowsToPortMappingsFromDatastore(switchPortId);
                    nri.addFlowsToPortMappingsToDatastore(fppInReg);
                }
            }
        }

        return flow.substring(0, flow.indexOf(",actions="));
    }

    public static String createPopNSHEgressFlow(String SwitchName, String nspHEX, String nsiHEX, String metadataHEX, int outPort, String username, String password, String IP) throws NoSuchAlgorithmException {
        String flow;
        String command;
        Random rand = new Random();
        int cookie = rand.nextInt();
        cookie = cookie >= 0 ? cookie : -cookie;
        flow = String.format("nsp=0x%s,nsi=0x%s,nshc1=0x%s,actions=pop_nsh,output:%d", nspHEX, nsiHEX, metadataHEX, outPort);

        command = String.format("sudo ovs-ofctl add-flow %s \" cookie=%d, table=0, priority=65534,idle_timeout=50000,%s\"", SwitchName, cookie, flow);

        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);

        return flow.substring(0, flow.indexOf(",actions="));
    }
public static String createUpdateMatchNSHFlow(String SwitchName, String nspHEX, String nsiHEX, String metadataHEX, int outPort, String username, String password, String IP) throws NoSuchAlgorithmException {
        String flow;
        String command;
        Random rand = new Random();
        int cookie = rand.nextInt();
        cookie = cookie >= 0 ? cookie : -cookie;
        flow = String.format("nsh_mdtype=1,nsp=0x%s,nsi=0x%s,nshc1=0x%s,actions=output:%d", nspHEX, nsiHEX, metadataHEX, outPort);

        command = String.format("sudo ovs-ofctl mod-flows  %s \"table=1,priority=65535,%s\"", SwitchName,flow);
        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:");
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);

        // adding flow to registry under specific port (outPort)
        nri = SFCManagerUtils.getNSHManagerRegistryImpl();
        if (nri != null) {
            // get all match flows per port
            MatchFlowsPerPort flowsPerPorts = nri.getMatchFlowsPerPortFromDatastore();
            // find the ones that match our outPort
            if (flowsPerPorts != null) {
                boolean found = false;
                FlowsToPortMappings fppInReg = null;
                for (FlowsToPortMappings fPerPort : flowsPerPorts.getFlowsToPortMappings()) {
                    if (fPerPort.getPort().equals(Integer.toString(outPort))) {
                        fppInReg = fPerPort;
                        found = true;
                        break;
                    }
                }

                Flows newFlow = new FlowsBuilder()
                        .setId(UUID.randomUUID().toString())
                        .setSwitchName(SwitchName)
                        .setNspHEX(nspHEX)
                        .setNsiHEX(nsiHEX)
                        .setMetadataHEX(metadataHEX)
                        .setUsername(username)
                        .setPassword(password)
                        .setIP(IP)
                        .build();

                // if it is the first mapping entry for this port
                if (found == false) {
                    List<Flows> flows = new ArrayList<>();
                    flows.add(newFlow);
                    FlowsToPortMappings ftpm = new FlowsToPortMappingsBuilder()
                            .setPort(Integer.toString(outPort))
                            .setFlows(flows)
                            .build();
                    nri.addFlowsToPortMappingsToDatastore(ftpm);
                } else {
                    fppInReg.getFlows().add(newFlow);
                    nri.removeFlowsToPortMappingsFromDatastore(Integer.toString(outPort));
                    nri.addFlowsToPortMappingsToDatastore(fppInReg);
                }
            }
        }

        return flow.substring(0, flow.indexOf(",actions="));
    }

    public static void deleteFlow(String SwitchName, String flow, String username, String password, String IP) {
        String command;
        command = String.format("sudo ovs-ofctl del-flows %s %s", SwitchName, flow);
        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);
    }

    public static void deleteAllFlows(String SwitchName, String username, String password, String IP) {
        String command;
        command = String.format("sudo ovs-ofctl del-flows %s", SwitchName);
        LOG.info("deleting all flows on switch: {}", SwitchName);
        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);
    }

    public static NSHManagerRegistryImpl getNSHManagerRegistryImpl() {
        if (nri == null) {
            return NSHManagerRegistryImpl.getInstance();
        } else {
            return nri;
        }
    }

    public static String createExitSwitchFlow(String switchId, String nspHEX, String nsiHEX, int outPort, String username, String password, String IP) {
        String flow;
        String command;
        Random rand = new Random();
        int cookie = rand.nextInt();
        cookie = cookie >= 0 ? cookie : -cookie;
        flow = String.format("nsh_mdtype=1,nsp=0x%s,nsi=0x%s,actions=output:%d", nspHEX, nsiHEX, outPort);

        command = String.format("sudo ovs-ofctl add-flow  %s \"cookie=%d,table=1,priority=65001,idle_timeout=50000,%s\"", switchId, cookie, flow);
        LOG.info("sending via ssh to IP {}, with username {} and password {} the command:",IP,username,password);
        LOG.info(command);

        String sshOutput = AnSshConnector.executeCommand(command, username, password, IP, 22);
        LOG.info("received ssh output:");
        LOG.info(sshOutput);

        return flow.substring(0, flow.indexOf(",actions="));
    }
}
