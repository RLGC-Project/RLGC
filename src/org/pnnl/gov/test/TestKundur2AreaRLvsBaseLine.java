package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.interpss.util.FileUtil;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabilityNetwork;

public class TestKundur2AreaRLvsBaseLine {
	
	@Test
	public void test_noAction_BaseLine() {
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
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			app.getDStabAlgo().solveDEqnStep(true);
		}
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		
	}
	
	//@Test
	public void testRL_baseline() {
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\Kundur-2area\\kunder_2area_ver30.raw",
				"testData\\Kundur-2area\\kunder_2area_full_tgov1.dyr"
				};
		
		String dynSimConfigFile = "testData\\Kundur-2area\\json\\kundur2area_baseline_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\Kundur-2area\\json\\kundur2area_RL_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			assertTrue(ob_act_space_dim[0]==1);
			assertTrue(ob_act_space_dim[1]==8);
			assertTrue(ob_act_space_dim[2]==1);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		assertTrue(obs_ary.length == 1);
		assertTrue(obs_ary[0].length == 8);
		System.out.println(Arrays.toString(obs_ary[0]));
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			if(app.getDStabAlgo().getSimuTime()>1.55 && app.getDStabAlgo().getSimuTime()<2.5)
			    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
			    
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
	}
	
	
	
	
	//@Test
	public void testRL_baseline_UVLS() {
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\Kundur-2area\\kunder_2area_ver30.raw",
				"testData\\Kundur-2area\\kunder_2area_full_tgov1.dyr"
				};
		
		String dynSimConfigFile = "testData\\Kundur-2area\\json\\kundur2area_baseline_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\Kundur-2area\\json\\kundur2area_RL_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			assertTrue(ob_act_space_dim[0]==1);
			assertTrue(ob_act_space_dim[1]==8);
			assertTrue(ob_act_space_dim[2]==1);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		assertTrue(obs_ary.length == 1);
		assertTrue(obs_ary[0].length == 8);
		System.out.println(Arrays.toString(obs_ary[0]));
		
		
		DStabilityNetwork dsNet =(DStabilityNetwork) app.getDStabAlgo().getNetwork();
		
		DStabBus bus5 = dsNet.getBus("Bus5");
		
//		  LVSHLoadRelayModel lvsh = new LVSHLoadRelayModel(bus5, "1");
//		    
//		    //Triplet <voltage, time, fraction>
//		    Triplet vtf1 = new Triplet(0.8, 0.15,0.2);
//		    Triplet vtf2 = new Triplet(0.75, 0.1,0.3);
//		    List<Triplet> settings= new ArrayList<>();
//		    settings.add(vtf1);
//		    settings.add(vtf2);
//		  
//		    lvsh.setRelaySetPoints(settings);
//		    
//		    app.getDStabAlgo().getNetwork().getRelayModelList().add(lvsh);
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			if(app.getDStabAlgo().getSimuTime()>1.55 && app.getDStabAlgo().getSimuTime()<2.5)
			    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete");
			    
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			
			
			app.getReward();
			
			if(app.isSimulationDone())
				break;
			
		}
		
		
		
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
	}
	


}
