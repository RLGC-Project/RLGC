package org.pnnl.gov.json;

import org.interpss.pssl.plugin.cmd.json.BaseJSONBean;

public class ReinforcementLearningConfigBean extends BaseJSONBean{
	
	

	

	public ReinforcementLearningConfigBean() {
		
	}
   //----------Below are added to version 3------------------//
	public double actionVoltThreshold = 0.0;
	public double actionPowerMWThreshold = 0.0;
	public double earlyTerminationThreshold = 0.0; //could be used to determine when to stop the simulation due to stability issues.
	public double stableThresholdCriterion = 0.0; //unit:  degree for angle,  pu for voltage
	public double preThresholdActionPenalty = 0.0; // If action is applied prior to meeting the <stableThresholdCriterion> defined above, the action will not be accepted but a penalty is applied.
	
   //----------Below are added to version 2------------------//
	public int version = 1; //include version info to make the configuration backward compatible 
    public String actionSpaceType = "discrete"; // discrete or continuous 
    public double[][]  actionValueRanges = {{}}; // { {min1,max1}, {min2, max2},..,{min_N, max_N}} for N continuous actions, if the dimension is less than the action space, the last entry will be copied and used to extend the array to the dimension of action space.  
	public double observationVoltThreshold = 1.0;
	
	
	//---------Below are for version 1------------------
	
	public String environmentName = ""; // implemented environment
	
	public String studyCaseFolder = "";
	
	// define observation stateTypes, such as voltage, frequency, genSpeed, loadP, loadQ
	public String[] observationStateTypes = {} ; //{"voltage","frequency","loadP","loadQ","genSpeed","genAng","genP", "genQ"}; 
	
	public String observationScopeType ="";  //{Bus, Zone, Area,System}
			
	public String[] observationScopeAry= {};
	
	public int historyObservationSize = 10;
	
	// define action space. what kind of actions to be applied, where they will be applied
	
	public String[] actionTypes = {}; //"LoadShed", "BrakeAction", "GeneratorTripping"
	
	public String actionScopeType =""; //{Bus, Zone, Area,System}
	
	public String[] actionScopeAry= {};
	
	public double envStepTimeInSec = 0;
	
	public double brakeResistorPU = 0.0;// in pu
	
	public double[] actionLevels = {0, 1}; // for defining discrete actions
	
    public double unstableReward = -1000; // default for kundur-2area case
    
    public double actionPenalty = 2.0; // for applying weighting/penalty to the action part in the reward function
    
    public double invalidActionPenalty = 20.0;
    
    public double preFaultActionPenalty = 100.0; 
    
    public double observationWeight = 1.0; // for applying weighting to the observation part in the reward function
    
    public boolean includeActionStatusInObservations = false;
    
    public double minVoltRecoveryLevel = 0.95; // in pu;
    
    public double maxVoltRecoveryTime = 15.0;// in second, start counting when the fault is cleared
    
    public String[] faultBusCandidates = {}; //"Bus4", "Bus15", "Bus21"
    public double[] faultStartTimeCandidates = {0.05};
    public double[] faultDurationCandidates = {0.0, 0.05, 0.08};
    
 
}
