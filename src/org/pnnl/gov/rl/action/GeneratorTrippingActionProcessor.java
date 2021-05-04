package org.pnnl.gov.rl.action;

import com.interpss.DStabObjectFactory;
import com.interpss.dstab.DStabilityNetwork;
import com.interpss.dstab.algo.DynamicSimuAlgorithm;
import com.interpss.dstab.mach.Machine;

public class GeneratorTrippingActionProcessor extends AbstractActionProcessor {

	
	
	protected DynamicSimuAlgorithm dstabAlgo = null;
	public GeneratorTrippingActionProcessor(DStabilityNetwork network,DynamicSimuAlgorithm dstabAlgo) {
		super(network);
		this.dstabAlgo = dstabAlgo;

	}
	
	
	@Override
	public double[] applyAction(double[] actionValueAry, String actionValueType, double duration) {
		boolean isActionApplied = true; 
		
		if(actionValueAry!=null) {
			if(actionValueType.equalsIgnoreCase("discrete")) {
				
				int i = 0;
				for(String genId:this.actionScopeGeneratorAry){
				    Machine mach = this.dstabNet.getMachine(genId);
			       
				    if(mach.isActive()){
			    		int action  = (int) actionValueAry[i];
						
						if(action>0) {
				
							double faultStartingTime = dstabAlgo.getSimuTime();
							String eventId = String.format("%s_trip_at_%f second",genId, faultStartingTime);
							this.dstabNet.addDynamicEvent(DStabObjectFactory.createGeneratorTripEvent(mach.getDStabBus().getId(), mach.getParentGen().getId(), this.dstabNet, faultStartingTime),eventId);
							isActionApplied = true;   
						}
						else{ //no action
							
						}
				     }
				     else { // if the machine is off-line already, no actual action
				    	 actionValueAry[i] = 0;
				     }
				  i++;
				}
			
				
			}
			else {
				throw new Error("Only descrete action type is supported for now!");
			}
		}
	  return actionValueAry;
		
	}

}
