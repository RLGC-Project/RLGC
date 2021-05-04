package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.common.util.IpssLogger;

public class TestIEEE39Bus_RL_GeneratorTripping {
	
	@Test
	public void test_IEEE39_RL_GenTrip_baseline_discrete() {
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\IEEE39\\IEEE39bus_multiloads_xfmr4_smallX_v30.raw",
				"testData\\IEEE39\\IEEE39bus_3AC.dyr"//IEEE39bus.dyr"
				};
		
		String dynSimConfigFile = "testData\\IEEE39\\json\\IEEE39_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\IEEE39\\json\\IEEE39_RL_generatorTripping_3gen.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("Obs names:\n"+Arrays.toString(app.getEnvObservationNames()));
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
			assertTrue(ob_act_space_dim[0]==1);
			assertTrue(ob_act_space_dim[1]==3*10);
			assertTrue(ob_act_space_dim[2]==3);
			assertTrue(ob_act_space_dim[3]==2);
			
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		
		
		app.reset(0, 0, app.getFaultStartTimeCandidates()[0], 0.15);
		
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			
			if(app.getDStabAlgo().getSimuTime()>1.4 && app.getDStabAlgo().getSimuTime()<1.55){
			    app.nextStepDynSim(0.1, new double[]{1, 1, 1}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0,0,0}, "discrete");
			
			
			app.getReward();
			System.out.println(Arrays.toString(app.getEnvObservationsDbl1DAry()));
			
			if(app.isSimulationDone())
				break;
			
			if(app.getDStabAlgo().getSimuTime()>10.0)
			    break;
			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		System.out.println("total rewards ="+app.getTotalRewards());
		
		System.out.println(Arrays.toString(app.getBaseCases()));
		
	}

}
