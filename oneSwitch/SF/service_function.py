from socket import *
import fcntl
import struct
from scapy.all import interact, bind_layers, sendp, conf, sniff
from scapy.fields import BitField, ByteField, ByteEnumField, X3BytesField, XIntField
from scapy.fields import ConditionalField, FieldLenField
from scapy.packet import Packet
from scapy.layers.inet import Ether, IP, UDP
from scapy.layers.inet6 import IPv6
from scapy.all import bind_layers
from scapy.fields import BitField, ByteField, ByteEnumField
from scapy.fields import ShortField, X3BytesField, XIntField
from scapy.fields import ConditionalField, PacketListField, BitFieldLenField
from scapy.layers.inet import Ether, IP
from scapy.layers.inet6 import IPv6
from scapy.layers.vxlan import VXLAN
from scapy.packet import Packet
from scapy.layers.l2 import GRE
from scapy.packet import Packet, bind_layers, Padding
from scapy.fields import BitField,ByteField
from scapy.layers.inet import IP
from scapy.layers.inet6 import IPv6
from scapy.layers.l2 import Ether, GRE

class MPLS(Packet):
   name = "MPLS"
   fields_desc =  [ BitField("label", 3, 20),
                    BitField("cos", 0, 3),
                    BitField("s", 1, 1),
                    ByteField("ttl", 0)  ]

   def guess_payload_class(self, payload):
       if len(payload) >= 1:
           if not self.s:
              return MPLS
           ip_version = (orb(payload[0]) >> 4) & 0xF
           if ip_version == 4:
               return IP
           elif ip_version == 6:
               return IPv6
       return Padding

bind_layers(Ether, MPLS, type=0x8847)
bind_layers(GRE, MPLS, proto=0x8847)
bind_layers(MPLS, MPLS, s=0)

class Metadata(Packet):
    name = 'NSH metadata'
    fields_desc = [XIntField('value', 0)]


class NSHTLV(Packet):
    "NSH MD-type 2 - Variable Length Context Headers"
    name = "NSHTLV"
    fields_desc = [
        ShortField('Class', 0),
        BitField('Critical', 0, 1),
        BitField('Type', 0, 7),
        BitField('Reserved', 0, 3),
        BitField('Len', 0, 5),
        PacketListField('Metadata', None, XIntField, count_from='Len')
    ]


class NSH(Packet):
    """Network Service Header.
       NSH MD-type 1 if there is no ContextHeaders"""
    name = "NSH"

    fields_desc = [
        BitField('Ver', 0, 2),
        BitField('OAM', 0, 1),
        BitField('Critical', 0, 1),
        BitField('Reserved', 0, 6),
        BitFieldLenField('Len', None, 6,
                         count_of='ContextHeaders',
                         adjust=lambda pkt, x: 6 if pkt.MDType == 1 else x + 2),
        ByteEnumField('MDType', 1, {1: 'Fixed Length',
                                    2: 'Variable Length'}),
        ByteEnumField('NextProto', 3, {1: 'IPv4',
                                       2: 'IPv6',
                                       3: 'Ethernet',
                                       4: 'NSH',
                                       5: 'MPLS'}),
        X3BytesField('NSP', 0),
        ByteField('NSI', 1),
        ConditionalField(XIntField('NPC', 0), lambda pkt: pkt.MDType == 1),
        ConditionalField(XIntField('NSC', 0), lambda pkt: pkt.MDType == 1),
        ConditionalField(XIntField('SPC', 0), lambda pkt: pkt.MDType == 1),
        ConditionalField(XIntField('SSC', 0), lambda pkt: pkt.MDType == 1),
        ConditionalField(PacketListField("ContextHeaders", None,
                                         NSHTLV, count_from="Length"),
                         lambda pkt: pkt.MDType == 2)
        ]

    def mysummary(self):
        return self.sprintf("NSP: %NSP% - NSI: %NSI%")


bind_layers(Ether, NSH, {'type': 0x894F}, type=0x894F)
bind_layers(VXLAN, NSH, {'flags': 0xC, 'NextProtocol': 4}, NextProtocol=4)
bind_layers(GRE, NSH, {'proto': 0x894F}, proto=0x894F)

bind_layers(NSH, IP, {'NextProto': 1}, NextProto=1)
bind_layers(NSH, IPv6, {'NextProto': 2}, NextProto=2)
bind_layers(NSH, Ether, {'NextProto': 3}, NextProto=3)
bind_layers(NSH, NSH, {'NextProto': 4}, NextProto=4)
bind_layers(NSH, MPLS, {'NextProto': 5}, NextProto=5)




def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])


def NSHForward(pkt):
	if pkt.haslayer(NSH):
	    print ("{" + pkt[NSH].mysummary()+"} "+pkt.summary())
	    next = pkt
	    next[NSH].NSI = next[NSH].NSI - 1
	   
	    sendp(next, iface=egress)

def StartSF():
    conf.verb = 0
    return sniff(iface=ingress,filter='inbound',prn=lambda x: NSHForward(x))


ingress = "eth1"
egress = "eth1"


if __name__ == "__main__":
    interact(mydict=globals(), mybanner="""
    Scapy with VxLAN GPE and NSH Support
    - Use sniff(iface=ingress) to display incoming packets
    - Use StartSF() to forward packets
    """)
