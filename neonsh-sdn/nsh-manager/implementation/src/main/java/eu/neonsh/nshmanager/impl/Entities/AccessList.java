package eu.neonsh.nshmanager.impl.Entities;

import java.util.ArrayList;

public class AccessList {

	public String Name;
	public  ArrayList<AccessListEntry> aces;
	public ArrayList<String> conditions;
	public ArrayList <String> flowIDs;
	public AccessList() {
		aces=new ArrayList<AccessListEntry>();
		conditions=new ArrayList<>();
		flowIDs = new ArrayList<>();
	}
	
}