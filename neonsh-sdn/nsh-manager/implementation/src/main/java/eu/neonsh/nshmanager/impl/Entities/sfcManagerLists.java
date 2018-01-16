package eu.neonsh.nshmanager.impl.Entities;

import eu.neonsh.nshmanager.impl.NSHManagerProvider;
import eu.neonsh.nshmanager.impl.SFCManagerUtils;
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;

import java.util.ArrayList;

public class sfcManagerLists {
	public ArrayList<node> nodes;
	public ArrayList<Chain> chains;
	public ArrayList<AccessList> acls;
	
	public sfcManagerLists() {
		nodes=new ArrayList<node>();
		chains=new ArrayList<Chain>();
		acls=new ArrayList<AccessList>();
	}


	public void insertACL(AccessList acl) {
		acls.add(acl);
	}
	public void deleteACLByName(String name) {
		AccessList acl = null;
		for(AccessList i : acls)
		{
			if(i.Name.equalsIgnoreCase(name)){
				acl=i;
				break;
			}
		}
		if(acl!=null){

//			for(String cond:acl.conditions){
//				RemoveFlowConditionInputBuilder builder=new RemoveFlowConditionInputBuilder();
//				NSHManagerProvider.vtnServices.vtnFlowConditionService.removeFlowCondition(builder.setName(cond).build());
//			}
//			for(String flow:acl.flowIDs){
//				NSHManagerUtils.deleteFlow(flow);
//			}
			acls.remove(acl);
		}

	}

	public void removeChainByName(String i) {
		Chain tmp=null;
		ArrayList<String> tmpListName=new ArrayList<>() ;
		for(Chain ch :chains){
			if(ch.name.equalsIgnoreCase(i)){
				tmp=ch;
			}
		}
		if(tmp!=null){
			chains.remove(tmp);
		}
		for(AccessList acl:acls){
			for(AccessListEntry ace:acl.aces){
				if(ace.aceRSP.equalsIgnoreCase(i)){
					tmpListName.add(acl.Name);
				}
			}
		}
		for(String t:tmpListName){
			deleteACLByName(t);
		}
		tmpListName.clear();
	}

    public void insertServiceFunction(node node) {
		nodes.add(node);
    }

	public void deleteServiceFunctionByName(String name) {
		node tmp = null;
		ArrayList<String> tmpChainListName=new ArrayList<>() ;
		for(node function:nodes){
			if(name.equalsIgnoreCase(function.name)){
				tmp=function;
				break;

			}
		}
		if(tmp!=null)
		{
			SfcProviderServiceFunctionAPI.deleteServiceFunctionState(new SfName(name));
			nodes.remove(tmp);
		}
		for(Chain ch:chains){
			if(ch.functions.contains(name)){
				tmpChainListName.add(ch.name);
			}
		}
		for(String t:tmpChainListName)
		{
			removeChainByName(t);
		}
		tmpChainListName.clear();
	}

    public void insertChain(Chain chain) {
		chains.add(chain);
    }

	public void updateChain(Chain chain) {
		//TODO chain update
	}


	public node getNodeFromName(String functionName) {
		for (node i : nodes) {
			if (i.name.equalsIgnoreCase(functionName)) {
				return i;
			}
		}
		return null;
	}

	public Chain getChainFromID(String chainID) {
		for (Chain i : chains) {
			if (chainID != null && chainID.equalsIgnoreCase(i.name)) {
				return i;
			}
		}
		return null;
	}


}
