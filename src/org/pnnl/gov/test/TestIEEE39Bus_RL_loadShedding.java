package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.interpss.IpssCorePlugin;
import org.interpss.dstab.relay.LVSHLoadRelayModel;
import org.interpss.numeric.datatype.Triplet;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.common.util.IpssLogger;
import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabilityNetwork;

public class TestIEEE39Bus_RL_loadShedding {
	
	
	//@Test
	public void test_noAction_BaseLine() throws SecurityException, IOException {
		IpssCorePlugin.init();
	    IpssPyGateway app = new IpssPyGateway();
	    IpssLogger.getLogger().setLevel(Level.ALL);
	    
	    Handler fileHandler  = new FileHandler("./testLogger.log");
	    IpssLogger.getLogger().addHandler(fileHandler);
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_3levels.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
//			app.getDStabAlgo().solveDEqnStep(true);
//			if(app.getDStabAlgo().getSimuTime()>10.0)
//				break;
			app.nextStepDynSim(0.1, new double[]{0.0, 0, 0}, "discrete");
		}
		
//		System.out.println("\n speed = \n"+app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println("\n volt = \n"+app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		//FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
		
	}
	
	@Test
	public void test_IEEE39_RL_baseline_discrete() {
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_3levels.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
			assertTrue(ob_act_space_dim[0]==10);
			assertTrue(ob_act_space_dim[1]==11);
			assertTrue(ob_act_space_dim[2]==3);
			assertTrue(ob_act_space_dim[3]==3);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		app.reset(0, 0, app.getFaultStartTimeCandidates()[0], 0.08);
		
	
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			
			if(app.getDStabAlgo().getSimuTime()>0.25 && app.getDStabAlgo().getSimuTime()<0.75){
			    app.nextStepDynSim(0.1, new double[]{2, 2, 2}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0, 0, 0}, "discrete");
			
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
			if(app.getDStabAlgo().getSimuTime()>10.0)
			    break;
			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
	}
	
	//@Test
	public void test_IEEE39_RL_baseline_continuous() {
		IpssLogger.getLogger().setLevel(Level.ALL);
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
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
			assertTrue(ob_act_space_dim[0]==4);
			assertTrue(ob_act_space_dim[1]==16);
			assertTrue(ob_act_space_dim[2]==4);
			assertTrue(ob_act_space_dim[3]==8);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		app.reset(0, 0, 0.05, 0.08);
		double[][] obs_ary = app.getEnvObservations();
		assertTrue(obs_ary.length == 4);
		assertTrue(obs_ary[0].length == 16);
		System.out.println(Arrays.toString(obs_ary[0]));
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			
			if(app.getDStabAlgo().getSimuTime()>1.55 && app.getDStabAlgo().getSimuTime()<1.85){
			    app.nextStepDynSim(0.1, new double[]{2, 2, 2, 2}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0, 0, 0, 0}, "discrete");
			
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
			if(app.getDStabAlgo().getSimuTime()>10.0)
			    break;
			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
	}
	
	//@Test
	public void test_IEEE39_RL_1motor_4actionLevels() {
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_1AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_1motor_4levels.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
			assertTrue(ob_act_space_dim[0]==4);
			assertTrue(ob_act_space_dim[1]==5);
			assertTrue(ob_act_space_dim[2]==1);
			assertTrue(ob_act_space_dim[3]==4);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		double[][] obs_ary = app.getEnvObservations();
		assertTrue(obs_ary.length == 4);
		assertTrue(obs_ary[0].length == 5);
		
		System.out.println(Arrays.toString(obs_ary[0]));
		
		//reset to mimic the case
		app.reset(0, 3, 8.0, 0.08);
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			
			if(app.getDStabAlgo().getSimuTime()>1.1 && app.getDStabAlgo().getSimuTime()<1.3){
			    app.nextStepDynSim(0.1, new double[]{0.0}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
//			if(app.getDStabAlgo().getSimuTime()>10.0)
//			    break;
			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
	}
	
	//@Test
	public void test_IEEE39_RL_3motor_3actionLevels() {
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_3levels.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
//			assertTrue(ob_act_space_dim[0]==4);
//			assertTrue(ob_act_space_dim[1]==5);
//			assertTrue(ob_act_space_dim[2]==1);
//			assertTrue(ob_act_space_dim[3]==4);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
//		for(int i=0;i<10000;i++) {
			//reset to mimic the case
			app.reset(0, 3, 0.05, 0.08);
			
			
			double[][] obs_ary = app.getEnvObservations();
	//		assertTrue(obs_ary.length == 4);
	//		assertTrue(obs_ary[0].length == 5);
			
			System.out.println(Arrays.toString(obs_ary[0]));
			
	
			
			while(!app.isSimulationDone()) {
				
				if(app.getDStabAlgo().getSimuTime()>1.09 && app.getDStabAlgo().getSimuTime()<1.31){
				    app.nextStepDynSim(0.1, new double[]{0,2,0}, "discrete"); // apply load shedding action to bus 504
				}
				else if(app.getDStabAlgo().getSimuTime()>0.09 && app.getDStabAlgo().getSimuTime()<0.5){
				    app.nextStepDynSim(0.1, new double[]{0,1,1}, "discrete"); // apply load shedding action to bus 504
				} 
				else
					app.nextStepDynSim(0.1, new double[]{0.0,0.0,0.0}, "discrete");
				
				
				app.getReward();
						
				if(app.getDStabAlgo().getSimuTime()<0.30)
				    System.out.println(Arrays.deepToString(app.getEnvObservations()));
				
			}
//		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
	}
	
	//@Test
	public void test_IEEE39_RL_3motor_3actionLevels_80Loading() {
		
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30_80.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_3levels.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
//			assertTrue(ob_act_space_dim[0]==4);
//			assertTrue(ob_act_space_dim[1]==5);
//			assertTrue(ob_act_space_dim[2]==1);
//			assertTrue(ob_act_space_dim[3]==4);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//reset to mimic the case
		app.reset(0, 3, 0.05, 0.08);
		
		
		double[][] obs_ary = app.getEnvObservations();
//		assertTrue(obs_ary.length == 4);
//		assertTrue(obs_ary[0].length == 5);
		
//		System.out.println(Arrays.toString(obs_ary[0]));
		

		
		while(!app.isSimulationDone()) {
			
			if(app.getDStabAlgo().getSimuTime()>0.09 && app.getDStabAlgo().getSimuTime()<0.11){
			    app.nextStepDynSim(0.1, new double[]{0,1,1}, "discrete"); // apply load shedding action to bus 504
			}
			else if(app.getDStabAlgo().getSimuTime()>0.19 && app.getDStabAlgo().getSimuTime()<0.41){
			    app.nextStepDynSim(0.1, new double[]{0,1,0}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0,0.0,0.0}, "discrete");
			
			
			app.getReward();
					
//			if(app.getDStabAlgo().getSimuTime()<0.30)
//			    System.out.println(Arrays.deepToString(app.getEnvironmentObversations()));
//			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
	}
	
	//@Test
	public void test_IEEE39_RL_3motor_3actionLevels_UVLS() {
		
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30_80.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_3levels.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
//			assertTrue(ob_act_space_dim[0]==4);
//			assertTrue(ob_act_space_dim[1]==5);
//			assertTrue(ob_act_space_dim[2]==1);
//			assertTrue(ob_act_space_dim[3]==4);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//reset to mimic the case
		app.reset(0, 3, 0.05, 0.08);
		
		
		double[][] obs_ary = app.getEnvObservations();
//		assertTrue(obs_ary.length == 4);
//		assertTrue(obs_ary[0].length == 5);
		
//		System.out.println(Arrays.toString(obs_ary[0]));
		
		DStabilityNetwork dsNet =(DStabilityNetwork) app.getDStabAlgo().getNetwork();
		
		DStabBus bus5 = dsNet.getBus("Bus5");
		
	    LVSHLoadRelayModel lvsh = new LVSHLoadRelayModel(bus5, "1");
	    
	    //Triplet <voltage, time, fraction>
	    Triplet vtf1 = new Triplet(0.8, 0.15,0.2);
	    Triplet vtf2 = new Triplet(0.75, 0.1,0.3);
	    List<Triplet> settings= new ArrayList<>();
	    settings.add(vtf1);
	    settings.add(vtf2);
	  
	    lvsh.setRelaySetPoints(settings);
	    
	    app.getDStabAlgo().getNetwork().getRelayModelList().add(lvsh);
	

		
		while(!app.isSimulationDone()) {
			
			if(app.getDStabAlgo().getSimuTime()>0.09 && app.getDStabAlgo().getSimuTime()<0.11){
			    app.nextStepDynSim(0.1, new double[]{0,1,1}, "discrete"); // apply load shedding action to bus 504
			}
			else if(app.getDStabAlgo().getSimuTime()>0.19 && app.getDStabAlgo().getSimuTime()<0.41){
			    app.nextStepDynSim(0.1, new double[]{0,1,0}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0,0.0,0.0}, "discrete");
			
			
			app.getReward();
					
//			if(app.getDStabAlgo().getSimuTime()<0.30)
//			    System.out.println(Arrays.deepToString(app.getEnvironmentObversations()));
//			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		assertTrue(Math.abs(app.getTotalRewards()+131.808)<1.0e-3);
	}
	


}
