package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.ieee.odm.adapter.IODMAdapter.NetType;
import org.ieee.odm.adapter.psse.PSSEAdapter;
import org.ieee.odm.adapter.psse.PSSEAdapter.PsseVersion;
import org.ieee.odm.model.dstab.DStabModelParser;
import org.interpss.IpssCorePlugin;
import org.interpss.mapper.odm.ODMDStabParserMapper;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.CoreCommonFactory;
import com.interpss.SimuObjectFactory;
import com.interpss.common.exp.InterpssException;
import com.interpss.common.msg.IPSSMsgHub;
import com.interpss.common.util.IpssLogger;
import com.interpss.dstab.BaseDStabNetwork;
import com.interpss.simu.SimuContext;
import com.interpss.simu.SimuCtxType;

public class Dstab_Texas2000Bus_Test {
	
	@Test
	public void test_Texas2000_loadshedding_RL_continuous() {
		
	    IpssPyGateway app = new IpssPyGateway();
	    app.setLoggerLevel(0); //0 - no logging, 1 - warming, 2 - info
	    
		String[] caseFiles = new String[]{
				"testData/ACTIVSg2000/ACTIVSg2000.raw",
				"testData/ACTIVSg2000/ACTIVSg2000_dyn_cmld_zone3_v1.dyr"
				};
		
		String dynSimConfigFile = "testData/ACTIVSg2000/json/Texas2000_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData/ACTIVSg2000/json/Texas2000_RL_loadShedding_zone3.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));

			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		app.reset(0, 0, 0.0, 0.1);
		
		int[][] adjMatrix = app.getAdjacencyMatrix();
		System.out.println("adj matrix Size: "+adjMatrix.length+","+adjMatrix[0].length);
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		
		// zone 3 and 4, observations: 176 buses (>60 kV)
		// zone 3, actions: 69 buses


		System.out.println("observation length: " + obs_ary[0].length);
		System.out.println("Observations:\n"+Arrays.toString(app.getEnvObservationNames()));
		assertTrue(obs_ary.length == 1);
		assertTrue(obs_ary[0].length == (176+69));
		
		System.out.println(Arrays.toString(obs_ary[0]));
		
		System.out.println("Action buses:\n"+Arrays.toString(app.getActionBusIds()));
		assertTrue(app.getActionValueRanges().length==app.getActionBusIds().length);
		System.out.println("Action value ranges:\n"+Arrays.toString(app.getActionValueRanges()));
		
		// define the action values
		double[] actions = new double[app.getActionBusIds().length];
		Arrays.fill(actions,-0.2); // assuming uniform loadshedding at 20% for each time step
		
		int i = 0;
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			System.out.println("Time ="+app.getDStabAlgo().getSimuTime());
			
//			app.nextStepDynSim(0.1, actions, "continuous");
			
			if(app.getDStabAlgo().getSimuTime()<0.1)
			     app.nextStepDynSim(0.1, new double[69], "continuous");
			else if(i<3) {
				app.nextStepDynSim(0.1, actions, "continuous");
				i++;
			}
			else {
				app.nextStepDynSim(0.1, new double[69], "continuous");
			}
			   
			
//			if(app.getDStabAlgo().getSimuTime()>1.1 && app.getDStabAlgo().getSimuTime()<1.85){
//				app.nextStepDynSim(0.1, actions, "continuous");
//			} 
//			else
//				app.nextStepDynSim(0.1, new double[46], "continuous");
//			
			//app.nextStepDynSim(0.1, new double[20], "continuous");
			//app.nextStepDynSim(0.1, actions, "continuous");
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
//			if(app.getDStabAlgo().getSimuTime()>1.0)
//			    break;
//			
		}
		
		
		
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
		//texas2000_hr_4375_v30_mod.raw
	}
	
	//@Test
	public void test_Texas2000_hour4375_loadshedding_RL_continuous() {
		
	    IpssPyGateway app = new IpssPyGateway();
	    app.setLoggerLevel(1); //0 - no logging, 1 - warming, 2 - info
	    
		String[] caseFiles = new String[]{
				"testData/ACTIVSg2000/full_texas2000_hr_4375_v33_mod.raw",
				"testData/ACTIVSg2000/ACTIVSg2000_dyn_acmotor_zone3_v1.dyr"
				};
		
		String dynSimConfigFile = "testData/ACTIVSg2000/json/Texas2000_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData/ACTIVSg2000/json/Equiv_Texas2000_RL_loadShedding_zone3.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));

			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		app.reset(0, 0, 0.0, 0.1);
		
		int[][] adjMatrix = app.getAdjacencyMatrix();
		System.out.println("adj matrix Size: "+adjMatrix.length+","+adjMatrix[0].length);
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		
		// zone 3 and 4, observations: 176 buses (>60 kV)
		// zone 3, actions: 69 buses


		System.out.println("observation length: " + obs_ary[0].length);
		System.out.println("Observations:\n"+Arrays.toString(app.getEnvObservationNames()));
		assertTrue(obs_ary.length == 1);
		//assertTrue(obs_ary[0].length == (176+69));
		
		System.out.println(Arrays.toString(obs_ary[0]));
		
		System.out.println("Action buses:\n"+Arrays.toString(app.getActionBusIds()));
		assertTrue(app.getActionValueRanges().length==app.getActionBusIds().length);
		System.out.println("Action value ranges:\n"+Arrays.toString(app.getActionValueRanges()));
		
		// define the action values
		double[] actions = new double[app.getActionBusIds().length];
		Arrays.fill(actions,-0.2); // assuming uniform loadshedding at 20% for each time step
		
		int i = 0;
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			System.out.println("Time ="+app.getDStabAlgo().getSimuTime());
			
//			app.nextStepDynSim(0.1, actions, "continuous");
			
			if(app.getDStabAlgo().getSimuTime()<0.1)
			     app.nextStepDynSim(0.1, new double[ob_act_space_dim[2]], "continuous");
			else if(i<0) {
				app.nextStepDynSim(0.1, actions, "continuous");
				i++;
			}
			else {
				app.nextStepDynSim(0.1, new double[ob_act_space_dim[2]], "continuous");
			}
			   
			
//			if(app.getDStabAlgo().getSimuTime()>1.1 && app.getDStabAlgo().getSimuTime()<1.85){
//				app.nextStepDynSim(0.1, actions, "continuous");
//			} 
//			else
//				app.nextStepDynSim(0.1, new double[46], "continuous");
//			
			//app.nextStepDynSim(0.1, new double[20], "continuous");
			//app.nextStepDynSim(0.1, actions, "continuous");
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
//			if(app.getDStabAlgo().getSimuTime()>1.0)
//			    break;
//			
		}
		
		
		
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
		
	}

	
	//@Test
	public void test_Equiv_Texas2000_loadshedding_RL_continuous() {
		
	    IpssPyGateway app = new IpssPyGateway();
	    app.setLoggerLevel(0); //0 - no logging, 1 - warming, 2 - info
	    
		String[] caseFiles = new String[]{
				"testData/ACTIVSg2000/texas2000_hr_4375_v33_mod_area7.raw",
				"testData/ACTIVSg2000/ACTIVSg2000_dyn_acmotor_zone3_v1.dyr"
				};
		
		String dynSimConfigFile = "testData/ACTIVSg2000/json/Texas2000_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData/ACTIVSg2000/json/Equiv_Texas2000_RL_loadShedding_zone3.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));

			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		app.reset(0, 0, 0.0, 0.1);
		
		int[][] adjMatrix = app.getAdjacencyMatrix();
		System.out.println("adj matrix Size: "+adjMatrix.length+","+adjMatrix[0].length);
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		
		// zone 3 and 4, observations: 176 buses (>60 kV)
		// zone 3, actions: 69 buses


		System.out.println("observation length: " + obs_ary[0].length);
		System.out.println("Observations:\n"+Arrays.toString(app.getEnvObservationNames()));
		assertTrue(obs_ary.length == 1);
		//assertTrue(obs_ary[0].length == (176+69));
		
		System.out.println(Arrays.toString(obs_ary[0]));
		
		System.out.println("Action buses:\n"+Arrays.toString(app.getActionBusIds()));
		assertTrue(app.getActionValueRanges().length==app.getActionBusIds().length);
		System.out.println("Action value ranges:\n"+Arrays.toString(app.getActionValueRanges()));
		
		// define the action values
		double[] actions = new double[app.getActionBusIds().length];
		Arrays.fill(actions,-0.2); // assuming uniform loadshedding at 20% for each time step
		
		int i = 0;
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			System.out.println("Time ="+app.getDStabAlgo().getSimuTime());
			
//			app.nextStepDynSim(0.1, actions, "continuous");
			
			if(app.getDStabAlgo().getSimuTime()<0.1)
			     app.nextStepDynSim(0.1, new double[ob_act_space_dim[2]], "continuous");
			else if(i<3) {
				app.nextStepDynSim(0.1, actions, "continuous");
				i++;
			}
			else {
				app.nextStepDynSim(0.1, new double[ob_act_space_dim[2]], "continuous");
			}
			   
			
//			if(app.getDStabAlgo().getSimuTime()>1.1 && app.getDStabAlgo().getSimuTime()<1.85){
//				app.nextStepDynSim(0.1, actions, "continuous");
//			} 
//			else
//				app.nextStepDynSim(0.1, new double[46], "continuous");
//			
			//app.nextStepDynSim(0.1, new double[20], "continuous");
			//app.nextStepDynSim(0.1, actions, "continuous");
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
//			if(app.getDStabAlgo().getSimuTime()>1.0)
//			    break;
//			
		}
		
		
		
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
		
	}
	
	@Test
	public void process_Zone_Data() throws InterpssException{
		IpssCorePlugin.init();
		
		IPSSMsgHub msg = CoreCommonFactory.getIpssMsgHub();
		IpssLogger.getLogger().setLevel(Level.WARNING);
		PSSEAdapter adapter = new PSSEAdapter(PsseVersion.PSSE_30);
		assertTrue(adapter.parseInputFile(NetType.DStabNet, new String[]{
				"testData/IEEE300/IEEE300Bus_modified_noHVDC_v2.raw",
				"testData/IEEE300/IEEE300_dyn_cmld_zone1.dyr"
		}));
		DStabModelParser parser =(DStabModelParser) adapter.getModel();
		
		//System.out.println(parser.toXmlDoc());
        
		
		
		SimuContext simuCtx = SimuObjectFactory.createSimuNetwork(SimuCtxType.DSTABILITY_NET);
		if (!new ODMDStabParserMapper(msg)
					.map2Model(parser, simuCtx)) {
			System.out.println("Error: ODM model to InterPSS SimuCtx mapping error, please contact support@interpss.com");
			return;
		}
		
		
	    BaseDStabNetwork dsNet =simuCtx.getDStabilityNet();
	}

}
