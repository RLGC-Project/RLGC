package org.pnnl.gov.rl.action;

import com.interpss.DStabObjectFactory;
import com.interpss.core.net.Branch;
import com.interpss.dstab.DStabBranch;
import com.interpss.dstab.DStabilityNetwork;
import com.interpss.dstab.algo.DynamicSimuAlgorithm;

public class LineSwitchActionProcessor extends AbstractActionProcessor{

protected DynamicSimuAlgorithm dstabAlgo = null;
public LineSwitchActionProcessor(DStabilityNetwork network,DynamicSimuAlgorithm dstabAlgo) {
	super(network);
	this.dstabAlgo = dstabAlgo;

}

/**
 * TODO:   implement line switch on or reclosure action [7/17/2021]
 */
@Override
public double[] applyAction(double[] actionValueAry, String actionValueType, double duration) {
	boolean isActionApplied = false; 
	
	if(actionValueAry!=null) {
		if(actionValueType.equalsIgnoreCase("discrete")) {
			
			int i = 0;
			for(String cutSetId:this.actionScopeCutSetAry) {
				int action  = (int) actionValueAry[i];
				
				if(action>0) {
			        
					for(String branchId: this.actionCutSetTable.get(cutSetId)) {
					    DStabBranch bra = this.dstabNet.getBranch(branchId);
	
				        if(bra!=null && bra.isActive()&&bra.getZ().abs()<999.0){
							double faultStartingTime = dstabAlgo.getSimuTime();
							String eventId = String.format("%s_trip_at_%f second",branchId, faultStartingTime);
							this.dstabNet.addDynamicEvent(DStabObjectFactory.createBranchSwitchEvent(branchId,faultStartingTime, this.dstabNet),eventId);
							isActionApplied = true;   
						}
						
				       else { // if the branch is off-line already, no actual action
				    	 actionValueAry[i] = 0;
				        }
				  
			        }
	
              } 
				i++;
	     }
		}
	}
	if (!isActionApplied)
		return null;
	return actionValueAry;
}


}
