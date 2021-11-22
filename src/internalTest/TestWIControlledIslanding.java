package internalTest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.interpss.util.FileUtil;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.common.util.IpssLogger;

public class TestWIControlledIslanding {
	
	//@Test
	public void test_WI_ControlledIslanding_baseline_discrete() {
		IpssLogger.getLogger().setLevel(Level.ALL);
	    IpssPyGateway app = new IpssPyGateway();
		
	    String folder ="C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\testData\\wecc_19";
		String[] caseFiles = new String[]{
				
				//"testData/wecc_19/pfcase_07_25_18_v30_pss34_psse23_gridpack_mod_v30.raw",
				folder+"\\pfcase_07_25_18_v30_gridpack_mod_removed_BC_AESO.RAW",
				//"testData/wecc_19/19hsp1ap_v30.raw",
				//"testData/wecc_19/pfcase_07_25_18_v30_pss34_psse23_gridpack_mod_h4_protection.dyr"
				//"testData/wecc_19/pfcase_07_25_18_v30_pss34_psse23_gridpack_mod_protection.dyr"
				
				//"testData/wecc_19/19hsp1ap.dyr",
				folder+"\\compete_gen_exc_gov_generic_protection_v1.dyr"
				//"testData/wecc_19/compete_gen_exc_generic_dyn.dyr"
				//"testData/wecc_19/compete_gen_dyn.dyr"
				};
		
		String dynSimConfigFile = folder+"\\json\\wecc19_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = folder+"\\json\\wecc19_RL_controlledIslanding_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("Obs names:\n"+Arrays.toString(app.getEnvObservationNames()));
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
			assertTrue(ob_act_space_dim[0]==1); // no historical data
			assertTrue(ob_act_space_dim[1]==(88+2)); // 88 bus freq, 1 totalGenTripPower, 1 totalLoadShedPower
			assertTrue(ob_act_space_dim[2]==1);
			assertTrue(ob_act_space_dim[3]==2);
			
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		
		
		app.reset(0, 0, app.getFaultStartTimeCandidates()[0], app.getFaultDurationCandidates()[1],0);
		
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			
			if(app.getDStabAlgo().getSimuTime()>0.89 && app.getDStabAlgo().getSimuTime()<0.91){
			    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete"); // apply load shedding action to bus 504
			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			
			app.getReward();
			System.out.println(Arrays.toString(app.getEnvObservationsDbl1DAry()));
			
			if(app.isSimulationDone())
				break;
//			
//			if(app.getDStabAlgo().getSimuTime()>20.0)
//			    break;
//			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
        FileUtil.writeText2File("C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\output\\mach_angle_RLGC_test_NoCI.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
        FileUtil.writeText2File("C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\output\\busVolt_RLGC_test_NoCI.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
        FileUtil.writeText2File("C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\output\\busFreq_RLGC_test_NoCI.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getBusFreqTable()));
		System.out.println("total rewards ="+app.getTotalRewards());
		System.out.println("step reward info: \n"+app.getStepRewardInfo());
		
		
		//System.out.println(Arrays.toString(app.getBaseCases()));
		
	}
	
	@Test
	public void test_WI_ControlledIslanding_smallCont_noCI() {
		IpssLogger.getLogger().setLevel(Level.SEVERE);
	    IpssPyGateway app = new IpssPyGateway();
	    app.setLoggerLevel(1);
	    
	    String folder ="C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\testData\\wecc_19";
		String[] caseFiles = new String[]{
				
				//"testData/wecc_19/pfcase_07_25_18_v30_pss34_psse23_gridpack_mod_v30.raw",
				folder+"\\pfcase_07_25_18_v30_gridpack_mod_removed_BC_AESO_WACM.RAW",
				//"testData/wecc_19/19hsp1ap_v30.raw",
				//"testData/wecc_19/pfcase_07_25_18_v30_pss34_psse23_gridpack_mod_h4_protection.dyr"
				//"testData/wecc_19/pfcase_07_25_18_v30_pss34_psse23_gridpack_mod_protection.dyr"
				
				//"testData/wecc_19/19hsp1ap.dyr",
				folder+"\\compete_gen_exc_gov_generic_protection_v1.3.dyr"
				//"testData/wecc_19/compete_gen_exc_generic_dyn.dyr"
				//folder+"\\compete_gen_dyn.dyr"
				};
		
		String dynSimConfigFile = folder+"\\json\\wecc19_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = folder+"\\json\\wecc19_RL_controlledIslanding_multiCont_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
			
			System.out.println("Obs names:\n"+Arrays.toString(app.getEnvObservationNames()));
			
			System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));
			
			assertTrue(ob_act_space_dim[0]==1); // no historical data
			assertTrue(ob_act_space_dim[1]==(85+2)); // 88 bus freq, 1 totalGenTripPower, 1 totalLoadShedPower
			assertTrue(ob_act_space_dim[2]==1);
			assertTrue(ob_act_space_dim[3]==2);
			
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		
		
		app.reset(0, 0, app.getFaultStartTimeCandidates()[0], app.getFaultDurationCandidates()[0],0);
		
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			
			if(app.getDStabAlgo().getSimuTime()>0.79 && app.getDStabAlgo().getSimuTime()<0.81){
			    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete"); // apply load shedding action to bus 504
			}
//			else if(app.getDStabAlgo().getSimuTime()>0.99 && app.getDStabAlgo().getSimuTime()<1.01){
//			    app.nextStepDynSim(0.1, new double[]{1.0}, "discrete"); // apply load shedding action to bus 504
//			} 
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			
			app.getReward();
			System.out.println(Arrays.toString(app.getEnvObservationsDbl1DAry()));
			
			if(app.isSimulationDone())
				break;
//			
//			if(app.getDStabAlgo().getSimuTime()>20.0)
//			    break;
//			
		}
		
		
		
		//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
//		System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
        FileUtil.writeText2File("C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\output\\mach_angle_RLGC_test_4cycles_noCont_NoCI.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
        FileUtil.writeText2File("C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\output\\busVolt_RLGC_test_4cycles_noCont_NoCI.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
        FileUtil.writeText2File("C:\\Users\\huan289\\git\\Test_WECC_DStab\\Test_DStab\\output\\busFreq_RLGC_test_4cycles_noCont_NoCI.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getBusFreqTable()));
		System.out.println("total rewards ="+app.getTotalRewards());
		System.out.println("step reward info: \n"+app.getStepRewardInfo());
		
		//System.out.println(Arrays.toString(app.getBaseCases()));
		
	}

}
