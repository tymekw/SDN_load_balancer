package pl.edu.agh.kt;

import java.util.List;

import org.projectfloodlight.openflow.types.OFPort;

public class ServerInstance {
	String ip;
	long lastResponseTime;
	int port;
	String macAddress;
	OFPort switchPort;
	List<Long> responseTimeList;

	
	public ServerInstance(String ip, String macAddress, int port, OFPort switchPort) {
		this.ip = ip;
		this.port = port;
		this.macAddress = macAddress;
		this.switchPort = switchPort;
		lastResponseTime = 0;
		this.responseTimeList = new LimitedQueue<Long>(10);
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public synchronized void addLastResponseTime(long lastResponseTime) {
		responseTimeList.add(lastResponseTime);
	}
	
	public synchronized long getAverageResponseTime() {
		long sum = 0;
		for (Long responseTime : responseTimeList) {
			sum = sum + responseTime;
		}
		return sum/responseTimeList.size();
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getIp() {
		return ip;
	}
	
	public String getMacAddress() {
		return macAddress;
	}
	
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public void setSwitchPort(OFPort switchPort) {
		this.switchPort = switchPort;
	}
	
	public OFPort getSwitchPort() {
		return switchPort;
	}
	

}
