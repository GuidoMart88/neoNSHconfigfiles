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
    SF3 = net.addHost( 'SF3')
    SF4 = net.addHost( 'SF4')
    SF5 = net.addHost( 'SF5')
    SF6 = net.addHost( 'SF6')
    service=net.addHost('service')
    s1 = net.addSwitch('s1')
    net.addLink(s1,client)
    net.addLink(s1,SF1)
    net.addLink(s1,SF2)
    net.addLink(s1,SF3)
    net.addLink(s1,SF4)
    net.addLink(s1,SF5)
    net.addLink(s1,SF6)
    net.addLink(s1,service)
    net.start()
    s1.cmd('ovs-vsctl set-controller s1 tcp:10.0.2.15:6633')
    CLI( net )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    neonsh_topo()
