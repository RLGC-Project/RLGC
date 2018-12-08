package org.pnnl.gov.rl.action;

import com.interpss.dstab.DStabilityNetwork;

public class AbstractActionProcessor implements ActionProcessor {
	
	protected String[] actionScopeBusAry = null;
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

}
