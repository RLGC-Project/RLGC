package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.interpss.dstab.relay.impl.LoadUFShedRelayModel;
import org.interpss.dstab.relay.impl.LoadUVShedRelayModel;
import org.interpss.numeric.datatype.Triplet;
import org.interpss.util.FileUtil;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabilityNetwork;

public class TestKundur2Area_RL_ControlledIslanding {
	
	@Test
	public void testManualSetting() {
	    IpssPyGateway app = new IpssPyGateway();
		
		String[] caseFiles = new String[]{
				"testData\\Kundur-2area\\kunder_2area_ver30.raw",
				"testData\\Kundur-2area\\kunder_2area_full_tgov1.dyr"
				};
		
		String dynSimConfigFile = "testData\\Kundur-2area\\json\\kundur2area_baseline_dyn_config.json"; // define dynamic simulation and monitoring
		String rlConfigJsonFile = "testData\\Kundur-2area\\json\\kundur2area_RL_controlledIslanding_config.json";
		
		int[] ob_act_space_dim = null;
		
		try {
			ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile,false);
			
			assertTrue(ob_act_space_dim[0]==1);
			assertTrue(ob_act_space_dim[1]==9); // 7 non-gen bus freq + 1 total gen tripped power + 1 total load shed power
			assertTrue(ob_act_space_dim[2]==1);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		

		 DStabilityNetwork dsNet = (DStabilityNetwork) app.getDStabAlgo().getNetwork();
		
		/*
		 * ---------------------add protection models----------------------------
		 */
		//add the LVSH load shedding model
	    DStabBus bus7 = dsNet.getBus("Bus7");
	    
	    LoadUVShedRelayModel uvls_bus7 = new LoadUVShedRelayModel(bus7, "1");
	    
	    //Triplet <voltage, time, fraction>
	    Triplet vtf1 = new Triplet(0.6, 0.2,0.1);
	    Triplet vtf2 = new Triplet(0.8, 0.3,0.3);
	    List<Triplet> settings= new ArrayList<>();
	    settings.add(vtf1);
	    settings.add(vtf2);
	  
	    uvls_bus7.setRelaySetPoints(settings);
	    
	    //add the under frequency load shedding model

	    LoadUFShedRelayModel ufls_bus7 = new LoadUFShedRelayModel(bus7, "1");
	    
	    //Triplet <freq, time, fraction>
	    Triplet ftf1 = new Triplet(0.98, 0.05,0.3);
	    Triplet ftf2 = new Triplet(0.96, 0.05,0.3);
	    List<Triplet> settings2= new ArrayList<>();
	    settings2.add(ftf1);
	    settings2.add(ftf2);
	  
	    ufls_bus7.setRelaySetPoints(settings2);
	    
	   
	    DStabBus bus9 = dsNet.getBus("Bus9");
	   
	    LoadUVShedRelayModel uvls_bus9 = new LoadUVShedRelayModel(bus9, "1");
	    
//	    //Triplet <voltage, time, fraction>
//	    Triplet vtf1 = new Triplet(0.6, 0.05,0.1);
//	    Triplet vtf2 = new Triplet(0.8, 0.16,0.3);
//	    List<Triplet> settings= new ArrayList<>();
//	    settings.add(vtf1);
//	    settings.add(vtf2);
	  
	    uvls_bus9.setRelaySetPoints(settings);
	    
	    //add the under frequency load shedding model

	    LoadUFShedRelayModel ufls_bus9 = new LoadUFShedRelayModel(bus9, "1");
	    
	    //Triplet <freq, time, fraction>
//	    Triplet ftf1 = new Triplet(0.995, 0.05,0.5);
//	    Triplet ftf2 = new Triplet(0.98, 0.05,0.3);
//	    List<Triplet> settings2= new ArrayList<>();
//	    settings2.add(ftf1);
//	    settings2.add(ftf2);
	  
	    ufls_bus9.setRelaySetPoints(settings2);
	   
	    
	   //app.getDStabAlgo().initialization();
		
		
		double[][] obs_ary = app.getEnvObservationsDbl2DAry();
		assertTrue(obs_ary.length == 1);
		assertTrue(obs_ary[0].length == 9);
		System.out.println(Arrays.toString(obs_ary[0]));
		
		app.reset(0, 0, app.getFaultStartTimeCandidates()[0], 0.30);
		
		while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
			if(app.getDStabAlgo().getSimuTime()>1.55 && app.getDStabAlgo().getSimuTime()<1.5)
			    app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			    
			else
				app.nextStepDynSim(0.1, new double[]{0.0}, "discrete");
			
			
			//System.out.println("observations =\n"+Arrays.toString(app.getEnvObservationsDbl1DAry()));
			System.out.println("reward = "+app.getReward());
			
			if(app.isSimulationDone())
				break;
			
		}
		
		
		System.out.println("bus volt =\n"+app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
		System.out.println("bus freq =\n"+app.getStateMonitor().toCSVString(app.getStateMonitor().getBusFreqTable()));
		System.out.println("mach speed =\n"+app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
		System.out.println("mach angle =\n"+app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
//		FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
	
		
		System.out.println("total rewards ="+app.getTotalRewards());
	}

}
