import time

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.node import RemoteController
from mininet.link import TCLink


class MyTopo(Topo):

    def __init__(self):
        "Create custom topo."
        # Initialize topology
        Topo.__init__(self)
        # Add hosts and switches
        H1 = self.addHost('h1')
        H2 = self.addHost('h2')
        H3 = self.addHost('h3')
        SERV1 = self.addHost('serv1')
        SERV2 = self.addHost('serv2')
        SERV3 = self.addHost('serv3')
        SERV4 = self.addHost('serv4')
        SERV5 = self.addHost('serv5')
        SERV6 = self.addHost('serv6')
        S1 = self.addSwitch('s1')
        devices = [H1, H2, H3, SERV1, SERV2, SERV3, SERV4, SERV5, SERV6]
        for device in devices:
            self.addLink(device, S1, cls=TCLink, bw=100)


def start_servers(net):
    print("starting servers...")
    net.hosts[3].cmd("python3 -m http.server &")
    net.hosts[4].cmd("python3 -m http.server &")
    net.hosts[5].cmd("python3 -m http.server &")
    net.hosts[6].cmd("python3 -m http.server &")
    net.hosts[7].cmd("python3 -m http.server &")
    net.hosts[8].cmd("python3 -m http.server &")


def set_static_mac_ip(net):
    print("setting static MAC and IP addresses...")
    net.hosts[0].config(mac="1e:ec:71:6e:4a:ea", ip='10.0.0.1')
    net.hosts[1].config(mac="a6:a0:01:5d:57:b5", ip='10.0.0.2')
    net.hosts[2].config(mac="ca:07:21:25:5a:35", ip='10.0.0.3')
    net.hosts[3].config(mac="a2:74:18:73:c1:ef", ip='10.0.0.4')
    net.hosts[4].config(mac="72:ec:00:c1:f2:ed", ip='10.0.0.5')
    net.hosts[5].config(mac="32:28:de:47:35:52", ip='10.0.0.6')
    net.hosts[6].config(mac="2a:27:a3:72:7d:eb", ip='10.0.0.7')
    net.hosts[7].config(mac="da:23:46:f6:00:bb", ip='10.0.0.8')
    net.hosts[8].config(mac="2e:78:50:52:54:24", ip='10.0.0.9')


def set_arp(net):
    print("setting ARP on hosts...")
    net.hosts[0].cmd("arp -s 10.0.0.5 ff:ff:ff:ff:ff:ff")
    net.hosts[1].cmd("arp -s 10.0.0.5 ff:ff:ff:ff:ff:ff")
    net.hosts[2].cmd("arp -s 10.0.0.5 ff:ff:ff:ff:ff:ff")

    print("Setting ARP on servers...")
    for server_id in range(3, 9):
        net.hosts[server_id].cmd("arp -s 10.0.0.1 1e:ec:71:6e:4a:ea")
        net.hosts[server_id].cmd("arp -s 10.0.0.2 a6:a0:01:5d:57:b5")
        net.hosts[server_id].cmd("arp -s 10.0.0.3 ca:07:21:25:5a:35")


def runExperiment():
    topo = MyTopo()
    controller = RemoteController('c0', ip='10.0.2.15', port=6653)
    net = Mininet(topo=topo, controller=controller)
    set_static_mac_ip(net)
    set_arp(net)
    net.start()
    start_servers(net)
    CLI(net)


if __name__ == '__main__':
    runExperiment()
