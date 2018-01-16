#!/usr/bin/python
 
from mininet.node import *
from mininet.topo import Topo
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.net import Mininet

def neonsh_topo():
    "Create custom topo."
    net = Mininet(controller=None)
    client = net.addHost('client')
    SF1 = net.addHost( 'SF1')
    SF2 = net.addHost( 'SF2')
    service=net.addHost('service')
    s1 = net.addSwitch('s1')
    s2 = net.addSwitch('s2')
    net.addLink(s1,client)
    net.addLink(s1,SF1)
    net.addLink(s1,s2)
    net.addLink(s2,SF2)
    net.addLink(s2,service)
    net.start()
    s1.cmd('ovs-vsctl set-controller s1 tcp:10.0.2.15:6633')
    s2.cmd('ovs-vsctl set-controller s2 tcp:10.0.2.15:6633')
    
    CLI( net )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    neonsh_topo()
