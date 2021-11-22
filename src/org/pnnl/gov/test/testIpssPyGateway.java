package org.pnnl.gov.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.interpss.numeric.matrix.MatrixUtil;
import org.interpss.numeric.util.NumericUtil;
import org.interpss.util.FileUtil;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;


public class testIpssPyGateway {
	
	@Test
	public void test_basic_functions() {
		IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\Kundur-2area\\kunder_2area_ver30.raw",
				"testData\\Kundur-2area\\kunder_2area.dyr"
				};
		
		String dynSimConfigFile = "testData\\Kundur-2area\\json\\kundur2area_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\Kundur-2area\\json\\kundur2area_RL_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//check environment name
		assertTrue(app.getRLConfigBean().environmentName.equalsIgnoreCase("kundur-2area"));
		
		
		// check whether the action space and the observation space are created, and their dimensions
		assertTrue(ob_act_space_dim!=null);
		
		int ob_space_dim = ob_act_space_dim[1];
		int act_space_dim = ob_act_space_dim[2];
		
		System.out.println("ob_act_space_dim: " +ob_space_dim+","+act_space_dim);
		assertTrue(ob_space_dim ==4*2*1);   //observation space; 4 machines, 2 observations for each machine
		assertTrue(act_space_dim ==1);      //action space
		
		// check the environment observation after the initialization 
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		//[1.0, 1.0, 1.0, 1.0, 0.6530790652351373, 0.45873956913178054, 0.5579368433471502, 0.9172826085024869]

		
		//run one environmentStep without action
		
		double stepTimeInSec = 0.1;
		app.nextStepDynSim(stepTimeInSec, null, null);
		
		
		// check the internal dynamic simulation time
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		assertTrue(NumericUtil.equals(app.getDStabAlgo().getSimuTime(),0.1,1.0E-6));
		
		
		// output the observations
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		
		// check the reward, it should be zero
		System.out.println("at 0.1s, no action, reward = "+app.getReward());
		assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
		// run one environmentStep with one brake action
		app.nextStepDynSim(stepTimeInSec, new double[]{1.0}, "discrete");
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		
		//calculate the rewards
		System.out.println("at 0.2s, 1 break action, reward = "+app.getReward());
		assertTrue(NumericUtil.equals(app.getReward(), -2.0018405678866427,1.0E-2));
		
		
		// run one environmentStep without any action
		app.nextStepDynSim(stepTimeInSec, null, null);
		double reward3=app.getReward();
		System.out.println("at 0.3s, no action, reward = "+reward3);
		assertTrue(NumericUtil.equals(reward3, -7.751666150461745E-4,1.0E-2));
		
		// run one environmentStep with one brake action
		app.nextStepDynSim(stepTimeInSec,new double[]{1.0}, "discrete");
		double reward4=app.getReward();
		System.out.println("at 0.4s, 1 brake action, reward = "+reward4);
		assertTrue(NumericUtil.equals(reward4, -2.001485316055762,1.0E-2));
		
		
		// run one environmentStep without any action
		app.nextStepDynSim(stepTimeInSec, null, null);
		double reward5=app.getReward();
		System.out.println("at 0.4s, 1 brake action, reward = "+reward5);
		assertTrue(NumericUtil.equals(reward5, -0.0011544383819446224,1.0E-2));
		
		// output the voltage to check if actions are applied consecutively in the above two steps
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		
		// check the rewards
		
		
		// check reset
		app.reset(0,0,0,0);
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		// check observations array conversion in the byte array form and and re-conversion
		
		double[][] observations =app.getEnvObservationsDbl2DAry();
		System.out.println("Observations = "+Arrays.toString(observations[0]));
		
		java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(app.getEnvObservationsByte1DAry());
		buf.order(ByteOrder.LITTLE_ENDIAN);
		
	    int n = buf.getInt(), m = buf.getInt();
	    System.out.println("n, m = "+n+","+m);
	    
	    double[][] matrix = new double[n][m];
	    for (int i = 0; i < n; ++i)
	         for (int j = 0; j < m; ++j)
	            matrix[i][j] = buf.getDouble();
     
	    System.out.println("Converted Observations = "+Arrays.toString(matrix[0]));
	    
	    for (int i = 0; i < n; ++i)
	        assertArrayEquals(observations[i], matrix[i], 1.0E-6);
	    
	    
		// System generation info
	    
	    System.out.println("Generation real power ary = "+Arrays.toString(app.getGenerationPAry()));
	    System.out.println("Area 1 Generation real power ary = "+Arrays.toString(app.getGenerationPByAreaAry(1)));
	    
	    
	    // System load info
	    System.out.println("Load real power ary = " + Arrays.toString(app.getLoadPAry()));
	    
	    System.out.println("Area 1 load real power ary = " + Arrays.toString(app.getLoadPByAreaAry(1)));
	    
	    // system adjacency matrix
	    System.out.println("Adjacency Matrix = "+ MatrixUtil.toString(app.getAdjacencyMatrix()));
	    
	    // system branch status
	    
	    app.setBranchStatus(5,6,"1",0);
	    System.out.println("turned off branch 5_6(1) status = "+ app.getDStabAlgo().getNetwork().getBranch("Bus5", "Bus6", "1").isActive());
	   
	    app.setBranchStatus(5,6,"1",1);
	    System.out.println("turned on branch 5_6(1) status = "+ app.getDStabAlgo().getNetwork().getBranch("Bus5", "Bus6", "1").isActive());
	}
	
	//@Test
	public void testRL_basic_function_multiStepObs() {
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\Kundur-2area\\kunder_2area_ver30.raw",
				"testData\\Kundur-2area\\kunder_2area_full_tgov1.dyr"
				};
		
		String dynSimConfigFile = "testData\\Kundur-2area\\json\\kundur2area_baseline_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\Kundur-2area\\json\\kundur2area_RL_config_multiStepObsv.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			assertTrue(ob_act_space_dim[0]==4);
			assertTrue(ob_act_space_dim[1]==8);
			assertTrue(ob_act_space_dim[2]==1);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		assertTrue(obs_ary.length == 4);
		assertTrue(obs_ary[0].length == 8);
		
		System.out.println("\n\\ninit observation array");
		System.out.println(Arrays.toString(obs_ary[0]));
		System.out.println(Arrays.toString(obs_ary[1]));
		System.out.println(Arrays.toString(obs_ary[2]));
		System.out.println(Arrays.toString(obs_ary[3]));
		
		app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
		
		obs_ary = app.getEnvObservationsDbl2DAry();
		System.out.println("\n\nobservation array after 1 step with action");
		System.out.println(Arrays.toString(obs_ary[0]));
		System.out.println(Arrays.toString(obs_ary[1]));
		System.out.println(Arrays.toString(obs_ary[2]));
		System.out.println(Arrays.toString(obs_ary[3]));
		
		app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
		
		obs_ary = app.getEnvObservationsDbl2DAry();
		System.out.println("\n\nobservation array after 2 steps with action");
		System.out.println(Arrays.toString(obs_ary[0]));
		System.out.println(Arrays.toString(obs_ary[1]));
		System.out.println(Arrays.toString(obs_ary[2]));
		System.out.println(Arrays.toString(obs_ary[3]));
		
		
		app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
		
		obs_ary = app.getEnvObservationsDbl2DAry();
		System.out.println("\n\nobservation array after 3 steps with action");
		System.out.println(Arrays.toString(obs_ary[0]));
		System.out.println(Arrays.toString(obs_ary[1]));
		System.out.println(Arrays.toString(obs_ary[2]));
		System.out.println(Arrays.toString(obs_ary[3]));
		
		
	    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
		
		obs_ary = app.getEnvObservationsDbl2DAry();
		System.out.println("\n\nobservation array after 4 steps with action");
		System.out.println(Arrays.toString(obs_ary[0]));
		System.out.println(Arrays.toString(obs_ary[1]));
		System.out.println(Arrays.toString(obs_ary[2]));
		System.out.println(Arrays.toString(obs_ary[3]));
		
	    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
		
		obs_ary = app.getEnvObservationsDbl2DAry();
		System.out.println("\n\nobservation array after 5 steps with action");
		System.out.println(Arrays.toString(obs_ary[0]));
		System.out.println(Arrays.toString(obs_ary[1]));
		System.out.println(Arrays.toString(obs_ary[2]));
		System.out.println(Arrays.toString(obs_ary[3]));
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			if(app.getDStabAlgo().getSimuTime()>1.55 && app.getDStabAlgo().getSimuTime()<2.5)
			    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
			    
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			app.getReward();
			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
	}
	
	
	//@Test
	public void test_loadShedding_function() {
		IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_4motorw_4AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//check environment name
		assertTrue(app.getRLConfigBean().environmentName.equalsIgnoreCase("IEEE39_FIDVR_LoadShedding"));
		
		
		// check whether the action space and the observation space are created, and their dimensions
		assertTrue(ob_act_space_dim!=null);
		
		int ob_space_dim = ob_act_space_dim[1];
		int act_space_dim = ob_act_space_dim[2];
		
		System.out.println("ob_act_space_dim: " +Arrays.toString(ob_act_space_dim));
//		assertTrue(ob_space_dim ==8*2*1);   //observation space; 8 buses, 2 observations for each machine
//		assertTrue(act_space_dim ==1);      //action space
		
		// check the environment observation after the initialization 
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		//[1.0, 1.0, 1.0, 1.0, 0.6530790652351373, 0.45873956913178054, 0.5579368433471502, 0.9172826085024869]

		
		//run one environmentStep without action
		
		double stepTimeInSec = 0.1;
		app.nextStepDynSim(stepTimeInSec, null, null);
		
		
		// check the internal dynamic simulation time
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		//assertTrue(NumericUtil.equals(app.getDStabAlgo().getSimuTime(),0.1,1.0E-6));
		
		
		// output the observations
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		
		// check the reward, it should be zero
		System.out.println("at 0.1s, no action, reward = "+app.getReward());
		//assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
		// run one environmentStep with one brake action
		app.nextStepDynSim(stepTimeInSec, new double[]{1.0,0,0,0}, "discrete");
		//System.out.println(Arrays.toString(app.getEnvironmentObversations()));
		
		//calculate the rewards
		System.out.println("at 0.2s, 1 break action, reward = "+app.getReward());
		//assertTrue(NumericUtil.equals(app.getReward(), -2.0018405678866427,1.0E-2));
		
		
		// run one environmentStep without any action
		app.nextStepDynSim(stepTimeInSec, null, null);
		double reward3=app.getReward();
		System.out.println("at 0.3s, no action, reward = "+reward3);
		//assertTrue(NumericUtil.equals(reward3, -7.751666150461745E-4,1.0E-2));
		
		// run one environmentStep with one brake action
		app.nextStepDynSim(stepTimeInSec,new double[]{1.0,1.0,1.0,1.0}, "discrete");
		double reward4=app.getReward();
		System.out.println("at 0.4s, 1 brake action, reward = "+reward4);
		//assertTrue(NumericUtil.equals(reward4, -2.001485316055762,1.0E-2));
		
		
		// run one environmentStep without any action
		app.nextStepDynSim(stepTimeInSec, null, null);
		double reward5=app.getReward();
		System.out.println("at 0.4s, 1 brake action, reward = "+reward5);
		//assertTrue(NumericUtil.equals(reward5, -0.0011544383819446224,1.0E-2));
		
		// output the voltage to check if actions are applied consecutively in the above two steps
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		
		// check the rewards
		
		
		// check reset
		app.reset(0,0,0,0);
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
	}
	
	//@Test
	public void test_loadShedding_continuous_function() {
		IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_continuous.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//check environment name
		assertTrue(app.getRLConfigBean().environmentName.equalsIgnoreCase("IEEE39_FIDVR_LoadShedding_Continuous_Action"));
		
		
		// check whether the action space and the observation space are created, and their dimensions
		assertTrue(ob_act_space_dim!=null);
		
		int ob_space_dim = ob_act_space_dim[1];
		int act_space_dim = ob_act_space_dim[2];
		
		System.out.println("ob_act_space_dim: " +Arrays.toString(ob_act_space_dim));
//		assertTrue(ob_space_dim ==8*2*1);   //observation space; 8 buses, 2 observations for each machine
//		assertTrue(act_space_dim ==1);      //action space
		
		// check the environment observation after the initialization 
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		//[1.0, 1.0, 1.0, 1.0, 0.6530790652351373, 0.45873956913178054, 0.5579368433471502, 0.9172826085024869]

		
		//run one environmentStep without action
		
		double stepTimeInSec = 0.1;
		app.nextStepDynSim(stepTimeInSec, new double[]{0,0,0}, "continuous");
		
		
		// check the internal dynamic simulation time
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		//assertTrue(NumericUtil.equals(app.getDStabAlgo().getSimuTime(),0.1,1.0E-6));
		
		
		// output the observations
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		
		// check the reward, it should be zero
		System.out.println("at 0.1s, no action, reward = "+app.getReward());
		//assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
		// run one environmentStep with one brake action
		app.nextStepDynSim(stepTimeInSec, new double[]{-1.0,0,0}, "continuous");
		//System.out.println(Arrays.toString(app.getEnvironmentObversations()));
		
		//calculate the rewards
		System.out.println("at 0.2s, 1 pre-fault loadshed action, reward = "+app.getReward());
		//assertTrue(NumericUtil.equals(app.getReward(), -2.0018405678866427,1.0E-2));
		
		
		// run one environmentStep without any action
		for(int i = 0;i<10;i++) {
			System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
			app.nextStepDynSim(stepTimeInSec, new double[]{0.0,0,0}, "continuous");
			double reward3=app.getReward();
			System.out.println("no action, reward = "+reward3);
			//assertTrue(NumericUtil.equals(reward3, -7.751666150461745E-4,1.0E-2));
		}
		
		// run one environmentStep with one brake action
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec,new double[]{0,-1.0,0}, "continuous");
		double reward4=app.getReward();
		System.out.println(" 1 post-fault loadshed action, reward = "+reward4);
		//assertTrue(NumericUtil.equals(reward4, -2.001485316055762,1.0E-2));
		
		
		// run one environmentStep with invalid action
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec, new double[]{0.0,-0.5,0}, "continuous");
		double reward5=app.getReward();
		System.out.println(" One invalid action, reward = "+reward5);
		//assertTrue(NumericUtil.equals(reward5, -0.0011544383819446224,1.0E-2));
		
		
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec, new double[]{0.0,0.0,-1.0}, "continuous");
		double reward6=app.getReward();
		System.out.println(" One loadshed action, reward = "+reward6);
		//assertTrue(NumericUtil.equals(reward5, -0.0011544383819446224,1.0E-2));
		
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec, new double[]{0.0,0.0,-1.0E-3}, "continuous");
		double reward7=app.getReward();
		System.out.println(" One loadshed action, reward = "+reward7);
		
		// output the voltage to check if actions are applied consecutively in the above two steps
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		
		// check the rewards
		
		
		// check reset
		app.reset(0,0,0,0);
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		System.out.print("reward after reset = "+app.getReward());
		assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
	}
	
	
	@Test
	public void test_loadShedding_continuous_function_usingConfigFiles() {
		IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = null;
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_continuous.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//check environment name
		assertTrue(app.getRLConfigBean().environmentName.equalsIgnoreCase("IEEE39_FIDVR_LoadShedding_Continuous_Action"));
		
		
		// check whether the action space and the observation space are created, and their dimensions
		assertTrue(ob_act_space_dim!=null);
		
		int ob_space_dim = ob_act_space_dim[1];
		int act_space_dim = ob_act_space_dim[2];
		
		System.out.println("ob_act_space_dim: " +Arrays.toString(ob_act_space_dim));
//		assertTrue(ob_space_dim ==8*2*1);   //observation space; 8 buses, 2 observations for each machine
//		assertTrue(act_space_dim ==1);      //action space
		
		// check the environment observation after the initialization 
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		//[1.0, 1.0, 1.0, 1.0, 0.6530790652351373, 0.45873956913178054, 0.5579368433471502, 0.9172826085024869]

		System.out.println("load bus&id within action scope: ");
		System.out.println(Arrays.toString(app.getLoadIdWithinActionScope()));
		System.out.println("load power within action scope: ");
		System.out.println(Arrays.toString(app.getLoadPWithinActionScope()));
		
		System.out.println(Arrays.toString(app.getLoadIdWithinActionScope()));
		
		//run one environmentStep without action
		
		double stepTimeInSec = 0.1;
		app.nextStepDynSim(stepTimeInSec, new double[]{0,0,0}, "continuous");
		
		
		// check the internal dynamic simulation time
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		//assertTrue(NumericUtil.equals(app.getDStabAlgo().getSimuTime(),0.1,1.0E-6));
		
		
		// output the observations
		System.out.println(Arrays.toString(app.getEnvObservationsDbl2DAry()));
		
		// check the reward, it should be zero
		System.out.println("at 0.1s, no action, reward = "+app.getReward());
		assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
		// run one environmentStep with one brake action
		app.nextStepDynSim(stepTimeInSec, new double[]{-1.0,0,0}, "continuous");
		//System.out.println(Arrays.toString(app.getEnvironmentObversations()));
		
		//calculate the rewards
		System.out.println("at 0.2s, 1 pre-fault loadshed action, reward = "+app.getReward());
		assertTrue(NumericUtil.equals(app.getReward(), -100.0,1.0E-2));
		
		
		// run one environmentStep without any action
		for(int i = 0;i<10;i++) {
			System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
			app.nextStepDynSim(stepTimeInSec, new double[]{0.0,0,0}, "continuous");
			double reward3=app.getReward();
			System.out.println("no action, reward = "+reward3);
			assertTrue(NumericUtil.equals(reward3, 0.0,1.0E-2));
		}
		
		// run one environmentStep with one brake action
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec,new double[]{0,-1.0,0}, "continuous");
		double reward4=app.getReward();
		System.out.println(" 1 post-fault loadshed action, reward = "+reward4);
		assertTrue(NumericUtil.equals(reward4, -350.7,1.0E-2));
		
		
		// run one environmentStep with invalid action
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec, new double[]{0.0,-0.5,0}, "continuous");
		double reward5=app.getReward();
		System.out.println(" One invalid action, reward = "+reward5);
		assertTrue(NumericUtil.equals(reward5, -3.0,1.0E-2));
		
		
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec, new double[]{0.0,0.0,-1.0}, "continuous");
		double reward6=app.getReward();
		System.out.println(" One loadshed action, reward = "+reward6);
		assertTrue(NumericUtil.equals(reward6, -244.8,1.0E-1));
		
		System.out.println("sim time = "+app.getDStabAlgo().getSimuTime());
		app.nextStepDynSim(stepTimeInSec, new double[]{0.0,0.0,-1.0E-3}, "continuous");
		double reward7=app.getReward();
		// Invalid action as all load on the bus 518 has been shed in the last step
		System.out.println(" One loadshed action, reward = "+reward7);
		assertTrue(NumericUtil.equals(reward7, -3.0,1.0E-1));  
		
		// output the voltage to check if actions are applied consecutively in the above two steps
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		
		// check the rewards
		
		
		// check reset
		app.reset(0,0,0,0);
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		System.out.print("reward after reset = "+app.getReward());
		assertTrue(NumericUtil.equals(app.getReward(), 0,1.0E-6));
		
		
	}
	

	
	//@Test
	public void test_divide() {
		double a = 0.1, b =0.005;
		System.out.println(Math.round(a/b));
		
		int c = 1, d =4;
		
		System.out.println(c%d);
	}
	
	

}
