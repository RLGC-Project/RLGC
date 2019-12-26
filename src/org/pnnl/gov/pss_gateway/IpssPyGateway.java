package org.pnnl.gov.pss_gateway;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import javax.swing.plaf.basic.BasicSliderUI.ActionScroller;

import org.apache.commons.math3.analysis.function.Atan;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.ode.FirstOrderConverter;
import org.ieee.odm.adapter.IODMAdapter.NetType;
import org.ieee.odm.adapter.psse.PSSEAdapter;
import org.ieee.odm.adapter.psse.PSSEAdapter.PsseVersion;
import org.ieee.odm.model.dstab.DStabModelParser;
import org.interpss.IpssCorePlugin;
import org.interpss.mapper.odm.ODMDStabParserMapper;
import org.interpss.pssl.plugin.cmd.json.BaseJSONBean;
import org.interpss.pssl.plugin.cmd.json.DstabRunConfigBean;
import org.pnnl.gov.json.ReinforcementLearningConfigBean;
import org.pnnl.gov.rl.action.Action;
import org.pnnl.gov.rl.action.ActionProcessor;
import org.pnnl.gov.rl.action.GenBrakeActionProcessor;
import org.pnnl.gov.rl.action.LoadChangeActionProcessor;

import com.hazelcast.internal.serialization.impl.ConstantSerializers.TheByteArraySerializer;
import com.interpss.CoreCommonFactory;
import com.interpss.DStabObjectFactory;
import com.interpss.SimuObjectFactory;
import com.interpss.common.exp.InterpssException;
import com.interpss.common.msg.IPSSMsgHub;
import com.interpss.common.util.IpssLogger;
import com.interpss.core.aclf.AclfGen;
import com.interpss.core.acsc.fault.SimpleFaultCode;
import com.interpss.core.algo.LoadflowAlgorithm;
import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabGen;
import com.interpss.dstab.DStabilityNetwork;
import com.interpss.dstab.algo.DynamicSimuAlgorithm;
import com.interpss.dstab.algo.DynamicSimuMethod;
import com.interpss.dstab.cache.StateMonitor;
import com.interpss.simu.SimuContext;
import com.interpss.simu.SimuCtxType;

import py4j.GatewayServer;

public class IpssPyGateway {
	
	// power system dynamic simulation related
	DynamicSimuAlgorithm dstabAlgo = null;
	
	DStabilityNetwork dsNet = null;
	StateMonitor sm = null;
	boolean isSimulationDone = false;
	
	// action
	Hashtable<String,Action> actionHashtable = null;
	String[] actionBusIds = null;
	boolean isActionApplied = false;
	boolean isPreFaultActionApplied = false;
	ActionProcessor actionProc = null;
	double[] agentActionValuesAry = null;
	double[] actualactionValuesAry = null;
	
	
	// observation
	Hashtable<Integer,double[]> observationHistoryRecord = null;
	
	LinkedHashMap<String,Double> obsrv_freq = null;
	LinkedHashMap<String,Double> obsrv_voltMag = null;
	LinkedHashMap<String,Double> obsrv_voltAng = null;
	LinkedHashMap<String,Double> obsrv_genSpd = null;
	LinkedHashMap<String,Double> obsrv_genAng = null;
	LinkedHashMap<String,Double> obsrv_genP = null;
	LinkedHashMap<String,Double> obsrv_loadP = null;
	LinkedHashMap<String,Double> obsrv_loadQ = null;
	
	List<String> obsrv_state_names = null;
	
	double[] observationAry = null;

	
	//rewards
	double stepReward = 0.0;
	double totalRewards = 0.0;
	double discountRate = 0.0;
	
	String[] caseInputFiles;
	String dynSimConfigJsonFile, rlConfigJsonFile;
	
	
	DstabRunConfigBean dstabBean = null;
	ReinforcementLearningConfigBean rlConfigBean = null;
	
	
	
	String environmentName = "";
	
	// randomized parameters for each episode
	int caseIdx = 0;
	int faultBusIdx = 0;
	String faultBusId = null;
	Complex faultZ = null;
	double faultStartTime = 0.0;
	double faultDuration = 0.05; // 3 cycles by default
	
	int internalSimStepNum = 0;
	int internalObsrvRecordNum = 0;
	
	double envStepTime = 0.0;
	
	boolean applyFaultDuringInitialization = true;
	
	
	/**
	 * Return the dimensions of the observation space and the action space {history_Observation_length,observation_space_size, action_space_dim};
	 * @param caseFiles
	 *  @param configJsonFile
	 * @return int [] 
	 * @throws IOException 
	 */
	public int[] initStudyCase(String[] caseFiles,  String dynSimConfigFile, String rlConfigFile) throws IOException {
		
		caseInputFiles = new String[3];
		dynSimConfigJsonFile=dynSimConfigFile;
		rlConfigJsonFile = rlConfigFile;
		
		dstabBean  = BaseJSONBean.toBean(dynSimConfigFile, DstabRunConfigBean.class);
		
		if(caseFiles!=null)
			caseInputFiles =caseFiles;
		else if (dstabBean!=null) {
			boolean hasSeqFile = false;
			caseInputFiles[0] = dstabBean.acscConfigBean.runAclfConfig.aclfCaseFileName;
			
			if(dstabBean.acscConfigBean.seqFileName.length()>0) {
				caseInputFiles[1] = dstabBean.acscConfigBean.seqFileName;
				hasSeqFile =true;
			}
			
			if(dstabBean.dynamicFileName.length()>0) {
				
				if(hasSeqFile) {
					caseInputFiles[2] = dstabBean.dynamicFileName;
				}
				else {
					caseInputFiles[1] = dstabBean.dynamicFileName;
				    caseInputFiles = Arrays.copyOfRange(caseInputFiles, 0,2);
				}
			}
			
		}
		
		rlConfigBean = BaseJSONBean.toBean( rlConfigFile,ReinforcementLearningConfigBean.class);
		
		// initialize the variables for storing the history observation records
		observationHistoryRecord = new Hashtable<>();
		
		
		//TODO if caseFiles is not defined, search in the <study case folder> (defined in the rlConfigJsonFile) and find the the first eligible case file.
		
		boolean initFlag = loadStudyCase(caseInputFiles);
		
		if(initFlag) {
			
			
			//read the dynSimConfigFile
			
			boolean lf_flag = true;
			LoadflowAlgorithm aclfAlgo = dstabAlgo.getAclfAlgorithm();
			
			//disable volt/var control
			aclfAlgo.getLfAdjAlgo().setApplyAdjustAlgo(false);
			
			try {
				lf_flag = aclfAlgo.loadflow();
			} catch (InterpssException e) {
				e.printStackTrace();
			}
			
			if(!lf_flag) {
				throw new Error("Power flow is not converged during initialization");
			}
			
			
			
			dstabAlgo.setSimuMethod(DynamicSimuMethod.MODIFIED_EULER);
			dstabAlgo.setSimuStepSec(dstabBean.simuTimeStepSec);
			dstabAlgo.setTotalSimuTimeSec(dstabBean.totalSimuTimeSec);
			
			if(!dstabBean.referenceGeneratorId.isEmpty())
				dstabAlgo.setRefMachine(this.dsNet.getMachine(dstabBean.referenceGeneratorId));
			
			// apply fault
			
			if(this.applyFaultDuringInitialization) {
				this.faultBusId = dstabBean.acscConfigBean.faultBusId;
				faultStartTime = dstabBean.eventStartTimeSec;
				faultDuration = dstabBean.eventDurationSec;
				
				if (this.faultBusId!= null && faultStartTime> 0 && faultDuration > 0)
		    	    dsNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent(this.faultBusId,this.dsNet,SimpleFaultCode.GROUND_3P, new Complex(0.0), null,faultStartTime,faultDuration),"3phaseFault@step-"+internalSimStepNum+"@"+faultBusId);
				else
					throw new Error("The fault settings are not correct! faultBusId, faultStartTime, faultDuration = "+ this.faultBusId+" ,"+faultStartTime+" ,"+faultDuration);
				
			}
			// set the monitoring variables
			
			sm = new StateMonitor();
			
			sm.addBusStdMonitor(dstabBean.monitoringBusAry);
			sm.addGeneratorStdMonitor(dstabBean.monitoringGenAry);
			
			
			// set the output handler
			dstabAlgo.setSimuOutputHandler(sm);
			
			dstabAlgo.setOutPutPerSteps(dstabBean.outputPerNSteps);
			
			
			//initialize dyn simulation
			
			boolean dsInitFlag = dstabAlgo.initialization();
			
		
			
			// need to run the system without fault for a period of external environment observation time step
			// in order to get the observation states
			
			envStepTime = rlConfigBean.envStepTimeInSec;
			
			//TODO for now consider only one action type during one environment action-observation step
			//nextStepDynSim(envStepTime,null,rlConfigBean.actionTypes[0]);
			
			
		}
		else{
			throw new Error("Program terminated because the study case is not properly imported!");
		}
		
		// need to initialize the action space first, as saveInternalObservations() needs the actionProcessor
		int[] action_space_dim = initActionSpace();
		
		// save the internal observations to this.observationHistoryRecord
		
		this.internalObsrvRecordNum = 0;
		saveInternalObservations();
		
		
	
		//read the rlConfigFile to determine the dimension of the observation and action spaces
		int[] observation_space_dim = initObsverationSpace();
		
		System.out.println("Observed states:\n"+Arrays.toString(getEnvObversationNames()));
		System.out.println("Initial values of the observed states:\n"+Arrays.toString(observationAry));
		
		
		// prepare the return array
		int[] obs_action_dim_ary= {observation_space_dim[0],observation_space_dim[1],action_space_dim[0],action_space_dim[1]};
		
		return obs_action_dim_ary;
		
	}
	
	//TODO hashtable to store the past N step internal "states" for output as an environment state
    public double[][] getEnvObversations() {
    	
    	 
    	int historyObservSize = this.rlConfigBean.historyObservationSize;
    	int record_size = observationHistoryRecord.get(0).length;
    	
    	double[][] multiStepObservations = new double[historyObservSize][record_size];
    	
    	if (this.internalObsrvRecordNum == 0){
           for(int i = 0; i< historyObservSize; i++) {
    			
    		    multiStepObservations[i] = observationHistoryRecord.get(0);
    		}
    	}
    	else if(this.internalObsrvRecordNum < historyObservSize) {
    		int idx = 0; 
    		for(int i = this.internalObsrvRecordNum-1; i>=0; i--) {
    			idx = historyObservSize-this.internalObsrvRecordNum + i;
    		    multiStepObservations[idx] = observationHistoryRecord.get(i);
    		}
    		for(int i = 0; i< idx; i++) {
    			
    		    multiStepObservations[i] = observationHistoryRecord.get(0);
    		}
    		
    	}
    	else {
    		for(int i = 0; i<historyObservSize; i++) {
    			
    			int rec_idx = this.internalObsrvRecordNum - historyObservSize +i;
    			multiStepObservations[i] = observationHistoryRecord.get(rec_idx);
    		}
    	}
    	
        return multiStepObservations;
    }
    
    
    public String[] getEnvObversationNames() {
    	
    	return obsrv_state_names.toArray(new String[0]);
    	
    }
    
    
	//TODO hashtable to store the past N step internal "states" for output as an environment state
    
    
	
	public double getReward() {
		//TODO could move to configuration
		this.stepReward = 0;
		double u = isActionApplied?1:0;
		
		//TODO the rewards are defined  on  a case by case basis
		if(this.rlConfigBean.environmentName.contains("kundur-2area")) {
			// area-1: "Bus1-mach1","Bus2-mach1",
			// area-2: "Bus3-mach1","Bus4-mach1"
			double equiv_angle_area_1 = (this.obsrv_genAng.get("genAngle_Bus1-mach1")+this.obsrv_genAng.get("genAngle_Bus2-mach1"))*0.5;
			double equiv_angle_area_2 = (this.obsrv_genAng.get("genAngle_Bus3-mach1")+this.obsrv_genAng.get("genAngle_Bus4-mach1"))*0.5;
			
			double equiv_spd_area_1 = (this.obsrv_genSpd.get("genSpeed_Bus1-mach1")+this.obsrv_genSpd.get("genSpeed_Bus2-mach1"))*0.5;
			double equiv_spd_area_2 = (this.obsrv_genSpd.get("genSpeed_Bus3-mach1")+this.obsrv_genSpd.get("genSpeed_Bus4-mach1"))*0.5;
			
			double delta_equiv_angle = equiv_angle_area_1-equiv_angle_area_2;
			
			double delta_equiv_spd = equiv_spd_area_1-equiv_spd_area_2;
			
			if(delta_equiv_angle > Math.PI || delta_equiv_angle < -Math.PI) {
				this.stepReward = rlConfigBean.unstableReward;
				isSimulationDone = true;
			}
			else {
				this.stepReward = -Math.abs(delta_equiv_spd) - rlConfigBean.actionPenalty*u;
			}
		}
		else if(this.rlConfigBean.environmentName.contains("IEEE39_FIDVR_LoadShedding")){
			double sumOfVoltageDeviation = 0.0;
			double sumOfLoadShedPU =0.0;
			double maxRecoveryTime = this.rlConfigBean.maxVoltRecoveryTime;
			
			int sumOfInvalidActions = 0;
			
			boolean isMiniRecoveryMet = true;
			
			
			if(this.dstabAlgo.getSimuTime()> (this.faultStartTime+this.faultDuration)){
				
				
				// process the observation part
				for(Entry<String, Double> e: this.obsrv_voltMag.entrySet()){
					String busId = e.getKey();
					double voltMag = e.getValue();
					
					// first level, 0.7 pu, 0.33 s
					if(this.dstabAlgo.getSimuTime() < (this.faultStartTime + this.faultDuration + 0.33)){
						if(voltMag<0.70)
						     sumOfVoltageDeviation += voltMag-0.70;
					}
					// first level, 0.8 pu, 0.5 s
					else if(this.dstabAlgo.getSimuTime() < (this.faultStartTime + this.faultDuration + 0.5)){
						if(voltMag<0.80)
						   sumOfVoltageDeviation += voltMag-0.80;
					}
					// first level, 0.9 pu, 1.5 s
					else if(this.dstabAlgo.getSimuTime()< (this.faultStartTime + this.faultDuration + 1.5)){
						if(voltMag<0.90)
						    sumOfVoltageDeviation += voltMag-0.90;
					}
					else if(this.dstabAlgo.getSimuTime()> (this.faultStartTime + this.faultDuration + 1.5)){
						if(voltMag<0.950)
						   sumOfVoltageDeviation += voltMag-0.95;
					}
					
					
					if(this.dstabAlgo.getSimuTime() > (this.faultStartTime+this.faultDuration +maxRecoveryTime)){
					        //if any violation occurs, set the <isMiniRecoveryMet> to false
							if(voltMag < this.rlConfigBean.minVoltRecoveryLevel) isMiniRecoveryMet = false;
						
					}
					
						
				}
			}
			else{
				//isMiniRecoveryMet = true;
				sumOfVoltageDeviation = 0.0;
			
			}
			
			// process the action part
			int i = 0;
			for(String loadBusId:this.actionBusIds){
				double initTotalLoadPU = this.dsNet.getBus(loadBusId).getInitLoad().getReal();
				
				// this is the actual fraction of load shedding after the actionProcessor processes it, not the action input from the agent.
				double changeFraction = this.actualactionValuesAry[i];
				
				//check invalid actions
				if (this.agentActionValuesAry!=null) {
					if(Math.abs(changeFraction) <1.0E-8 && Math.abs(this.agentActionValuesAry[i]) > 1.0E-4){
						sumOfInvalidActions++;
					}
				}
				
				i++;
				
				if(changeFraction<0.0){
					sumOfLoadShedPU += initTotalLoadPU*Math.abs(changeFraction); // for load shedding, the changeFraction is negative
					
					//IpssLogger.getLogger().info("Bus, initLoad, shedLoadFraction = "+loadBusId+", "+initTotalLoadPU+", "+changeFraction);
					// System.out.println("Bus, initLoad, shedLoadFraction = "+loadBusId+", "+initTotalLoadPU+", "+changeFraction);
				}
				
			}
			
			
				
				
			if(isMiniRecoveryMet){
				    this.stepReward = this.rlConfigBean.observationWeight*sumOfVoltageDeviation 
				    		             - this.rlConfigBean.actionPenalty*sumOfLoadShedPU 
				    		             - this.rlConfigBean.invalidActionPenalty*sumOfInvalidActions;
				    
				    //replaced wit the logic below for prefault actions
//				    if((this.dstabAlgo.getSimuTime()<=this.faultStartTime) && (sumOfLoadShedPU > 0.0)){
//				    	this.stepReward = this.rlConfigBean.observationWeight*sumOfVoltageDeviation 
//		    		             - this.rlConfigBean.preFaultActionPenalty*sumOfLoadShedPU 
//		    		             - this.rlConfigBean.invalidActionPenalty*sumOfInvalidActions;
//					}
				    
				    if((this.dstabAlgo.getSimuTime()<=this.faultStartTime) && this.isPreFaultActionApplied){
				    	
				    	this.stepReward = this.rlConfigBean.observationWeight*sumOfVoltageDeviation 
		    		             - this.rlConfigBean.preFaultActionPenalty
		    		             - this.rlConfigBean.invalidActionPenalty*sumOfInvalidActions;
					}
			}
			else{
					IpssLogger.getLogger().severe("Minimum voltage recovery was not met at time = "+this.dstabAlgo.getSimuTime()+", simulation is stopped by early termination.");
					this.stepReward = this.rlConfigBean.unstableReward;
					this.isSimulationDone =true;
			}
		}// end of environment name check!
		else{
			throw new Error("The reward function for the environment has not been implemented yet: "+ this.rlConfigBean.environmentName);
		}
		
		this.totalRewards += this.stepReward;
		
	        return this.stepReward;
	}
	

	
	/**
	 * The simulation is done when one of the criterion is met: 1) reach the total simulation time; 2) goes unstable
	 * @return
	 */
	public boolean isSimulationDone() {
		
		if(isSimulationDone)
			   return true;
		
		if(this.rlConfigBean.environmentName.contains("kundur-2area")) {
			// area-1: "Bus1-mach1","Bus2-mach1",
			// area-2: "Bus3-mach1","Bus4-mach1"
			double equiv_angle_area_1 = (this.obsrv_genAng.get("genAngle_Bus1-mach1")+this.obsrv_genAng.get("genAngle_Bus2-mach1"))*0.5;
			double equiv_angle_area_2 = (this.obsrv_genAng.get("genAngle_Bus3-mach1")+this.obsrv_genAng.get("genAngle_Bus4-mach1"))*0.5;
			
			double equiv_spd_area_1 = (this.obsrv_genSpd.get("genSpeed_Bus1-mach1")+this.obsrv_genSpd.get("genSpeed_Bus2-mach1"))*0.5;
			double equiv_spd_area_2 = (this.obsrv_genSpd.get("genSpeed_Bus3-mach1")+this.obsrv_genSpd.get("genSpeed_Bus4-mach1"))*0.5;
			
			double delta_equiv_angle = equiv_angle_area_1-equiv_angle_area_2;
			
			double delta_equiv_spd = equiv_spd_area_1-equiv_spd_area_2;
			
			if(delta_equiv_angle > Math.PI || delta_equiv_angle < -Math.PI) {
				
				return isSimulationDone = true;
			}
		}
		else if(this.rlConfigBean.environmentName.contains("IEEE39_FIDVR_LoadShedding")){
		
			double maxRecoveryTime = this.rlConfigBean.maxVoltRecoveryTime;
			double minRecoveryVoltPU = 0.95;
			
			if(maxRecoveryTime<3.0)  maxRecoveryTime = 3.0;
			
			if(this.dstabAlgo.getSimuTime()> (this.faultStartTime+this.faultDuration)){
				
				
				// process the observation part
				for(Entry<String, Double> e: this.obsrv_voltMag.entrySet()){
					String busId = e.getKey();
					double voltMag = e.getValue();
					
					if(this.dstabAlgo.getSimuTime() > (this.faultStartTime+this.faultDuration +maxRecoveryTime )){
					        //if any violation occurs, set the <isMiniRecoveryMet> to false
							if(voltMag < minRecoveryVoltPU){ 
								return isSimulationDone = true;
							}
						
					}
						
				}
			}
		}
			
		isSimulationDone = dstabAlgo.getSimuTime()>= dstabAlgo.getTotalSimuTimeSec();
		
		return isSimulationDone;
	}
	
	
	/**
	 * Run the dynamic simulation for a period of the RL learning environment time step, which
	 * is usually equal to multiple internal dynamic simulation time steps.
	 * 
	 * <stepTimeInSec> need to be times of internal simulation time step.
	 * 
	 * For now consider only one action type during one environment action-observation step
	 * @return
	 */
	public boolean nextStepDynSim(double stepTimeInSec, double[] actionValueAry, String actionType){
		boolean simFlag = true;
		
		//reset the variable
		this.isActionApplied = false;
		this.isPreFaultActionApplied = false;
		
		// NOTE: internally it may run multiple steps for one environment action step.
		int internalSteps = (int) Math.round(stepTimeInSec/dstabAlgo.getSimuStepSec());
		
		
		
		this.agentActionValuesAry = Arrays.copyOf(actionValueAry, actionValueAry.length);
		
		if(this.agentActionValuesAry!=null) {
			if(this.dstabAlgo.getSimuTime() < this.faultStartTime){
				for(int i = 0; i <this.agentActionValuesAry.length;i++){
					if( Math.abs(this.agentActionValuesAry[i]) > 0.0){ // non-zero values will be detected
						this.isPreFaultActionApplied = true;
						
						this.agentActionValuesAry[i] = 0.0; // force it to zero; invalid actions will not be applied
						
						System.out.println("Pre-fault(event) action: index, value = "+i+", "+actionValueAry[i]);
					}
				}
			}
		// 
		System.out.println ("Apply actions at time ="+this.dstabAlgo.getSimuTime());
		applyAction(this.agentActionValuesAry, actionType, stepTimeInSec);
		}
		
		if (this.isPreFaultActionApplied)
			IpssLogger.getLogger().warning("To help training, any non-zero action being applied prior to the fault(event) time is regarded as invalid, thus will be forced to zero and not applied to simulator!\n"); 
		
		for(int i = 0; i<internalSteps; i++) {
			if(dstabAlgo.getSimuTime()<dstabAlgo.getTotalSimuTimeSec()) {
				
				simFlag = dstabAlgo.solveDEqnStep(true);
				
				// record observations at the predefined steps
				if(this.internalSimStepNum % this.dstabBean.outputPerNSteps == 0){
					
				   // save the internal observations, the time step will be the same as the envStepTimeInSec.
				    saveInternalObservations();
				   
				    //update the total observation record number
				    this.internalObsrvRecordNum++;
				}
				
				this.internalSimStepNum++;
				
			}
			else
				simFlag = false;
		}
		

		
		if(simFlag) {
			
		}
		
		
		
		return simFlag;
		
		
	
	}
	
    public int[] reset(int caseIdx,int faultBusIdx, double faultStartTime, double faultDuration) {
    	int [] initDimAry = null;
    	this.stepReward = 0.0;
    	this.totalRewards = 0.0;
    	this.isSimulationDone = false;
    	this.internalSimStepNum = 0;
    	this.internalObsrvRecordNum = 0;
    	
    	this.applyFaultDuringInitialization = false;
    	this.observationHistoryRecord = new Hashtable<>();
    	this.faultStartTime = faultStartTime;
    	this.faultDuration = faultDuration;
    	
    	this.isActionApplied = false;
    	this.agentActionValuesAry = null;
    	
    	
    	//TODO which parameter to apply the randomization? 
    	// fault type, fault location, scenario (case index)
    	
    	// case index is not used for now
    	

    	
    	try {
    		initDimAry = initStudyCase(caseInputFiles,  dynSimConfigJsonFile, rlConfigJsonFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	
    	if(faultBusIdx>= 0 && faultBusIdx<this.dsNet.getNoBus())
    	    this.faultBusId = this.dsNet.getBusList().get(faultBusIdx).getId();
    	
    	if (this.faultBusId!= null && faultStartTime> 0 && faultDuration > 0){
    	    dsNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent(this.faultBusId,this.dsNet,SimpleFaultCode.GROUND_3P, new Complex(0.0), null,faultStartTime,faultDuration),"3phaseFault@"+faultBusId);
    	}
    	else{
    		this.faultStartTime = 0.0;
        	this.faultDuration = 0.0;
    	}
    	
    	return initDimAry;
    	
	}
	
	private boolean loadStudyCase(String[] caseFiles) {
		IpssCorePlugin.init();
		IPSSMsgHub msg = CoreCommonFactory.getIpssMsgHub();
		PSSEAdapter adapter = new PSSEAdapter(PsseVersion.PSSE_30);
		
		System.out.println("case files:"+Arrays.toString(caseFiles));
		adapter.parseInputFile(NetType.DStabNet, caseFiles);
		DStabModelParser parser =(DStabModelParser) adapter.getModel();
		
		
		SimuContext simuCtx = SimuObjectFactory.createSimuNetwork(SimuCtxType.DSTABILITY_NET);
		if (!new ODMDStabParserMapper(msg)
					.map2Model(parser, simuCtx)) {
			System.out.println("Error: ODM model to InterPSS SimuCtx mapping error, please contact support@interpss.com");
			return false;
		}
		
		
	    dsNet =(DStabilityNetwork) simuCtx.getDStabilityNet();
	    //System.out.println(dsNet.net2String());
	    
		dstabAlgo = simuCtx.getDynSimuAlgorithm();
		
		if(dstabAlgo !=null)
			return true;
		else
			return false;
	}
	
	/**
	 * integer array {historyObservSize,obsrv_size};
	 * @return int [] 
	 */
	private int[] initObsverationSpace() {
		
		int obsrv_size = 0;
		
		int historyObservSize = this.rlConfigBean.historyObservationSize;
		
		
		if(historyObservSize>1) {
			double[] record = this.observationHistoryRecord.get(0);
			if(record !=null) {
				for(int i = 1; i<historyObservSize; i++) {
					this.observationHistoryRecord.put(i, record);
				}
			}
			else{
				throw new Error("The first record in this.observationHistoryRecord is null!");
			}
			
		}
		
		double[][] all_observ_states = getEnvObversations();
		
		
		obsrv_size = all_observ_states[0].length;
		
		int [] obsrv_space_dim = new int[] {historyObservSize,obsrv_size};
		
		return obsrv_space_dim;
		
		
	}
	
	private int[] initActionSpace() {
        int action_location_num = 0;  // how many locations the action will be applied to
        int action_level_num = 1;     // for each location, how many action levels will be considered. It could be [0,1] or [0, -0.1, -0.2,...]
		
		String[] obsrvStateTypes = rlConfigBean.observationStateTypes;
		
		String scopeType = rlConfigBean.actionScopeType;
		
		String[] scopeAry = rlConfigBean.actionScopeAry;
		
		String[] actionTypes = rlConfigBean.actionTypes;
		
		action_level_num = rlConfigBean.actionLevels.length;
		
		if(action_level_num<=1){
			throw new Error("Error in action levels definition, it should have at least two numbers. "+rlConfigBean.actionLevels);
		}
		
		
		if(actionTypes.length>0){
		  if(actionTypes[0].equalsIgnoreCase("BrakeAction")){
			  this.actionProc = new GenBrakeActionProcessor(this.dsNet, this.dstabAlgo, this.envStepTime);
		  }
		  else if(actionTypes[0].equalsIgnoreCase("LoadShed")){
			  this.actionProc = new LoadChangeActionProcessor(this.dsNet);
		  }
		  else{
			  throw new Error("The input action type is not supportted yet: "+actionTypes[0]);
		  }
		}
		else{
			throw new Error("The ctionTypes in the RL Json configuration file is empty");
		}
		
		List<String> actionBusIdList = new ArrayList<>();
		
		for(DStabBus bus:dsNet.getBusList()) {
			if(bus.isActive()) {
				if(isBusWithinScope(bus,scopeType,scopeAry)) {
					
					for(String actionType: actionTypes) {
						if(actionType.equalsIgnoreCase("LoadShed")) {
							//check if it is a load
							if(bus.isLoad()) {
								action_location_num++;
								actionBusIdList.add(bus.getId());
							}
						}
						else if(actionType.equalsIgnoreCase("GenShed")) {
							//TODO
						}
						else if(actionType.equalsIgnoreCase("BrakeAction")) {
							action_location_num++;
							actionBusIdList.add(bus.getId());
						}
					}
				}
			}
		}
		
		// convert the list to array
		actionBusIds = actionBusIdList.toArray(new String[actionBusIdList.size()]);
		
		// set the action scope and action levels
		this.actionProc.setActionScopeByBus(actionBusIds);
		this.actionProc.setActionLevels(rlConfigBean.actionLevels);
		
		//action_space_dim is defined by action_location_num and action_level_num
		int [] action_space_dim = {action_location_num, action_level_num};
		return action_space_dim;
		
	}
	
	private boolean isBusWithinScope(DStabBus bus, String scopeType,String[] scopeAry) {
		boolean inScope = true;
		
		if (scopeType.equalsIgnoreCase("Bus")) {
			if(Arrays.stream(scopeAry).anyMatch(bus.getId()::equals))
				inScope = true;
			else
				inScope = false;
			
		}
		else if(scopeType.equalsIgnoreCase("Area")) {
			if(Arrays.stream(scopeAry).anyMatch(bus.getArea().getId()::equals))
				inScope = true;
			else
				inScope = false;
			
			
		}
		else if(scopeType.equalsIgnoreCase("Zone")) {
			if(Arrays.stream(scopeAry).anyMatch(bus.getZone().getId()::equals))
				inScope = true;
			else
				inScope = false;
			
		}
		
		else { // the whole system
			
			inScope = true;
		}
		
		return inScope;
			
	}
	
	
	

	

	
	private boolean applyAction(double[] actionValueAry, String actionValueType, double duration) {
		
		
		if(actionValueAry!=null) {	
			this.actualactionValuesAry = actionProc.applyAction(actionValueAry, actionValueType, duration);
			
			if(this.actualactionValuesAry!=null)
			      isActionApplied = true;
		}
		
		
		return isActionApplied ;
	}
	
	private Action mapGymActionToIpssAction(String gymAction, String actionType) {
		//TODO to be implemented
		
		return null;
	}
	

    
	/**
	 * Keep records of the observations based on the observation scope and type configurations of the input RL  configuration json file.
	 * @return
	 */
    private double[] saveInternalObservations() {
    	// TODO consider to save the past N-step states and retrieve the past states when necessary
    	
        String[] obsrvStateTypes = rlConfigBean.observationStateTypes;
		
		String scopeType = rlConfigBean.observationScopeType;
		
		String[] scopeAry = rlConfigBean.observationScopeAry;
		
		
		// The reason of using separate hashtables to store the states is to keep the same type of states in a consecutive form, as the implementation below is iterating over
		// the bus, not the state type first.
		// Then different types of states are concatted to one large vector/array 
		
		obsrv_freq = new LinkedHashMap<>();
		obsrv_voltMag = new LinkedHashMap<>();
		obsrv_voltAng = new LinkedHashMap<>();
		obsrv_genSpd = new LinkedHashMap<>();
		obsrv_genAng = new LinkedHashMap<>();
		obsrv_genP = new LinkedHashMap<>();
		obsrv_loadP = new LinkedHashMap<>();
		obsrv_loadQ = new LinkedHashMap<>();
		
		obsrv_state_names = new ArrayList<>();
		
		List<Double> all_observ_states = new ArrayList<>();
		
		
		// iterate over all buses and check whether it fits the scope (type and definitions);
	
				
		for(DStabBus bus:dsNet.getBusList()) {
			if(bus.isActive()) {
				if(isBusWithinScope(bus,scopeType,scopeAry)) {
					
					for(String stateType : obsrvStateTypes) {
						if(stateType.equalsIgnoreCase("frequency")) {
							
							obsrv_freq.put("frequency_"+bus.getId(),bus.getFreq());
						}
						if(stateType.equalsIgnoreCase("voltageMag")) {
							
							obsrv_voltMag.put("voltageMag_"+bus.getId(),bus.getVoltageMag());
						}
						if(stateType.equalsIgnoreCase("voltageAng")) {
							
							obsrv_voltAng.put("voltageAng_"+bus.getId(),bus.getVoltageAng());
						}
						
						if(stateType.equalsIgnoreCase("loadP")) {
							if(bus.isLoad() || bus.getContributeLoadList().size()>0) {
								//TODO need to update to capture dynamic total loads
							   obsrv_loadP.put("loadP_"+bus.getId(),bus.getLoadP());
							}
						}
						if(stateType.equalsIgnoreCase("loadQ")) {
							if(bus.isLoad() || bus.getContributeLoadList().size()>0) {
							  //TODO need to update to capture dynamic total loads
							   obsrv_loadQ.put("loadQ_"+bus.getId(),bus.getLoadQ());
							}
						}
						if(stateType.equalsIgnoreCase("genSpeed")) {
							if(bus.isGen() || bus.getContributeGenList().size()>0) {
							    for(AclfGen gen: bus.getContributeGenList()) {
							       if(gen.isActive()) {
							    	   DStabGen dsGen = (DStabGen) gen;
							    	   if(dsGen.getMach()!=null) {
								         
								           obsrv_genSpd.put("genSpeed_"+dsGen.getMach().getId(),dsGen.getMach().getSpeed());
								           // dsGen.getMach().getGovernor().get
							    	   }
							       }
							    }
							   
							}
						}
						if(stateType.equalsIgnoreCase("genAngle")) {
							if(bus.isGen() || bus.getContributeGenList().size()>0) {
							    for(AclfGen gen: bus.getContributeGenList()) {
							       if(gen.isActive()) {
							    	   DStabGen dsGen = (DStabGen) gen;
							    	   if(dsGen.getMach()!=null) {
								         
								           obsrv_genAng.put("genAngle_"+dsGen.getMach().getId(),dsGen.getMach().getAngle());
							    	   }
							       }
							    }
							   
							}
						}
						
					}
					
				}
				
			}
		}
		
		// values
		all_observ_states.addAll(obsrv_freq.values());
		all_observ_states.addAll(obsrv_voltMag.values());
		all_observ_states.addAll(obsrv_voltAng.values());
		all_observ_states.addAll(obsrv_loadP.values());
		all_observ_states.addAll(obsrv_loadQ.values());
		all_observ_states.addAll(obsrv_genSpd.values());
		all_observ_states.addAll(obsrv_genAng.values());
		
		// the observed state names
		obsrv_state_names.addAll(obsrv_freq.keySet());
		obsrv_state_names.addAll(obsrv_voltMag.keySet());
		obsrv_state_names.addAll(obsrv_voltAng.keySet());
		obsrv_state_names.addAll(obsrv_loadP.keySet());
		obsrv_state_names.addAll(obsrv_loadQ.keySet());
		obsrv_state_names.addAll(obsrv_genSpd.keySet());
		obsrv_state_names.addAll(obsrv_genAng.keySet());
		
		
		observationAry = Stream.of(all_observ_states.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
		
		
		//append the action status observations to <this.observationHistoryRecord>
		//TODO tentatively hard-code to add remaining fractions of to-be-tripped bus loads to observation space and the observed state
		if( rlConfigBean.includeActionStatusInObservations == true && rlConfigBean.environmentName.contains("LoadShed")){
			
			double[] actionStatus = this.actionProc.getAgentActionStatus(); // one state for observation for each location.
			observationAry = DoubleStream.concat(DoubleStream.of(observationAry),DoubleStream.of(actionStatus)).toArray();
			
			String[] actionBusAry = this.actionProc.getActionScopeByBus();
			
			for (String actionBus:actionBusAry) {
				obsrv_state_names.add("ActionStatus@"+actionBus);
			}
		    
		}
		
		
		//historical observation window size
		observationHistoryRecord.put(this.internalObsrvRecordNum, observationAry);
		
		
		
		return observationAry;
		
	}
    
    public DynamicSimuAlgorithm getDStabAlgo() {
    	return this.dstabAlgo;
    }
    
	public ReinforcementLearningConfigBean getRLConfigBean() {
		return this.rlConfigBean;
	}
	
	public StateMonitor getStateMonitor() {
		return this.sm;
	}
	
	public int getTotalBusNum() {
		return this.dsNet.getNoBus();
	}
	
	public double getActionStepTime() {
		return this.rlConfigBean.envStepTimeInSec;
	}
	
	public double getTotalRewards() {
		return this.totalRewards;
	}
	
	/**
	 * return the agent-environment action time step
	 * @return
	 */
	public double getEnvTimeStep(){
		return this.rlConfigBean.envStepTimeInSec;
	}
	
	
	public String[] getStudyCases(){
		return this.rlConfigBean.studyCases;
		
	}
	
	public String[] getFaultBusCandidates(){
		return this.rlConfigBean.faultBusCandidates;
		
	}
	
	public double[] getFaultStartTimeCandidates(){
		return this.rlConfigBean.faultStartTimeCandidates;
		
	}
	
	public double[] getFaultDurationCandidates(){
		return this.rlConfigBean.faultDurationCandidates;
		
	}
	
	public String getActionSpaceType() {
		return this.rlConfigBean.actionSpaceType;
	}
	
	public double[][] getActionValueRanges(){
		return this.rlConfigBean.actionValueRanges;
	}

	
	
	/**
	 * main method to start the service
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		IpssLogger.getLogger().setLevel(Level.INFO);
		IpssPyGateway app = new IpssPyGateway();
		// app is now the gateway.entry_point
		int port = 25332;
		
		if (args.length>0) {
			port = Integer.valueOf(args[0]);
		}
			
		GatewayServer server = new GatewayServer(app,port);

		System.out.println("InterPSS Engine for Reinforcement Learning (IPSS-RL) developed by Qiuhua Huang @ PNNL. Version 0.82, built on 12/26/2019");

		System.out.println("Starting Py4J " + app.getClass().getTypeName() + " at port ="+port);
		server.start();
	}
	
	
}
