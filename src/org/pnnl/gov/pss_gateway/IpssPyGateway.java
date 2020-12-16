package org.pnnl.gov.pss_gateway;


import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.basic.BasicSliderUI.ActionScroller;

import org.apache.commons.math3.analysis.function.Atan;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.ode.FirstOrderConverter;
import org.ieee.odm.adapter.IODMAdapter.NetType;
import org.ieee.odm.adapter.psse.PSSEAdapter;
import org.ieee.odm.adapter.psse.PSSEAdapter.PsseVersion;
import org.ieee.odm.common.ODMLogger;
import org.ieee.odm.model.dstab.DStabModelParser;
import org.interpss.IpssCorePlugin;
import org.interpss.fadapter.IpssFileAdapter.FileFormat;
import org.interpss.mapper.odm.ODMDStabParserMapper;
import org.interpss.numeric.sparse.ISparseEqnInteger;
import org.interpss.pssl.plugin.IpssAdapter;
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
import com.interpss.core.aclf.AclfLoad;
import com.interpss.core.acsc.fault.SimpleFaultCode;
import com.interpss.core.algo.LoadflowAlgorithm;
import com.interpss.core.datatype.IFaultResult;
import com.interpss.core.net.Branch;
import com.interpss.dstab.BaseDStabBus;
import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabGen;
import com.interpss.dstab.DStabLoad;
import com.interpss.dstab.DStabilityNetwork;
import com.interpss.dstab.algo.DynamicSimuAlgorithm;
import com.interpss.dstab.algo.DynamicSimuMethod;
import com.interpss.dstab.cache.StateMonitor;
import com.interpss.simu.SimuContext;
import com.interpss.simu.SimuCtxType;
import java.util.Collections;

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
	LinkedHashMap<String,Double> obsrv_genQ = null;
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
	
	
	DstabRunConfigBean dynSimConfigBean = null;
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
	
	boolean isFirstInit = true;
	
	String absolutePath2DataFolder = null;
	
	double[][] all_observ_states = null;
	
	List<String> baseCaseFiles = null;
	List<Double> all_observ_states_list = null;
	List<String> actionBusIdList = new ArrayList<>();
	
	
	
	public IpssPyGateway() {
		IpssCorePlugin.init();
	    IpssLogger.getLogger().setLevel(Level.OFF);
	    ODMLogger.getLogger().setLevel(Level.OFF);
	    
		System.out.println("Working Directory = " +
	              System.getProperty("user.dir"));
		
		absolutePath2DataFolder = System.getProperty("user.dir");
	}
	
	
	public int[] initStudyCase(String[] caseFiles,  String dynSimConfigFile, String rlConfigFile) throws IOException {
		
		// use the current directory folder as the absolute Path to the Data Folder "testData\"
		return initStudyCase(caseFiles,  dynSimConfigFile, rlConfigFile,absolutePath2DataFolder);
		
	}
	
	/**
	 * Return the dimensions of the observation space and the action space {history_Observation_length,observation_space_size, action_space_dim};
	 * @param caseFiles
	 *  @param configJsonFile
	 * @return int [] 
	 * @throws IOException 
	 */
	public int[] initStudyCase(String[] caseFiles,  String dynSimConfigFile, String rlConfigFile, String path2DataFolder) throws IOException {
		
		caseInputFiles = new String[3];
		dynSimConfigJsonFile=dynSimConfigFile;
		rlConfigJsonFile = rlConfigFile;
		
		dynSimConfigBean  = BaseJSONBean.toBean(dynSimConfigFile, DstabRunConfigBean.class);
		
		if(caseFiles!=null) {
			caseInputFiles =caseFiles;
		}
		else if (dynSimConfigBean!=null) {
			boolean hasSeqFile = false;
			caseInputFiles[0] = dynSimConfigBean.acscConfigBean.runAclfConfig.aclfCaseFileName;
			
			if(dynSimConfigBean.acscConfigBean.seqFileName.length()>0) {
				caseInputFiles[1] = dynSimConfigBean.acscConfigBean.seqFileName;
				hasSeqFile =true;
			}
			
			if(dynSimConfigBean.dynamicFileName.length()>0) {
				
				if(hasSeqFile) {
					caseInputFiles[2] = dynSimConfigBean.dynamicFileName;
				}
				else {
					caseInputFiles[1] = dynSimConfigBean.dynamicFileName;
				    caseInputFiles = Arrays.copyOfRange(caseInputFiles, 0,2);
				}
			}
			
		}
		
		rlConfigBean = BaseJSONBean.toBean( rlConfigFile,ReinforcementLearningConfigBean.class);
		
		
		absolutePath2DataFolder = path2DataFolder;
		if(isFirstInit) {
			//get the base case files
			
			baseCaseFiles = new ArrayList<>();
			
			//first, add the initial base case to the list
			baseCaseFiles.add(caseInputFiles[0]); 
			
			//then, add other base cases under the study Case Folder defined in rlConfigBean to the list
			
			String folderPath = rlConfigBean.studyCaseFolder;
			
			if(folderPath!=null && !folderPath.isEmpty()) {
			    
				File dir = new File(absolutePath2DataFolder + File.separator + folderPath); 
				File[] listFiles = dir.listFiles((d, s) -> {
					return s.toLowerCase().endsWith(".raw");
				});
				
				if(listFiles!= null && listFiles.length>0) {
					for (File f: listFiles) {
						
						String newCaseString = dir + File.separator + f.getName();
						//System.out.println("File: " + newCaseString);
						if(!baseCaseFiles.contains(newCaseString))
								baseCaseFiles.add(newCaseString);
					} 
				}
			}
			
			Collections.sort(baseCaseFiles); //sort in ascending order;

			IpssLogger.getLogger().info("Imported power flow base case files:\n"+Arrays.toString(baseCaseFiles.toArray())+"\n");
			
			isFirstInit = false;
		}
		
		
		
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
			dstabAlgo.setSimuStepSec(dynSimConfigBean.simuTimeStepSec);
			dstabAlgo.setTotalSimuTimeSec(dynSimConfigBean.totalSimuTimeSec);
			
			if(!dynSimConfigBean.referenceGeneratorId.isEmpty())
				dstabAlgo.setRefMachine(this.dsNet.getMachine(dynSimConfigBean.referenceGeneratorId));
			
			// apply fault
			
			if(this.applyFaultDuringInitialization) {
				this.faultBusId = dynSimConfigBean.acscConfigBean.faultBusId;
				faultStartTime = dynSimConfigBean.eventStartTimeSec;
				faultDuration = dynSimConfigBean.eventDurationSec;
				
				if (this.faultBusId!= null && faultStartTime> 0 && faultDuration > 0)
		    	    dsNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent(this.faultBusId,this.dsNet,SimpleFaultCode.GROUND_3P, new Complex(0.0), null,faultStartTime,faultDuration),"3phaseFault@step-"+internalSimStepNum+"@"+faultBusId);
				else
					throw new Error("The fault settings are not correct! faultBusId, faultStartTime, faultDuration = "+ this.faultBusId+" ,"+faultStartTime+" ,"+faultDuration);
				
			}
			// set the monitoring variables
			
			this.sm = new StateMonitor();
			
			this.sm.addBusStdMonitor(dynSimConfigBean.monitoringBusAry);
			this.sm.addGeneratorStdMonitor(dynSimConfigBean.monitoringGenAry);
			
			
			// set the output handler
			dstabAlgo.setSimuOutputHandler(this.sm);
			
			dstabAlgo.setOutPutPerSteps(dynSimConfigBean.outputPerNSteps);
			
			
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
		
		IpssLogger.getLogger().info("Observed states:\n"+Arrays.toString(getEnvObservationNames()));
		IpssLogger.getLogger().info("Initial values of the observed states:\n"+Arrays.toString(observationAry));
		
		
		// prepare the return array
		int[] obs_action_dim_ary= {observation_space_dim[0],observation_space_dim[1],action_space_dim[0],action_space_dim[1]};
		
		return obs_action_dim_ary;
		
	}
	/**
	 * get flatten 1-D array 
	 * @return
	 */
    public byte[] getEnvObservationsByte1DAry() {
		/*
		 * Based on https://stackoverflow.com/questions/39095994/fast-conversion-of-java-array-to-numpy-array-py4j?noredirect=1&lq=1
		 */
    	
    	double[][] obs_ary = getEnvObservationsDbl2DAry();
    	int iMax = obs_ary.length;
    	int jMax = obs_ary[0].length;
    	
    	ByteBuffer dblBuffer = ByteBuffer.allocate(8+Double.BYTES*iMax*jMax); // header = 8 (2 ints), body = Double.BYTES*iMax*jMax)
	    dblBuffer.order(ByteOrder.LITTLE_ENDIAN); // Java's default is big-endian

	    dblBuffer.putInt(iMax);
	    dblBuffer.putInt(jMax);
	    // Copy ints from obs_Array into dblBuffer as bytes
	    for (int i = 0; i < iMax; i++) {
	        for (int j = 0; j < jMax; j++){
	            dblBuffer.putDouble(obs_ary[i][j]);
	        }
	    }

	    // Convert the ByteBuffer to a byte array and return it
	    byte[] byteArray = dblBuffer.array();
	    
	    return byteArray;
    	
	}

   
	/**
	 * get a flatten 1-D array of 2-D observations array for historical N steps that is internally saved in the form of new double[historyObservSize][record_size];
	 * @return
	 */
	
	public double[] getEnvObservationsDbl1DAry() {
		/**
		 * Based on https://stackoverflow.com/questions/2569279/how-to-flatten-2d-array-to-1d-array
		 */
		return Arrays.stream(getEnvObservationsDbl2DAry())
		        .flatMapToDouble(Arrays::stream)
		        .toArray();
	}
	//TODO hashtable to store the past N step internal "states" for output as an environment state
    public double[][] getEnvObservationsDbl2DAry() {
    	
    	 
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
    
    
    public String[] getEnvObservationNames() {
    	
    	return obsrv_state_names.toArray(new String[0]);
    	
    }
    
    public String[] getActionBusIds() {
    	return this.actionBusIds;
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
		else if(this.rlConfigBean.environmentName.contains("FIDVR_LoadShedding")){
			double sumOfVoltageDeviation = 0.0;
			double sumOfLoadShedPU =0.0;
			double maxRecoveryTime = this.rlConfigBean.maxVoltRecoveryTime;
			
			int sumOfInvalidActions = 0;
			
			boolean isMiniRecoveryMet = true;
			
			List<String> miniVoltRecoveryViolationBusList = new ArrayList<>();
			
			
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
							if(voltMag < this.rlConfigBean.minVoltRecoveryLevel) {
								isMiniRecoveryMet = false;
								miniVoltRecoveryViolationBusList.add(busId);
							}
							
						
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
					IpssLogger.getLogger().severe("Violation bus list: "+Arrays.toString(miniVoltRecoveryViolationBusList.toArray()));
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
		
		if(actionValueAry!=null) {
			this.agentActionValuesAry = Arrays.copyOf(actionValueAry, actionValueAry.length);
			
			if(this.dstabAlgo.getSimuTime() < this.faultStartTime){
				for(int i = 0; i <this.agentActionValuesAry.length;i++){
					if( Math.abs(this.agentActionValuesAry[i]) > 0.0){ // non-zero values will be detected
						this.isPreFaultActionApplied = true;
						
						this.agentActionValuesAry[i] = 0.0; // force it to zero; invalid actions will not be applied
						
						IpssLogger.getLogger().info("Pre-fault(event) action: index, value = "+i+", "+actionValueAry[i]);
					}
				}
			}
		   // 
		    IpssLogger.getLogger().info("Apply actions at time ="+this.dstabAlgo.getSimuTime());
		    applyAction(this.agentActionValuesAry, actionType, stepTimeInSec);
		}
		
		if (this.isPreFaultActionApplied)
			IpssLogger.getLogger().warning("To help training, any non-zero action being applied prior to the fault(event) time is regarded as invalid, thus will be forced to zero and not applied to simulator!\n"); 
		
		for(int i = 0; i<internalSteps; i++) {
			if(dstabAlgo.getSimuTime()<dstabAlgo.getTotalSimuTimeSec()) {
				
				simFlag = dstabAlgo.solveDEqnStep(true);
				
				// record observations at the predefined steps
				if(this.internalSimStepNum % this.dynSimConfigBean.outputPerNSteps == 0){
					
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
    	this.internalSimStepNum = 0;
    	this.internalObsrvRecordNum = 0;
    	
    	this.isSimulationDone = false;
    	this.applyFaultDuringInitialization = false;
    	this.isActionApplied = false;
    	
    	this.observationHistoryRecord.clear();
    	
    	this.agentActionValuesAry = null;
    	this.sm= null;
    	
    	this.faultStartTime = faultStartTime;
    	this.faultDuration = faultDuration;
    	
    	
        //update caseInputFiles using the base case associated with the input case index 
    	//TODO, in the future, the input dynamic file could also be updated if associated dynamic files are provided.
    	
    	if(caseIdx>=0 && baseCaseFiles.size()>caseIdx) {
    		caseInputFiles[0] = baseCaseFiles.get(caseIdx);
    	}
    	else {
    		throw new Error("Error in the caseIdx in reset() function inpute, caseIdx must be less than number of cases. However, caseIdx ="+caseIdx+", # of total cases ="+baseCaseFiles.size());
    		
    	}
    	

    	
    	try {
    		initDimAry = initStudyCase(caseInputFiles,  dynSimConfigJsonFile, rlConfigJsonFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	
    	if(faultBusIdx>= 0 && faultBusIdx<this.rlConfigBean.faultBusCandidates.length) {
    	    this.faultBusId = this.rlConfigBean.faultBusCandidates[faultBusIdx];
    	    if (this.dsNet.getBus(this.faultBusId)==null) {
    	    	this.faultBusId= null;
    	    	throw new Error(("Error in the faultBusId in faultBusCandidates list in RL json configure file, index="+faultBusIdx));
        		
    	    }
    	}
    	else {
    		throw new Error(("The faultBusIdx is outside the faultBusCandidates list defined in RL json configure file!"));
    		
    	}
    	
    	if (this.faultBusId!= null && faultStartTime>= 0.0 && faultDuration > 0.0){
    	    dsNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent(this.faultBusId,this.dsNet,SimpleFaultCode.GROUND_3P, new Complex(0.0), null,faultStartTime,faultDuration),"3phaseFault@"+faultBusId);
    	}
    	else{
    		this.faultStartTime = 0.0;
        	this.faultDuration = 0.0;
    	}
    	
    	IpssLogger.getLogger().info(String.format("Case id: %d, Fault bus id: %s, fault start time: %f, fault duration: %f", caseIdx, faultBusId,faultStartTime,faultDuration));
    	
    	return initDimAry;
    	
	}
	
	private boolean loadStudyCase(String[] caseFiles) {	
		//IpssCorePlugin.init();
		IPSSMsgHub msg = CoreCommonFactory.getIpssMsgHub();
		
		if(dynSimConfigBean.acscConfigBean.runAclfConfig.format!=IpssAdapter.FileFormat.PSSE) {
			IpssLogger.getLogger().severe("Error: Input file is not PSS/E format based on the configuration json file input. Currently ONLY PSS/E format is supported");
			return false;
		}
		
		
		org.interpss.pssl.plugin.IpssAdapter.PsseVersion ver = dynSimConfigBean.acscConfigBean.runAclfConfig.version;
		
		PsseVersion version = (ver == org.interpss.pssl.plugin.IpssAdapter.PsseVersion.PSSE_30?PsseVersion.PSSE_30:PsseVersion.PSSE_33);
				
		PSSEAdapter adapter = new PSSEAdapter(version);
		
		IpssLogger.getLogger().info("Case files:"+Arrays.toString(caseFiles));
		
		adapter.parseInputFile(NetType.DStabNet, caseFiles);
		DStabModelParser parser =(DStabModelParser) adapter.getModel();
		
		
		SimuContext simuCtx = SimuObjectFactory.createSimuNetwork(SimuCtxType.DSTABILITY_NET);
		if (!new ODMDStabParserMapper(msg)
					.map2Model(parser, simuCtx)) {
			IpssLogger.getLogger().severe("Error: ODM model to InterPSS SimuCtx mapping error, please contact support@interpss.com");
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
		
		all_observ_states = getEnvObservationsDbl2DAry();
		
		
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
		
		actionBusIdList.clear();
		
		for(DStabBus bus:dsNet.getBusList()) {
			if(bus.getBaseVoltage()>=rlConfigBean.actionVoltThreshold) {
				if(bus.isActive()) {
					if(isBusWithinScope(bus,scopeType,scopeAry)) {
						
						for(String actionType: actionTypes) {
							if(actionType.equalsIgnoreCase("LoadShed")) {
								//check if it is a load
								if(bus.isLoad()||!bus.getContributeLoadList().isEmpty()) {
									if((bus.getLoadP()*dsNet.getBaseMva()) >=rlConfigBean.actionPowerMWThreshold) {
										action_location_num++;
										actionBusIdList.add(bus.getId());
									}
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
	
	private boolean isBusWithinScope(BaseDStabBus<?,?> bus, String scopeType,String[] scopeAry) {
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
    	
//        String[] obsrvStateTypes = rlConfigBean.observationStateTypes;
		
		String scopeType = rlConfigBean.observationScopeType;
		
//		String[] scopeAry = rlConfigBean.observationScopeAry;
		
		
		// The reason of using separate hashtables to store the states is to keep the same type of states in a consecutive form, as the implementation below is iterating over
		// the bus, not the state type first.
		// Then different types of states are concatted to one large vector/array
		
		for(String stateType : rlConfigBean.observationStateTypes) {
			if(stateType.equalsIgnoreCase("frequency")) {
				
				if(obsrv_freq==null)
					obsrv_freq = new LinkedHashMap<>();
				else {
					obsrv_freq.clear();
				}
			}
			else if(stateType.equalsIgnoreCase("voltageMag")) {
				
				if(obsrv_voltMag==null)
					obsrv_voltMag = new LinkedHashMap<>();
				else {
					obsrv_voltMag.clear();
					
				}
			}
			else if(stateType.equalsIgnoreCase("voltageAng")) {
				
				if(obsrv_voltAng==null)
					obsrv_voltAng = new LinkedHashMap<>();
				else {
					obsrv_voltAng.clear();
					
				}
			}
			
			else if(stateType.equalsIgnoreCase("loadP")) {
				
				if(obsrv_loadP==null)
					obsrv_loadP = new LinkedHashMap<>();
				else {
					obsrv_loadP.clear();
					
				}
				
			}
			else if(stateType.equalsIgnoreCase("loadQ")) {
				if(obsrv_loadQ==null)
					obsrv_loadQ = new LinkedHashMap<>();
				else {
					obsrv_loadQ.clear();
					
				}
			}
			else if(stateType.equalsIgnoreCase("genSpeed")) {

				if(obsrv_genSpd==null)
					obsrv_genSpd = new LinkedHashMap<>();
				else {
					obsrv_genSpd.clear();
				}
			}
            else if(stateType.equalsIgnoreCase("genAngle")) {
            	if(obsrv_genAng==null)
        			obsrv_genAng = new LinkedHashMap<>();
        		else {
        			obsrv_genAng.clear();
        		}
			}
            else if(stateType.equalsIgnoreCase("genP")) {
            	if(obsrv_genP==null)
        			obsrv_genP = new LinkedHashMap<>();
        		else {
        			obsrv_genP.clear();
        		}
			} 
			
            else if(stateType.equalsIgnoreCase("genQ")) {
            	if(obsrv_genQ==null)
        			obsrv_genQ = new LinkedHashMap<>();
        		else {
        			obsrv_genQ.clear();
        		}
			} 
		}
	
		
		
		//obsrv_genP = new LinkedHashMap<>();
		
		
		obsrv_state_names = new ArrayList<>();
		
		if(all_observ_states_list == null)
			all_observ_states_list = new ArrayList<>();
		else {
			all_observ_states_list.clear();
		}
		
		
		// iterate over all buses and check whether it fits the scope (type and definitions);
	
				
		for(DStabBus bus:dsNet.getBusList()) {
			if(bus.isActive()) {
				if(bus.getBaseVoltage()>=rlConfigBean.observationVoltThreshold)
				  if(isBusWithinScope(bus,scopeType,rlConfigBean.observationScopeAry)) {
					
					for(String stateType : rlConfigBean.observationStateTypes) {
						if(stateType.equalsIgnoreCase("frequency")) {
							
							obsrv_freq.put("frequency_"+bus.getId(),bus.getFreq());
						}
						else if(stateType.equalsIgnoreCase("voltageMag")) {
							
							obsrv_voltMag.put("voltageMag_"+bus.getId(),bus.getVoltageMag());
						}
						else if(stateType.equalsIgnoreCase("voltageAng")) {
							
							obsrv_voltAng.put("voltageAng_"+bus.getId(),bus.getVoltageAng());
						}
						
						else if(stateType.equalsIgnoreCase("loadP")) {
							if(bus.isLoad() || bus.getContributeLoadList().size()>0) {
								//TODO need to update to capture dynamic total loads
							   obsrv_loadP.put("loadP_"+bus.getId(),bus.getLoadP());
							}
						}
						else if(stateType.equalsIgnoreCase("loadQ")) {
							if(bus.isLoad() || bus.getContributeLoadList().size()>0) {
							  //TODO need to update to capture dynamic total loads
							   obsrv_loadQ.put("loadQ_"+bus.getId(),bus.getLoadQ());
							}
						}
						else if(stateType.equalsIgnoreCase("genSpeed")) {
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
						else if(stateType.equalsIgnoreCase("genAngle")) {
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
						
						else if(stateType.equalsIgnoreCase("genP")) {
							if(bus.isGen() || bus.getContributeGenList().size()>0) {
							    for(AclfGen gen: bus.getContributeGenList()) {
							       if(gen.isActive()) {
							    	   DStabGen dsGen = (DStabGen) gen;
							    	   if(dsGen.getMach()!=null) {
								         
								           obsrv_genP.put("genP_"+dsGen.getMach().getId(),dsGen.getMach().getPe());
							    	   }
							       }
							    }
							   
							}
						}
						
						else if(stateType.equalsIgnoreCase("genQ")) {
							if(bus.isGen() || bus.getContributeGenList().size()>0) {
							    for(AclfGen gen: bus.getContributeGenList()) {
							       if(gen.isActive()) {
							    	   DStabGen dsGen = (DStabGen) gen;
							    	   if(dsGen.getMach()!=null) {
								         
								           obsrv_genQ.put("genQ_"+dsGen.getMach().getId(),dsGen.getMach().getQGen());
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
		if(obsrv_freq!=null) {
			all_observ_states_list.addAll(obsrv_freq.values());
			// the observed state names
			obsrv_state_names.addAll(obsrv_freq.keySet());
		}
		if(obsrv_voltMag!=null) {
			all_observ_states_list.addAll(obsrv_voltMag.values());
			obsrv_state_names.addAll(obsrv_voltMag.keySet());
		}
		if(obsrv_voltAng!=null) {
			all_observ_states_list.addAll(obsrv_voltAng.values());
			obsrv_state_names.addAll(obsrv_voltAng.keySet());
		}
		if(obsrv_loadP!=null) {
			all_observ_states_list.addAll(obsrv_loadP.values());
			obsrv_state_names.addAll(obsrv_loadP.keySet());
		}
		if(obsrv_loadQ!=null) {
			all_observ_states_list.addAll(obsrv_loadQ.values());
			obsrv_state_names.addAll(obsrv_loadQ.keySet());
		}
		if(obsrv_genSpd!=null) {
			all_observ_states_list.addAll(obsrv_genSpd.values());
			obsrv_state_names.addAll(obsrv_genSpd.keySet());
		}
		if(obsrv_genAng!=null) {
			all_observ_states_list.addAll(obsrv_genAng.values());
			obsrv_state_names.addAll(obsrv_genAng.keySet());
		}
		if(obsrv_genP!=null) {
			all_observ_states_list.addAll(obsrv_genP.values());
			obsrv_state_names.addAll(obsrv_genP.keySet());
		}
		if(obsrv_genQ!=null) {
			all_observ_states_list.addAll(obsrv_genQ.values());
			obsrv_state_names.addAll(obsrv_genQ.keySet());
		}
		
		
		observationAry = Stream.of(all_observ_states_list.toArray(new Double[0])).mapToDouble(Double::doubleValue).toArray();
		
		
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
	
	
	public String[] getBaseCases(){
		return this.baseCaseFiles.toArray(new String[] {});
		
	}
	
	public int getTotalBaseCaseNumber() {
		return this.baseCaseFiles.size();
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
		// check the consistency of dimension of actionValueRanges array and the action space
		if(this.rlConfigBean.actionValueRanges.length!=this.actionBusIdList.size()) {
			double[][] temp = this.rlConfigBean.actionValueRanges;
			
			if(temp.length>this.actionBusIdList.size()) {
				this.rlConfigBean.actionValueRanges = Arrays.copyOfRange(temp, 0, this.actionBusIdList.size());
			}
			else {// temp.length<this.actionBusIdList.size(), need to extend the array by copy 
				double[][] temp2 = new double[this.actionBusIdList.size()][temp[0].length];
				System.arraycopy(temp, 0, temp2, 0, temp.length);
				Arrays.fill(temp2, temp.length, this.actionBusIdList.size(), temp[temp.length-1]);
				this.rlConfigBean.actionValueRanges = temp2;
			}
		}
		
		return this.rlConfigBean.actionValueRanges;
	}
	
	/**
	 * Get on-line generation total real power, in PU
	 * @return
	 */
	public double getTotalGenerationPU() {
		double totalGen = 0;
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isGen()) {
				for(AclfGen gen: bus.getContributeGenList()) {
					if(gen.isActive())
					    totalGen += gen.getGen().getReal(); // Gen is pu on system mva base
				}
			}
		}
		
		return totalGen;
	}
	
	 /**
	  * Get on-line generation total capacity (Pmax), in PU
	  * @return
	  */
	public double getMaxOnlineGenerationCapacity() {
		
		double totalGenMax = 0;
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isGen()) {
				for(AclfGen gen: bus.getContributeGenList()) {
					if(gen.isActive())
					    totalGenMax += gen.getPGenLimit().getMax(); // Gen is pu on system mva base
				}
			}
		}
		
		return totalGenMax; //convert from PU to MW.
		
	}
	
	/**
	 * Get the generation active power in P.U. of each on-line generator, and return them in an array
	 * @return double[]
	 */
	public double[] getGenerationPAry() {
		
		List<Double> genPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isGen()) {
				for(AclfGen gen: bus.getContributeGenList()) {
					if(gen.isActive())
						genPList.add(gen.getGen().getReal()); // Gen is pu on system mva base
				}
			}
		}
		
		 double[] genPAry = genPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return genPAry;
	}

    
    public double[] getGenerationPByZoneAry(int zoneNumber) {
    	List<Double> genPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isGen() && bus.getZone().getNumber()==zoneNumber) {
				for(AclfGen gen: bus.getContributeGenList()) {
					if(gen.isActive())
						genPList.add(gen.getGen().getReal()); // Gen is pu on system mva base
				}
			}
		}
		
		 double[] genPAry = genPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return genPAry;
	}
    
    public double[] getGenerationPByAreaAry(int areaNumber) {
    	List<Double> genPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isGen() && bus.getArea().getNumber()==areaNumber) {
				for(AclfGen gen: bus.getContributeGenList()) {
					if(gen.isActive())
						genPList.add(gen.getGen().getReal()); // Gen is pu on system mva base
				}
			}
		}
		
		 double[] genPAry = genPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return genPAry;
	}
    
	
	/**
	 * Get each load active power in P.U. and return them in an array
	 * @return
	 */
    public double[] getLoadPAry() {
    	List<Double> loadPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isLoad()) {
				for(AclfLoad load: bus.getContributeLoadList()) {
					if(load.isActive())
						loadPList.add(load.getLoad(bus.getVoltageMag()).getReal()); // Load is pu on system mva base
				}
			}
		}
		
		 double[] loadPAry = loadPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return loadPAry;
	}
    
    public double[] getLoadPByZoneAry(int zoneNumber) {
    	List<Double> loadPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isLoad()&& bus.getZone().getNumber()==zoneNumber) {
				for(AclfLoad load: bus.getContributeLoadList()) {
					if(load.isActive())
						loadPList.add(load.getLoad(bus.getVoltageMag()).getReal()); // Load is pu on system mva base
				}
			}
		}
		
		 double[] loadPAry = loadPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return loadPAry;
	}
    
    public double[] getLoadPByAreaAry(int areaNumber) {
    	List<Double> loadPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isLoad()&& bus.getArea().getNumber()==areaNumber) {
				for(AclfLoad load: bus.getContributeLoadList()) {
					if(load.isActive())
						loadPList.add(load.getLoad(bus.getVoltageMag()).getReal()); // Load is pu on system mva base
				}
			}
		}
		
		 double[] loadPAry = loadPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return loadPAry;
	}
    
   public double[] getLoadPWithinActionScope() {
    	List<Double> loadPList = new ArrayList<Double>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isLoad() && 
			   isBusWithinScope(bus,rlConfigBean.actionScopeType,rlConfigBean.actionScopeAry)) {
				if(bus.getContributeLoadList().isEmpty()) {
					loadPList.add(bus.getLoadP());
				}
				else { 
					for(AclfLoad load: bus.getContributeLoadList()) {
						if(load.isActive())
							loadPList.add(load.getLoad(bus.getVoltageMag()).getReal()); // Load is pu on system mva base
					 }
				}
			}
		}
		
		 double[] loadPAry = loadPList.stream().mapToDouble(Double::doubleValue).toArray();
		 
		 return loadPAry;
    }
   
   public String[] getLoadIdWithinActionScope() {
   	List<String> loadIdList = new ArrayList<>();
		for(BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
			if(bus.isActive() && bus.isLoad() && 
			   isBusWithinScope(bus,rlConfigBean.actionScopeType,rlConfigBean.actionScopeAry)) {
				 for(AclfLoad load: bus.getContributeLoadList()) {
					if(load.isActive())
						loadIdList.add(bus.getId()+"_"+load.getId()); // Load is pu on system mva base
				 }
			}
		}
		
		 String[] loadIdAry = loadIdList.toArray(new String[0]);
		 
		 return loadIdAry;
   }
   
   public int[][] getAdjacencyMatrix() {
	   int k = 0;
	   for (BaseDStabBus<DStabGen,DStabLoad> bus: this.dsNet.getBusList()) {
		   bus.setSortNumber(k);
		   k++;
	   }
	   
	   ISparseEqnInteger  aMatrix= this.dsNet.formAdjacencyMatrix();
	   int dim= aMatrix.getDimension();
	   int[][] adjMatrix = new int[dim][dim];
	   for(int i=0;i<dim;i++){
			for(int j=0;j<dim;j++){
				adjMatrix[i][j]=aMatrix.getAijElement(i,j);
					
			}
			
		}
	   
	   return adjMatrix;
   }
   
   public byte[] getAdjacencyMatrixByte1DAry() {
	   int[][] adj_ary = getAdjacencyMatrix();
   	   int iMax = adj_ary.length;
       int jMax = adj_ary[0].length;
   	
   	    ByteBuffer intBuffer = ByteBuffer.allocate(8+Integer.BYTES*iMax*jMax); // header = 8 (2 ints), body = Double.BYTES*iMax*jMax)
	    intBuffer.order(ByteOrder.LITTLE_ENDIAN); // Java's default is big-endian

	    intBuffer.putInt(iMax);
	    intBuffer.putInt(jMax);
	    // Copy ints from obs_Array into dblBuffer as bytes
	    for (int i = 0; i < iMax; i++) {
	        for (int j = 0; j < jMax; j++){
	            intBuffer.putInt(adj_ary[i][j]);
	        }
	    }

	    // Convert the ByteBuffer to a byte array and return it
	    byte[] byteArray = intBuffer.array();
	    
	    return byteArray;
   	
   }
   
   public void setBranchStatus(int fromNum, int toNum, String cirId, int status) {
	   String fBusId = "Bus"+fromNum;
	   String tBusId = "Bus"+toNum;
	   
	   Branch bra = dsNet.getBranch(fBusId, tBusId, cirId);
	   
	   if(bra!=null) {
		   bra.setStatus(status==0?false:true);
	   }
	   else {
		   IpssLogger.getLogger().severe("No branch is found for the input fromNum, toNum and cirId: "+ fromNum+","+toNum+","+cirId);;
	   }
	   
   }
   
   public void setGeneratorStatus(int busNum, String genId, int status) {
	   String busId =  "Bus"+busNum;
	   DStabBus bus = dsNet.getBus(busId);
	   
	 
	   if(bus.getContributeGenList().size()>0) {
		   for(AclfGen gen:bus.getContributeGenList()) {
			   if(gen.getId().equals(genId)){
				   gen.setStatus(status==0?false:true);
			   }
		   }
	   }
	   
   }
   
   public void setLoadStatus(int busNum, String loadId, int status) {
	   String busId =  "Bus"+busNum;
	   DStabBus bus = dsNet.getBus(busId);
	   
	 
	   if(bus.getContributeLoadList().size()>0) {
		   for(AclfLoad load:bus.getContributeLoadList()) {
			   if(load.getId().equals(loadId)){
				   load.setStatus(status==0?false:true);
			   }
		   }
	   }
   }

	public void setLoggerLevel(int level) {
		if(level>2) {
			IpssLogger.getLogger().setLevel(Level.FINE);
			ODMLogger.getLogger().setLevel(Level.FINE);
		}
		if(level==2) {
			IpssLogger.getLogger().setLevel(Level.INFO);
			ODMLogger.getLogger().setLevel(Level.INFO);
		}
		else if(level==1) {
			IpssLogger.getLogger().setLevel(Level.WARNING);
			ODMLogger.getLogger().setLevel(Level.WARNING);
		}
		else {
			IpssLogger.getLogger().setLevel(Level.OFF);
			ODMLogger.getLogger().setLevel(Level.OFF);
		}
	}
	
	
	/**
	 * main method to start the service
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		IpssPyGateway app = new IpssPyGateway();
		
		// app is now the gateway.entry_point
		int port = 25332;
		int logLevel = 0;
		
		if (args.length>0) {
			port = Integer.valueOf(args[0]);
			
			if(args.length>1) {
				logLevel = Integer.valueOf(args[1]);
				
			}
		}
		if(logLevel>=2) {
			IpssLogger.getLogger().setLevel(Level.FINE);
		}
		else if(logLevel==1) {
			IpssLogger.getLogger().setLevel(Level.WARNING);
		}
		else {
			IpssLogger.getLogger().setLevel(Level.OFF);
		}
		
			
		GatewayServer server = new GatewayServer(app,port);

		System.out.println("InterPSS Engine for Reinforcement Learning (IPSS-RL) developed by Qiuhua Huang @ PNNL. Version 1.0.0_rc, built on 12/14/2020");

		System.out.println("Starting Py4J " + app.getClass().getTypeName() + " at port ="+port);
		server.start();
	}
	
	
}
