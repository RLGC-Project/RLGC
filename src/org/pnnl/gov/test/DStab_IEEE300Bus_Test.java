package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

import org.apache.commons.math3.complex.Complex;
import org.ieee.odm.adapter.IODMAdapter.NetType;
import org.ieee.odm.adapter.psse.PSSEAdapter;
import org.ieee.odm.adapter.psse.PSSEAdapter.PsseVersion;
import org.ieee.odm.model.dstab.DStabModelParser;
import org.interpss.IpssCorePlugin;
import org.interpss.display.AclfOutFunc;
import org.interpss.mapper.odm.ODMDStabParserMapper;
import org.interpss.numeric.NumericConstant;
import org.interpss.util.FileUtil;
import org.junit.Test;
import org.pnnl.gov.pss_gateway.IpssPyGateway;

import com.interpss.CoreCommonFactory;
import com.interpss.CoreObjectFactory;
import com.interpss.DStabObjectFactory;
import com.interpss.SimuObjectFactory;
import com.interpss.common.exp.InterpssException;
import com.interpss.common.msg.IPSSMsgHub;
import com.interpss.common.util.IpssLogger;
import com.interpss.core.acsc.fault.AcscBusFault;
import com.interpss.core.acsc.fault.SimpleFaultCode;
import com.interpss.core.algo.LoadflowAlgorithm;
import com.interpss.dstab.BaseDStabBus;
import com.interpss.dstab.BaseDStabNetwork;
import com.interpss.dstab.algo.DynamicSimuAlgorithm;
import com.interpss.dstab.algo.DynamicSimuMethod;
import com.interpss.dstab.cache.StateMonitor;
import com.interpss.dstab.cache.StateMonitor.DynDeviceType;
import com.interpss.dstab.devent.DynamicEvent;
import com.interpss.dstab.devent.DynamicEventType;
import com.interpss.simu.SimuContext;
import com.interpss.simu.SimuCtxType;

public class DStab_IEEE300Bus_Test {
		
		//@Test
		public void test_IEEE300_Dstab() throws InterpssException{
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
		    
		    //dsNet.setBypassDataCheck(true);
			DynamicSimuAlgorithm dstabAlgo = simuCtx.getDynSimuAlgorithm();
			LoadflowAlgorithm aclfAlgo = dstabAlgo.getAclfAlgorithm();
			
			aclfAlgo.getDataCheckConfig().setAutoTurnLine2Xfr(true);

			aclfAlgo.getLfAdjAlgo().setApplyAdjustAlgo(false);
			aclfAlgo.setTolerance(1.0E-6);
			assertTrue(aclfAlgo.loadflow());
			System.out.println(AclfOutFunc.loadFlowSummary(dsNet));
			
			dstabAlgo.setSimuMethod(DynamicSimuMethod.MODIFIED_EULER);
			dstabAlgo.setSimuStepSec(0.002);
			dstabAlgo.setTotalSimuTimeSec(5.0);
			//dstabAlgo.setRefMachine(dsNet.getMachine("Bus10030-mach1"));
			

			StateMonitor sm = new StateMonitor();
			sm.addBusStdMonitor(new String[]{"Bus10000","Bus10001","Bus10002","Bus10015","Bus10016","Bus10028"});
			sm.addGeneratorStdMonitor(new String[]{"Bus10002-mach1","Bus10003-mach1","Bus10005-mach1","Bus10008-mach1","Bus10009-mach1"});
	
			
			// set the output handler
			dstabAlgo.setSimuOutputHandler(sm);
			dstabAlgo.setOutPutPerSteps(25);
			//dstabAlgo.setRefMachine(dsNet.getMachine("Bus39-mach1"));
			
			IpssLogger.getLogger().setLevel(Level.INFO);
			
			dsNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent("Bus20",dsNet,SimpleFaultCode.GROUND_3P,new Complex(0,0),null,0.0d,0.08),"3phaseFault@Bus20");
			

			if (dstabAlgo.initialization()) {
				double t1 = System.currentTimeMillis();
				System.out.println("time1="+t1);
				System.out.println("Running DStab simulation ...");
				System.out.println(dsNet.getMachineInitCondition());
				dstabAlgo.performSimulation();
				double t2 = System.currentTimeMillis();
				System.out.println("used time="+(t2-t1)/1000.0);

			}
			System.out.println(sm.toCSVString(sm.getMachSpeedTable()));
//			System.out.println(sm.toCSVString(sm.getMachAngleTable()));
			System.out.println(sm.toCSVString(sm.getBusVoltTable()));
			assertTrue(sm.getMachSpeedTable().get("Bus10003-mach1").get(0).getValue()-sm.getMachSpeedTable().get("Bus10003-mach1").get(10).getValue()<1.0E-4);
			assertTrue(sm.getMachSpeedTable().get("Bus10009-mach1").get(0).getValue()-sm.getMachSpeedTable().get("Bus10009-mach1").get(10).getValue()<1.0E-4);
			
		}
		
		//@Test
		public void test_IEEE300_Dstab_compositeLoadModel() throws InterpssException{
			IpssCorePlugin.init();
			IpssLogger.getLogger().setLevel(Level.WARNING);
			PSSEAdapter adapter = new PSSEAdapter(PsseVersion.PSSE_30);
			assertTrue(adapter.parseInputFile(NetType.DStabNet, new String[]{
					"testData/IEEE300/IEEE300Bus_modified_noHVDC.raw",
					"testData/IEEE300/IEEE300_dyn_v2_cmld.dyr"
			}));
			DStabModelParser parser =(DStabModelParser) adapter.getModel();
			
			//System.out.println(parser.toXmlDoc());
            
			
			
			SimuContext simuCtx = SimuObjectFactory.createSimuNetwork(SimuCtxType.DSTABILITY_NET);
			if (!new ODMDStabParserMapper(IpssCorePlugin.getMsgHub())
						.map2Model(parser, simuCtx)) {
				System.out.println("Error: ODM model to InterPSS SimuCtx mapping error, please contact support@interpss.com");
				return;
			}
			
			
		    BaseDStabNetwork dsNet =simuCtx.getDStabilityNet();
		    
		    //dsNet.setBypassDataCheck(true);
			DynamicSimuAlgorithm dstabAlgo = simuCtx.getDynSimuAlgorithm();
			LoadflowAlgorithm aclfAlgo = dstabAlgo.getAclfAlgorithm();
			
			aclfAlgo.getDataCheckConfig().setAutoTurnLine2Xfr(true);

			aclfAlgo.getLfAdjAlgo().setApplyAdjustAlgo(false);
			aclfAlgo.setTolerance(1.0E-6);
			assertTrue(aclfAlgo.loadflow());
			System.out.println(AclfOutFunc.loadFlowSummary(dsNet));
			
			dstabAlgo.setSimuMethod(DynamicSimuMethod.MODIFIED_EULER);
			dstabAlgo.setSimuStepSec(0.002);
			dstabAlgo.setTotalSimuTimeSec(5.0);
			//dstabAlgo.setRefMachine(dsNet.getMachine("Bus10030-mach1"));
			

			StateMonitor sm = new StateMonitor();
			sm.addBusStdMonitor(new String[]{"Bus10000","Bus10001","Bus10015","Bus10016","Bus10028"});
			sm.addBusStdMonitor(new String[]{"Bus1_loadBus","Bus33_loadBus","Bus562_loadBus"});
			
			sm.addGeneratorStdMonitor(new String[]{"Bus10003-mach1","Bus10005-mach1","Bus10008-mach1","Bus10009-mach1"});
			
			sm.addDynDeviceMonitor(DynDeviceType.ACMotor, "ACMotor_1@Bus1_loadBus");
			sm.addDynDeviceMonitor(DynDeviceType.ACMotor, "ACMotor_1@Bus33_loadBus");
			sm.addDynDeviceMonitor(DynDeviceType.InductionMotor, "IndMotor_1_A@Bus1_loadBus");
			
			// set the output handler
			dstabAlgo.setSimuOutputHandler(sm);
			dstabAlgo.setOutPutPerSteps(25);
			//dstabAlgo.setRefMachine(dsNet.getMachine("Bus39-mach1"));
			
			IpssLogger.getLogger().setLevel(Level.INFO);
			
			dsNet.addDynamicEvent(DStabObjectFactory.createBusFaultEvent("Bus1",dsNet,SimpleFaultCode.GROUND_3P,new Complex(0,0),null,1.0d,0.08),"3phaseFault@Bus17");
			

			if (dstabAlgo.initialization()) {
				double t1 = System.currentTimeMillis();
				System.out.println("time1="+t1);
				System.out.println("Running DStab simulation ...");
				System.out.println(dsNet.getMachineInitCondition());
				dstabAlgo.performSimulation();
				double t2 = System.currentTimeMillis();
				System.out.println("used time="+(t2-t1)/1000.0);

			}
			System.out.println(sm.toCSVString(sm.getMachSpeedTable()));
			System.out.println(sm.toCSVString(sm.getMachAngleTable()));
			System.out.println(sm.toCSVString(sm.getBusVoltTable()));
			System.out.println(sm.toCSVString(sm.getMotorPTable()));
			System.out.println(sm.toCSVString(sm.getAcMotorPTable()));
			
			assertTrue(sm.getMachSpeedTable().get("Bus10003-mach1").get(0).getValue()-sm.getMachSpeedTable().get("Bus10003-mach1").get(10).getValue()<1.0E-4);
			assertTrue(sm.getMotorPTable().get("IndMotor_1_A@Bus1_loadBus").get(0).getValue()-sm.getMotorPTable().get("IndMotor_1_A@Bus1_loadBus").get(10).getValue()<1.0E-4);
			assertTrue(sm.getAcMotorPTable().get("ACMotor_1@Bus1_loadBus").get(0).getValue()-sm.getAcMotorPTable().get("ACMotor_1@Bus1_loadBus").get(10).getValue()<1.0E-4);
			

			FileUtil.writeText2File("IEEE300_busVolts_fault@Bus70.csv",sm.toCSVString(sm.getBusVoltTable()));
		
		}
		
		//@Test
		public void test_IEEE300_loadshedding_RL_continuous() {
			
		    IpssPyGateway app = new IpssPyGateway();
		    app.setLoggerLevel(1);
		    
			String[] caseFiles = new String[]{
					"testData/IEEE300/IEEE300Bus_modified_noHVDC_v2.raw",
					"testData/IEEE300/IEEE300_dyn_cmld_zone1.dyr"
					};
			
			String dynSimConfigFile = "testData\\IEEE300\\json\\IEEE300_dyn_config.json"; // define dynamic simulation and monitoring
			String rlConfigJsonFile = "testData\\IEEE300\\json\\IEEE300_RL_loadShedding_zone1_continuous.json";
			
			int[] ob_act_space_dim = null;
			
			try {
				ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
				
				System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));

				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
			app.reset(0, 2, 1.0, 0.10);
			double[][] obs_ary = app.getEnvObservationsDbl2DAry();
			
			// zone 1, observations: 108 buses (>60kv)
			// zone 1, actions: 20 buses


			System.out.println("observation length: " + obs_ary[0].length);
			assertTrue(obs_ary.length == 10);
			assertTrue(obs_ary[0].length == (108+20));
			System.out.println("Observations:\n"+Arrays.toString(app.getEnvObservationNames()));
			System.out.println(Arrays.toString(obs_ary[0]));
			
			System.out.println("Action buses:\n"+Arrays.toString(app.getActionBusIds()));
			
			
			// for faults at bus idx 0, fault duration 0.1 s
//			double[] actions = new double[] {0, 0, -0.0, -0.0, -0.0 , -0.1, -0.1, -0.1, -0.0, -0.1, 
//					-0.0, -0.0, -0.1, -0.0, -0.0, -0.1,  0., 0., -0.1, -0.1 };

			
			// for faults at bus idx 1 or 2, fault duration 0.1 s
			double[] actions = new double[] {0, 0, -0.0, -0.0, -0.1 , -0.1, -0.2, -0.0, -0.0, -0.0, 
					-0.0, -0.0, -0.1, -0.0, -0.1, -0.1,  -0.0, -0.0, -0.0, -0.1 };
			
			while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
				
				if(app.getDStabAlgo().getSimuTime()>1.1 && app.getDStabAlgo().getSimuTime()<1.85){
					app.nextStepDynSim(0.1, actions, "continuous");
				} 
				else
					app.nextStepDynSim(0.1, new double[20], "discrete");
//				
				//app.nextStepDynSim(0.1, new double[20], "continuous");
				//app.nextStepDynSim(0.1, actions, "continuous");
				
				app.getReward();
				
				if(app.isSimulationDone())
					break;
				
//				if(app.getDStabAlgo().getSimuTime()>10.0)
//				    break;
//				
			}
			
			
			
			//System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
//			System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
			System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//			FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		
			System.out.println("total rewards ="+app.getTotalRewards());
			
		}
		
		@Test
		public void test_IEEE300_loadshedding_RL_continuous_moreActionBuses() {
			
		    IpssPyGateway app = new IpssPyGateway();
		    app.setLoggerLevel(1); //0 - no logging, 1 - warming, 2 - info
		    
			String[] caseFiles = new String[]{
					"testData/IEEE300/IEEE300Bus_modified_noHVDC_v2.raw",
					"testData/IEEE300/IEEE300_dyn_cmld_zone1.dyr"
					};
			
			String dynSimConfigFile = "testData\\ACTIVSg2000\\json\\Texas2000_dyn_config.json"; // define dynamic simulation and monitoring
			String rlConfigJsonFile = "testData\\ACTIVSg2000\\json\\Texas2000_RL_loadShedding_zone3.json";
			
			int[] ob_act_space_dim = null;
			
			try {
				ob_act_space_dim = app.initStudyCase(caseFiles, dynSimConfigFile, rlConfigJsonFile);
				
				System.out.println("ob_act_space_dim array = "+Arrays.toString( ob_act_space_dim));

				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
			app.reset(0, 1, 0.0, 0.08);
			double[][] obs_ary = app.getEnvObservationsDbl2DAry();
			
			// zone 1, observations: 108 buses (>60kv)
			// zone 1, actions: 20 buses


			System.out.println("observation length: " + obs_ary[0].length);
			//assertTrue(obs_ary.length == 10);
			//assertTrue(obs_ary[0].length == (108+46));
			System.out.println("Observations:\n"+Arrays.toString(app.getEnvObservationNames()));
			System.out.println(Arrays.toString(obs_ary[0]));
			
			System.out.println("Action buses:\n"+Arrays.toString(app.getActionBusIds()));
			
			
			// for faults at bus idx 0, fault duration 0.1 s
//			double[] actions = new double[] {0, 0, -0.0, -0.0, -0.0 , -0.1, -0.1, -0.1, -0.0, -0.1, 
//					-0.0, -0.0, -0.1, -0.0, -0.0, -0.1,  0., 0., -0.1, -0.1 };

			
			// for faults at bus idx 1 or 2, fault duration 0.1 s
//			double[] actions = new double[] {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
//                    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
//                    -1, -1, -1, -1, -1, -1};
			
			// to check COMPLDW 3-phase induction motor when  all loads at 3 buses are shedded.
//			double[] actions = new double[] {-1, -1, -1};
			
			double[] actions = new double[] {-0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, 
                    -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, -0.2, 
                    -0.2, -0.2, -0.2, -0.2, -0.2, -0.2};
			
			int i = 0;
			while(app.getDStabAlgo().getSimuTime()<app.getDStabAlgo().getTotalSimuTimeSec()) {
				System.out.println("Time ="+app.getDStabAlgo().getSimuTime());
				
				app.nextStepDynSim(0.1, actions, "continuous");
				
//				if(app.getDStabAlgo().getSimuTime()<0.1)
//				     app.nextStepDynSim(0.1, new double[46], "continuous");
//				else if(i<5) {
//					app.nextStepDynSim(0.1, actions, "continuous");
//					i++;
//				}
//				else {
//					app.nextStepDynSim(0.1, new double[46], "continuous");
//				}
				   
				
//				if(app.getDStabAlgo().getSimuTime()>1.1 && app.getDStabAlgo().getSimuTime()<1.85){
//					app.nextStepDynSim(0.1, actions, "continuous");
//				} 
//				else
//					app.nextStepDynSim(0.1, new double[46], "continuous");
//				
				//app.nextStepDynSim(0.1, new double[20], "continuous");
				//app.nextStepDynSim(0.1, actions, "continuous");
				
				app.getReward();
				
				if(app.isSimulationDone())
					break;
				
//				if(app.getDStabAlgo().getSimuTime()>1.0)
//				    break;
//				
			}
			
			
			
			System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachSpeedTable()));
			System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
			System.out.println(app.getStateMonitor().toCSVString(app.getStateMonitor().getBusVoltTable()));
//			FileUtil.writeText2File("C:\\Qiuhua\\DeepScienceLDRD\\output\\mach_angle_refbus1.csv",app.getStateMonitor().toCSVString(app.getStateMonitor().getMachAngleTable()));
		
			System.out.println("total rewards ="+app.getTotalRewards());
			
		}
		
		private DynamicEvent create3PhaseFaultEvent(String faultBusId, BaseDStabNetwork net,double startTime, double durationTime){
		       // define an event, set the event id and event type.
				DynamicEvent event1 = DStabObjectFactory.createDEvent("BusFault3P@"+faultBusId, "Bus Fault 3P@"+faultBusId, 
						DynamicEventType.BUS_FAULT, net);
				event1.setStartTimeSec(startTime);
				event1.setDurationSec(durationTime);
				
		      // define a bus fault
				BaseDStabBus faultBus = net.getDStabBus(faultBusId);

				AcscBusFault fault = CoreObjectFactory.createAcscBusFault("Bus Fault 3P@"+faultBusId, net);
		  		fault.setBus(faultBus);
				fault.setFaultCode(SimpleFaultCode.GROUND_3P);
				fault.setZLGFault(NumericConstant.SmallScZ);

		      // add this fault to the event, must be consist with event type definition before.
				event1.setBusFault(fault); 
				return event1;
		}

}
