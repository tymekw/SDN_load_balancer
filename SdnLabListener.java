package pl.edu.agh.kt;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdnLabListener implements IFloodlightModule, IOFMessageListener {

    protected IFloodlightProviderService floodlightProvider;
    protected static Logger logger;

    ServerInstance SERVER_1;
    ServerInstance SERVER_2;
    ServerInstance SERVER_3;
    ServerInstance SERVER_4;
    ServerInstance SERVER_5;
    ServerInstance SERVER_6;

    static List<ServerInstance> serverList = new ArrayList<ServerInstance>();

    HostInstance HOST_1;
    HostInstance HOST_2;
    HostInstance HOST_3;


    @Override
    public String getName() {
        return SdnLabListener.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub 
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub 
        return false;
    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
                                                                   FloodlightContext cntx) {
        logger.info("************* NEW PACKET IN *************");

        OFPacketIn pin = (OFPacketIn) msg;
        OFPort inPort = pin.getInPort();

        ServerInstance selectedInstance = getBestInstance();
        // Add some lag in order to force choosing different server (Simple Python server is single threaded)
        selectedInstance.addLastResponseTime(500L);
        logger.info("Selected best instance: " + selectedInstance.getIp());


        if (inPort == OFPort.of(1)
                || inPort == OFPort.of(2)
                || inPort == OFPort.of(3)) {
            HostInstance hostInstance = getHostBySwitchPort(inPort.getPortNumber());

            Flows.forwardFirstPacket(sw, pin, selectedInstance.getSwitchPort(), selectedInstance.getIp(), selectedInstance.getMacAddress());
            Flows.simpleAdd(sw, pin, cntx, selectedInstance.getSwitchPort(), selectedInstance.getIp(), selectedInstance.getMacAddress());

            TCP tcp = extractTCP(cntx);

            Flows.simpleAddWithSourceIP(sw, pin, selectedInstance.getIp(), "10.0.2.5", hostInstance.getIp(),
                    tcp.getDestinationPort().getPort(), tcp.getSourcePort().getPort());
        }

        return Command.STOP;
    }

    private HostInstance getHostBySwitchPort(int port) {
        if (port == 1) {
            return HOST_1;
        } else if (port == 2) {
            return HOST_2;
        } else if(port == 3){
            return HOST_3;
        } else {
            logger.error("ERROR in getHostBySwitchPort, port : " + port);
            return null;
        }
    }


    private static IPv4 extractIP(FloodlightContext cntx) {
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

        // TODO Detect switch type and match to create hardware-implemented flow

        if (eth.getEtherType() == EthType.IPv4) {
            return (IPv4) eth.getPayload();
        }
        logger.info("null");
        return null;
    }

    private static ServerInstance getBestInstance() {
        long minLastResponseTime = Long.MAX_VALUE;
        ServerInstance bestServer = null;
        List<ServerInstance> bestResponseServers = new ArrayList<ServerInstance>();

        for (ServerInstance instance : serverList) {
            logger.info("Instance " + instance.getIp() + " latest response: " + instance.getLatestResponseTime());
            if (instance.getLatestResponseTime() == minLastResponseTime) {
                bestResponseServers.add(instance);

            } else if(instance.getLatestResponseTime() < minLastResponseTime) {
                minLastResponseTime = instance.getLatestResponseTime();
                bestResponseServers.clear();
                bestResponseServers.add(instance);
            }
        }

        if (bestResponseServers.size() == 1) {
            return bestResponseServers.get(0);
        }


        long minAverageResponseTime = Long.MAX_VALUE;
        ServerInstance bestAverageServer = null;
        for (ServerInstance instance : bestResponseServers) {
            logger.info("Instance " + instance.getIp() + " average response: " + instance.getAverageResponseTime());
            if (instance.getAverageResponseTime() < minAverageResponseTime) {
                minAverageResponseTime = instance.getAverageResponseTime();
                bestAverageServer = instance;
            }
        }


        return bestAverageServer;
    }

    private static TCP extractTCP(FloodlightContext cntx) {
        IPv4 ip = extractIP(cntx);
        if (ip == null) {
            logger.info("extractTCP no ip");
            return null;
        }

        if (ip.getProtocol().equals(IpProtocol.TCP)) {
            return (TCP) ip.getPayload();
        } else {
            logger.info("extractTCP no tcp");
            return null;
        }
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub 
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub 
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        logger = LoggerFactory.getLogger(SdnLabListener.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        logger.info("******************* START **************************");
        SERVER_1 = new ServerInstance("10.0.2.4","a2:74:18:73:c1:ef", 8000,  OFPort.of(4));
        SERVER_2 = new ServerInstance("10.0.2.5","72:ec:00:c1:f2:ed", 8000,  OFPort.of(5));
        SERVER_3 = new ServerInstance("10.0.2.6","32:28:de:47:35:52", 8000,  OFPort.of(6));
        SERVER_4 = new ServerInstance("10.0.2.7","2a:27:a3:72:7d:eb", 8000,  OFPort.of(7));
        SERVER_5 = new ServerInstance("10.0.2.8","da:23:46:f6:00:bb", 8000,  OFPort.of(8));
        SERVER_6 = new ServerInstance("10.0.2.9","2e:78:50:52:54:24", 8000,  OFPort.of(9));

        serverList.addAll(Arrays.asList(SERVER_1,SERVER_2,SERVER_3,SERVER_4,SERVER_6));

        HOST_1 = new HostInstance("10.0.2.1", "1e:ec:71:6e:4a:ea",  OFPort.of(1));
        HOST_2 = new HostInstance("10.0.2.2", "a6:a0:01:5d:57:b5",  OFPort.of(2));
        HOST_3 = new HostInstance("10.0.2.3", "ca:07:21:25:5a:35",  OFPort.of(3));

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(6);

        executor.scheduleAtFixedRate(new ResponseTimeChecker(SERVER_1), 10, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(new ResponseTimeChecker(SERVER_2), 10, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(new ResponseTimeChecker(SERVER_3), 10, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(new ResponseTimeChecker(SERVER_4), 10, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(new ResponseTimeChecker(SERVER_5), 10, 2, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(new ResponseTimeChecker(SERVER_6), 10, 2, TimeUnit.SECONDS);

    }

} 
