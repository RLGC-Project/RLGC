package org.pnnl.gov.rl.action;

import java.util.Hashtable;
import java.util.List;

import com.interpss.dstab.DStabilityNetwork;

public class AbstractActionProcessor implements ActionProcessor {
	
	protected String[] actionScopeBusAry = null;
	protected String[] actionScopeGeneratorAry = null;
	protected String[] actionScopeBranchAry = null;
	protected String[] actionScopeCutSetAry = null;
	protected Hashtable<String, List<String>> actionCutSetTable = null;
	
	private double[] actionLevels= null;
	protected DStabilityNetwork dstabNet = null;
	
	public AbstractActionProcessor(DStabilityNetwork network){
		this.dstabNet = network;
	}

	@Override
	public void setActionScopeByBus(String[] actionBusAry) {
		this.actionScopeBusAry = actionBusAry;

	}

	@Override
	public String[] getActionScopeByBus() {
		
		return this.actionScopeBusAry;
	}
	
	@Override
	public void setActionScopeByGenerator(String[] actionScopeAry) {
		this.actionScopeGeneratorAry = actionScopeAry;

	}

	@Override
	public String[] getActionScopeByGenerator() {
		
		return this.actionScopeGeneratorAry;
	}
	
	@Override
	public void setActionScopeByBranch(String[] actionScopeAry) {
		
		this.actionScopeBranchAry = actionScopeAry;
	}

	@Override
	public String[] getActionScopeByBranch() {
		
		return this.actionScopeBranchAry;
	}


	@Override
	public double[] applyAction(double[] actionValueAry, String actionValueType, double duration) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setActionLevels(double[] actionLevels) {
		this.actionLevels = actionLevels;
		
	}

	@Override
	public double[] getActionLevels() {
		
		return this.actionLevels;
	}

	@Override
	public double[] getAgentActionStatus() {
		
		throw new UnsupportedOperationException();
	}

	@Override
	public void setActionScopeByCutSet(String[] actionScopeAry) {
		this.actionScopeCutSetAry = actionScopeAry;
		
	}

	@Override
	public String[] getActionScopeByCutSet() {
		
		return this.actionScopeCutSetAry;
	}

	@Override
	public Hashtable<String, List<String>> getActionCutSetTable() {
	
		return this.actionCutSetTable;
	}

	@Override
	public void setActionCutSetTable(Hashtable<String, List<String>> cutSetTable) {
		 this.actionCutSetTable = cutSetTable;
		
	}

	
}
