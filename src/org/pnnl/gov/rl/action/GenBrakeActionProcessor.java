package org.pnnl.gov.rl.action;

import org.apache.commons.math3.complex.Complex;

import com.interpss.DStabObjectFactory;
import com.interpss.core.acsc.fault.SimpleFaultCode;
import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabilityNetwork;
import com.interpss.dstab.algo.DynamicSimuAlgorithm;

public class GenBrakeActionProcessor extends AbstractActionProcessor implements ActionProcessor {
    
	protected double brakeResistorPU = 0.0;
	protected DynamicSimuAlgorithm dstabAlgo = null;
	public GenBrakeActionProcessor(DStabilityNetwork network, DynamicSimuAlgorithm dstabSimAlgo, double brakeResistorInPU) {
		super(network);
		this.brakeResistorPU = brakeResistorInPU;
		this.dstabAlgo = dstabSimAlgo;
	}
	
	@Override
	public double[] applyAction(double[] actionValueAry, String actionValueType, double duration) {
		boolean isActionApplied = true; 
		
		if(actionValueAry!=null) {
			if(actionValueType.equalsIgnoreCase("discrete")) {
				
				int i = 0;
				for(String busId:this.actionScopeBusAry){
				    DStabBus bus = this.dstabNet.getBus(busId);
			       
			       if(bus.isActive()){
			    		int action  = (int) actionValueAry[i];
						
						if(action>0) {
							//apply fault to mimic the breaking resistor
							
							Complex faultZ = new Complex(this.brakeResistorPU,0);
							double faultStartingTime = dstabAlgo.getSimuTime();
							this.dstabNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent(busId,this.dstabNet,SimpleFaultCode.GROUND_3P, faultZ, null,faultStartingTime,duration),"Action@"+busId + "at time =" +faultStartingTime);
							isActionApplied = true;   
						}
						else{ //no action
							
						}
			       }
				}
			
				
			}
			else {
				throw new Error("Only descrete action type is supported for now!");
			}
		}
	  return actionValueAry;
		
	}

}
