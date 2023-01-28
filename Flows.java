package pl.edu.agh.kt;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
 
public class Flows {
 
	private static final Logger logger = LoggerFactory.getLogger(Flows.class);
 
	public static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static short FLOWMOD_DEFAULT_PRIORITY = 100;
 
	protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = false;
	protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = false;
	protected static boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;
 
	public Flows() {
		logger.info("Flows() begin/end");
	}
 
	public static void forwardFirstPacket(IOFSwitch sw, OFPacketIn pi,
			OFPort outport, String outIp, String outMac) {
				OFActionSetNwDst changeIp = sw.getOFFactory().actions().buildSetNwDst()
						.setNwAddr(IPv4Address.of(outIp)).build();
				OFActionSetDlDst changeMacDst = sw.getOFFactory().actions()
						.buildSetDlDst().setDlAddr(MacAddress.of(outMac))
					.build();
		if (pi == null) {
			return;
		}
 
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi
				.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
 
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
 
		List<OFAction> actions = new ArrayList<OFAction>();
 
		actions.add(changeMacDst);
		actions.add(changeIp);
 
		actions.add(sw.getOFFactory()
		.actions()
		.buildOutput()
		.setPort(outport)
		.setMaxLen(0xffFFffFF)
		.build());
 
		pob.setActions(actions);
 
		if (sw.getBuffers() == 0) {
			pi = pi.createBuilder().setBufferId(OFBufferId.NO_BUFFER).build();
			pob.setBufferId(OFBufferId.NO_BUFFER);
			logger.info("The switch doesn't support buffering");
		} else {
			pob.setBufferId(pi.getBufferId());
		}
 
		if (pi.getBufferId() == OFBufferId.NO_BUFFER) {
			byte[] packetData = pi.getData();
			pob.setData(packetData);
 
			logger.info("NO_BUFFER");
		} else {
			logger.info("Packet_Out buffer id: {}", pob.getBufferId());
		}
 
		sw.write(pob.build());
	}
 
	public static void simpleAdd(IOFSwitch sw, OFPacketIn pin,
			FloodlightContext cntx, OFPort outPort, String outIp, String outMac) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);
 
		// actions
		OFActionSetNwDst changeIp = sw.getOFFactory().actions().buildSetNwDst()
				.setNwAddr(IPv4Address.of(outIp)).build();
		OFActionSetDlDst changeMacDst = sw.getOFFactory().actions()
				.buildSetDlDst().setDlAddr(MacAddress.of(outMac))
				.build();
 
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();
		aob.setPort(outPort);
		aob.setMaxLen(Integer.MAX_VALUE);
 
		actions.add(changeMacDst);
		actions.add(changeIp);
		actions.add(aob.build());
		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				.setBufferId(pin.getBufferId()).setOutPort(outPort)
				.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch
		try {
			sw.write(fmb.build());
			logger.info(
					"Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(),
							outPort.getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}
	}
 
	public static void simpleAddWithSourceIP(IOFSwitch sw, OFPacketIn pin,
		 String realSrcIp, String srcIp, String dstIp, int tcpSrc, int tcpDst) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		Match.Builder m = sw.getOFFactory().buildMatch();
 
		m.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		.setExact(MatchField.IPV4_SRC, IPv4Address.of(realSrcIp))
		.setExact(MatchField.IPV4_DST, IPv4Address.of(dstIp));
 
 
 
		m.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
				.setExact(MatchField.TCP_SRC, TransportPort.of(tcpSrc))
				.setExact(MatchField.TCP_DST, TransportPort.of(tcpDst));
 
		// actions
		OFActionSetNwSrc changeIp = sw.getOFFactory().actions().buildSetNwSrc()
				.setNwAddr(IPv4Address.of(srcIp)).build();
 
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();
		aob.setPort(pin.getInPort());
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(changeIp);
		actions.add(aob.build());
		fmb.setMatch(m.build()).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				.setBufferId(pin.getBufferId()).setOutPort(pin.getInPort())
				.setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch
		try {
			sw.write(fmb.build());
			logger.info(
					"Z powrotem: Flow from port x forwarded to port {}; match: {}",
					new Object[] {
							pin.getInPort().getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}
	}
 
	public static Match createMatchFromPacket(IOFSwitch sw, OFPort inPort,
			FloodlightContext cntx) {
 
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();
 
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort);
 
		if (FLOWMOD_DEFAULT_MATCH_MAC) {
			mb.setExact(MatchField.ETH_SRC, srcMac).setExact(
					MatchField.ETH_DST, dstMac);
		}
 
		if (FLOWMOD_DEFAULT_MATCH_VLAN) {
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
		}
 
		if (eth.getEtherType() == EthType.IPv4) { 
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address dstIp = ip.getDestinationAddress();
 
			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
						.setExact(MatchField.IPV4_SRC, srcIp)
						.setExact(MatchField.IPV4_DST, dstIp);
			}
 
			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				}
 
				if (ip.getProtocol().equals(IpProtocol.TCP)) {
					TCP tcp = (TCP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
							.setExact(MatchField.TCP_SRC, tcp.getSourcePort())
							.setExact(MatchField.TCP_DST,
									tcp.getDestinationPort());
				} else if (ip.getProtocol().equals(IpProtocol.UDP)) {
					UDP udp = (UDP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
							.setExact(MatchField.UDP_SRC, udp.getSourcePort())
							.setExact(MatchField.UDP_DST,
									udp.getDestinationPort());
				}
			}
		} else if (eth.getEtherType() == EthType.ARP) { 
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		}
 
		return mb.build();
	}
}
