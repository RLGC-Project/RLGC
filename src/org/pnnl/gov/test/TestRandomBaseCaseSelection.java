package org.pnnl.gov.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.common.util.IpssLogger;

public class TestRandomBaseCaseSelection {
	
	@Test
	public void test_with_IEEE39_RL() {
		
		
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
			//ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile,"C:\\Users\\huan289\\git\\RLGC");
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			

			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		 //reset to different case Idx
			app.reset(7, 3, 0.05, 0.08);
			
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
				    System.out.println(Arrays.deepToString(app.getEnvObservationsDbl2DAry()));
				
			}

	}

}
