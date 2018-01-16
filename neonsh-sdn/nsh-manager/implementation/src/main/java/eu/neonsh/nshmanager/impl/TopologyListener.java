/*
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package eu.neonsh.nshmanager.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.*;
import org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.linkchanged.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.opendaylight.yang.gen.v1.urn.eu.virtuwind.monitoring.rev150722.TopologyChanged;
//import org.opendaylight.yang.gen.v1.urn.eu.virtuwind.monitoring.rev150722.TopologyChangedBuilder;

//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;


/**
 * Created by geopet on 04/06/16.
 */
public class TopologyListener implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyListener.class);
    private NotificationProviderService notificationService;

    private final DataBroker dataBroker;

    public TopologyListener(DataBroker dataBroker, NotificationProviderService notificationService) {
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
    }


    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
        LOG.info("Data changed");


        LOG.info("listener");

        if (dataChangeEvent == null) {
            return;
        }
        try {

            List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeList = this.getNodes(dataBroker);


            Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
            Set<InstanceIdentifier<?>> removedPaths = dataChangeEvent.getRemovedPaths();
            Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
            Map<InstanceIdentifier<?>, DataObject> updatedData = dataChangeEvent.getUpdatedData();


            if (createdData != null && !createdData.isEmpty()) {
                Set<InstanceIdentifier<?>> linksIds = createdData.keySet();
                for (InstanceIdentifier<?> linkId : linksIds) {
                    if (Link.class.isAssignableFrom(linkId.getTargetType())) {
                        Link link = (Link) createdData.get(linkId);
                        LOG.info("Start publishing a created link: " + link.toString());
                        //0 for created data
                        sendNotification(nodeList, link, 0);

                    }
                }
            }

            if (removedPaths != null && !removedPaths.isEmpty() && originalData != null && !originalData.isEmpty()) {
                for (InstanceIdentifier<?> instanceId : removedPaths) {
                    if (Link.class.isAssignableFrom(instanceId.getTargetType())) {
                        Link link = (Link) originalData.get(instanceId);

                        // No need to get the value of current speed because link was removed;
                        LinkChangedBuilder linkChangedBuilder = new LinkChangedBuilder().setLink(new LinkBuilder(link).build()).setTopologyUpdate(LinkChanged.TopologyUpdate.LinkRemoved);

                        LOG.info("Start publishing a changed link: " + link.toString());
                        notificationService.publish(linkChangedBuilder.build());


                    }
                }
            }

            if (updatedData != null && !updatedData.isEmpty()) {
                Set<InstanceIdentifier<?>> linksIds = updatedData.keySet();
                for (InstanceIdentifier<?> linkId : linksIds) {
                    if (Link.class.isAssignableFrom(linkId.getTargetType())) {
                        Link link = (Link) updatedData.get(linkId);

                        LOG.info("Start publishing an updated link: " + link.toString());

                        // 1 is for updated data, 0 is for created data
                        sendNotification(nodeList, link, 1);


                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void sendNotification(List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeList, Link link, Integer indicator) {

        LinkChangedBuilder linkChangedBuilder = new LinkChangedBuilder();


        if (indicator == 0) {

            linkChangedBuilder.setTopologyUpdate(LinkChanged.TopologyUpdate.LinkAdded);


        } else {

            linkChangedBuilder.setTopologyUpdate(LinkChanged.TopologyUpdate.LinkUpdated);

        }

        if (link.getLinkId().getValue().contains("host")) {
            org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.linkchanged.Link linkAttributes = new LinkBuilder(link).build();
            linkChangedBuilder.setLink(linkAttributes);

            notificationService.publish(linkChangedBuilder.build());


        } else {


            String nodeToFind = link.getSource().getSourceNode().getValue();
            String outputNodeConnector = link.getSource().getSourceTp().getValue();

            for (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node : nodeList) {

                if (node.getId().getValue().equals(nodeToFind)) {

                    List<NodeConnector> nodeConnectors = node.getNodeConnector();

                    for (NodeConnector nc : nodeConnectors) {

                        if (nc.getId().getValue().equals(outputNodeConnector)) {
// TODO: the following is only temporal! Need to implement other qos, not only bandwidth!!!
                            //This is the one
                            FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);

                            FlowCapableNodeConnectorStatisticsData statData = nc
                                    .getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
                            FlowCapableNodeConnectorStatistics statistics = statData
                                    .getFlowCapableNodeConnectorStatistics();
                            BigInteger packetsTransmitted = BigInteger.ZERO;
                            BigInteger packetErrorsTransmitted = BigInteger.ZERO;
                            if(statistics != null && statistics.getPackets() != null) {
                                packetsTransmitted = statistics.getPackets().getTransmitted();
                                packetErrorsTransmitted = statistics.getTransmitErrors();

                            }

                            Float packetLoss = (packetsTransmitted.floatValue() == 0) ? 0
                                    : packetErrorsTransmitted.floatValue()
                                    / packetsTransmitted.floatValue();

                            LOG.info("Notification service publishes for link: " + link.toString());
                            org.opendaylight.yang.gen.v1.urn.eu.neonsh.nshmanager.rev161017.linkchanged.Link linkAttributes = new LinkBuilder(link).build();
                            linkChangedBuilder.setLink(linkAttributes);

                            notificationService.publish(linkChangedBuilder.build());

                        }
                    }

                }
            }
        }
    }

    /**
     * Method to get  all Nodes from Toplogy nprovided DataBroker
     *
     * @param db - DataBroker to extract data fromn md-sal
     * @return List<Node> - list of nodes found in the topoology
     */

    public static List<Node> getAllNodes(DataBroker db) {
        List<Node> nodeList = new ArrayList<Node>();

        try {
            TopologyId topoId = new TopologyId("flow:1");
            InstanceIdentifier<Topology> nodesIid = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(topoId)).toInstance();
            ReadOnlyTransaction nodesTransaction = db.newReadOnlyTransaction();
            CheckedFuture<Optional<Topology>, ReadFailedException> nodesFuture = nodesTransaction
                    .read(LogicalDatastoreType.OPERATIONAL, nodesIid);
            Optional<Topology> nodesOptional = nodesFuture.checkedGet();

            if (nodesOptional != null && nodesOptional.isPresent())
                nodeList = nodesOptional.get().getNode();
            //System.out.println("\n\n" + nodeList);
            LOG.info("Nodelist: " + nodeList);
            return nodeList;
        } catch (Exception e) {
            LOG.info("Node Fetching Failed");
        }

        return nodeList;
    }

    /**
     * Method to get  all links from Toplogy nprovided DataBroker
     *
     * @param db DataBroker from which toplogy links should be extracted
     * @return List<Link> found in the topology
     */

    public static List<Link> getAllLinks(DataBroker db) {
        List<Link> nodeList = new ArrayList<>();


        try {
            TopologyId topoId = new TopologyId("flow:1");
            InstanceIdentifier<Topology> nodesIid = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class, new TopologyKey(topoId)).toInstance();
            ReadOnlyTransaction nodesTransaction = db.newReadOnlyTransaction();
            CheckedFuture<Optional<Topology>, ReadFailedException> nodesFuture = nodesTransaction
                    .read(LogicalDatastoreType.OPERATIONAL, nodesIid);
            Optional<Topology> nodesOptional = nodesFuture.checkedGet();

            if (nodesOptional != null && nodesOptional.isPresent())
                nodeList = nodesOptional.get().getLink();

            return nodeList;
        } catch (Exception e) {
            LOG.info("Node Fetching Failed");
        }

        return nodeList;
    }

    public static List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> getNodes(DataBroker db) throws ReadFailedException {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeList = new ArrayList<>();
        InstanceIdentifier<Nodes> nodesIid = InstanceIdentifier.builder(
                Nodes.class).build();
        ReadOnlyTransaction nodesTransaction = db.newReadOnlyTransaction();
        CheckedFuture<Optional<Nodes>, ReadFailedException> nodesFuture = nodesTransaction
                .read(LogicalDatastoreType.OPERATIONAL, nodesIid);
        Optional<Nodes> nodesOptional = Optional.absent();
        nodesOptional = nodesFuture.checkedGet();

        if (nodesOptional != null && nodesOptional.isPresent()) {
            nodeList = nodesOptional.get().getNode();
        }
        return nodeList;
    }
}
