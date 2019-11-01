package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.interpss.pssl.plugin.cmd.json.BaseJSONBean;
import org.junit.Test;
import org.pnnl.gov.json.ReinforcementLearningConfigBean;

public class testRlConfigJsonFile {
	
	@Test
	public void testRLConfigV1() throws IOException {
		ReinforcementLearningConfigBean rlConfig = BaseJSONBean.toBean("testData\\\\Kundur-2area\\json\\kundur2area_RL_config.json", ReinforcementLearningConfigBean.class);
		
		System.out.println(rlConfig.toString());
		
		assertTrue(rlConfig.environmentName.equals("kundur-2area"));
		
		assertTrue(rlConfig.actionTypes[0].equals("BrakeAction"));
	}
	
	@Test
	public void testRLConfigV2() throws IOException {
		ReinforcementLearningConfigBean rlConfig = BaseJSONBean.toBean("testData\\IEEE39\\json\\IEEE39_RL_loadShedding_3motor_continuous.json", ReinforcementLearningConfigBean.class);
		
		System.out.println(rlConfig.toString());
		
		assertTrue(rlConfig.environmentName.equals("IEEE39_FIDVR_LoadShedding_Continuous_Action"));
		
		assertTrue(rlConfig.actionTypes[0].equals("LoadShed"));
		
		assertTrue(rlConfig.version==2);
		
		assertTrue(rlConfig.actionSpaceType.equals("continuous"));
		
		assertTrue(rlConfig.actionValueRanges[0][0] ==-1.0);
		assertTrue(rlConfig.actionValueRanges[0][1] ==0.0);
		assertTrue(rlConfig.actionValueRanges[1][0] ==-1.0);
		assertTrue(rlConfig.actionValueRanges[1][1] ==0.0);
		assertTrue(rlConfig.actionValueRanges[2][0] ==-1.0);
		
		
	}
}