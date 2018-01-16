package eu.neonsh.nshmanager.impl.Entities;

import java.util.concurrent.atomic.AtomicInteger;

public class AccessListEntry {
	public String aceName;
	public int ID;
	public Boolean isIP;
	public Boolean isIPv4;
	public Short aceIPDscp;
	public Short aceIPProtocol;
	public int aceIPSourceLowerPort=0;
	public int aceIPSourceUpperPort=0;
	public int aceIPDestLowerPort=0;
	public int aceIPDestUpperPort=0;
	public String aceIPv4SourceIP;
	public String aceIPv4DestIP;
	public String aceIPv6SourceIP;
	public String aceIPv6DestIP;
	public Long aceIPv6Label;
	public String aceEthSourceMAC;
	public String aceEthSourceMask;
	public String aceEthDestMAC;
	public String aceEthDestMask;
	public String aceAction;
	public String aceRSP;
	public String flowRepresentation;
	private static AtomicInteger AceCounter;
	public AccessListEntry() {
		flowRepresentation="";
		if(AccessListEntry.AceCounter==null)
		{
			AccessListEntry.AceCounter=new AtomicInteger(1);
			ID=1;
		}
		else
		{
			ID= AccessListEntry.AceCounter.getAndAdd(2);
		}
	}
	
}
