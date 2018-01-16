package eu.neonsh.nshmanager.impl.Entities;

import java.util.concurrent.atomic.AtomicInteger;

public class node {
	public String name;
	public String ip;
	public String inputInterfaceName;
	public String outputInterfaceName;
	public String restUri;
	public String type;
	public boolean hasOneInterface;
	public static AtomicInteger nodesCounter;
	public node() {
		if(node.nodesCounter==null){
			node.nodesCounter=new AtomicInteger();
		}
		
	}
}
