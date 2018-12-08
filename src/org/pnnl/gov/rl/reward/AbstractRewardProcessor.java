package org.pnnl.gov.rl.reward;

public class AbstractRewardProcessor {
	
	
	protected double stepReward = 0.0;
	protected double totalReward = 0.0;
	
	
	public double getStepReward(){
		throw new UnsupportedOperationException();
	}
	
	public double getTotalReward(){
		return this.totalReward;
	}
	
}
