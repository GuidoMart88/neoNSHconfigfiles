#!/bin/bash

# Controller access info
controllerUrl="10.0.2.15:8181"
sudo ovs-ofctl del-flows s1
sudo ovs-ofctl add-flow s1 "priority=0 actions=CONTROLLER:65535"
sudo ovs-ofctl del-flows s2
sudo ovs-ofctl add-flow s2 "priority=0 actions=CONTROLLER:65535"
 
### Controller configuration for NeoNSH demo

echo "Make sure (mininet) topology and ODL controller are up and running"
echo "in mininet execute pingall and after it finishes Press enter to continue..."
read stuff
sudo ovs-ofctl del-flows s1
sudo ovs-ofctl del-flows s2


## Controller details 
echo "Press enter to continue..."
read stuff
echo "Configuring topology details on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:manually-enter-topology-info \
-d '{
    "input": {
        "topology-info": {
            "switches": [
                {
                    "switch-id": "s1",
                    "role": [
                        "CLASSIFIER",
                        "FORWARDER"
                    ],
                    "ssh-info": {
                        "ip": "10.0.2.15",
                        "username": "guidomart",
                        "password": "guidomart",
                        "ssh-port": "22"
                    },
                    "src-port": "1",
                    "dest-port": "3"
                },{
                    "switch-id": "s2",
                    "role": [
                        "CLASSIFIER",
                        "FORWARDER"
                    ],
                    "ssh-info": {
                        "ip": "10.0.2.15",
                        "username": "guidomart",
                        "password": "guidomart",
                        "ssh-port": "22"
                    },
                    "src-port": "1",
                    "dest-port": "3"
                }
            ]
        }
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding Service Function (Type) on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/service-function:put-service-function \
-d '{
    "input": {
        "request_reclassification": "false",
        "nsh-aware": "true",
        "name": "SFT1",
        "type": "dpi",
        "sf-data-plane-locator": [
            {
                "name": "dl1"
            }
        ]
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"



echo "Press enter to continue..."
read stuff
echo "Adding Metadata on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:add-metadata \
-d '{
    "input": {
        "metadata": {
            "metadata-elements": [
                {
                    "name": "metadata1",
                    "nsh-metadata": {
                        "npc": "1",
                        "nsc": "0",
                        "spc": "0",
                        "ssc": "0"
                    }
                }
            ]
        }
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding second Metadata on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:add-metadata \
-d '{
    "input": {
        "metadata": {
            "metadata-elements": [
                {
                    "name": "metadata2",
                    "nsh-metadata": {
                        "npc": "2",
                        "nsc": "0",
                        "spc": "0",
                        "ssc": "0"
                    }
                }
            ]
        }
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding SFI on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:add-SFIs \
-d '{
    "input": {
        "SFIs": {
            "service-function-instances": [
                {
                    "name": "SFI1",
                    "sf-type": "SFT1",
                    "topology-info": {
                        "switch-id": "s1",
                        "port": "2"
                    },
                    "management-info": {
                        "associated-metadata": {
                            "metadata-names": [
                                "metadata1"
                            ]
                        },
                        "ssh-info": {
                            "ip": "10.0.2.15",
                            "username": "guidomart",
                            "password": "guidomart",
                            "ssh-port": "22"
                        }
                    }
                }
            ]
        }
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding 2nd SFI on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:add-SFIs \
-d '{
    "input": {
        "SFIs": {
            "service-function-instances": [
                {
                    "name": "SFI2",
                    "sf-type": "SFT1",
                    "topology-info": {
                        "switch-id": "s2",
                        "port": "2"
                    },
                    "management-info": {
                        "associated-metadata": {
                            "metadata-names": [
                                "metadata2"
                            ]
                        },
                        "ssh-info": {
                            "ip": "10.0.2.15",
                            "username": "guidomart",
                            "password": "guidomart",
                            "ssh-port": "22"
                        }
                    }
                }
            ]
        }
    }
}'
echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding SFC on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/service-function-chain:put-service-function-chains \
-d '{
    "input": {
        "service-function-chain": [
            {
                "name": "SFC1",
                "symmetric": "false",
                "sfc-service-function": [
                    {
                        "name": "SFT1",
                        "type": "dpi",
                        "order": "0"
                    }
                ]
            }
        ]
    }
}'


echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding Access List on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:add-access-lists \
-d '{
    "input": {
        "access-lists": {
            "access-lists": [
                {
                    "name": "acl1",
                    "access-list-entries": [
                        {
                         "acle-id": "acle1",
                         "src-ip": "10.0.0.1",
                         "IPprotocol": "1",
                         "metadata-name": "metadata1"
                        }
                    ]
                }
            ]
        }
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

echo "Press enter to continue..."
read stuff
echo "Adding Classification on registry..."
curl --user "admin":"admin" -H "Content-type: application/json" -X POST \
http://$controllerUrl/restconf/operations/nshmanager:add-classification \
-d '{
    "input": {
        "access-list-name": "acl1",
        "sfc-name": "SFC1"
    }
}'

echo ""
echo "Output should be an OK message. To confirm that details were entered correctly on the registry, you can visit 'http://$controllerUrl/restconf/operational/nshmanager-registry:nsh-manager-DM' from a browser on the controller's VM"

# sudo ovs-ofctl add-flow s1 "priority=65000,actions=resubmit(,1)"
sudo ovs-ofctl add-flow s1 "priority=65000,table=1,in_port=1,arp,actions=output:3"
sudo ovs-ofctl add-flow s1 "priority=65000,table=1,in_port=3,arp,actions=output:1"
sudo ovs-ofctl add-flow s1 "priority=65000,table=1,in_port=3,icmp,actions=output:1"
sudo ovs-ofctl add-flow s1 "priority=65001,table=1,nsh_mdtype=1,nsp=0,nsi=254,actions=output:3"
# sudo ovs-ofctl add-flow s2 "priority=65000,actions=resubmit(,1)"
sudo ovs-ofctl add-flow s2 "priority=65000,table=1,in_port=1,arp,actions=output:3"
sudo ovs-ofctl add-flow s2 "priority=65000,table=1,in_port=3,arp,actions=output:1"
sudo ovs-ofctl add-flow s2 "priority=65000,table=1,in_port=3,icmp,actions=output:1"