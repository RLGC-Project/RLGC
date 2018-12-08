package org.pnnl.gov.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.interpss.pssl.plugin.cmd.json.BaseJSONBean;
import org.junit.Test;
import org.pnnl.gov.json.ReinforcementLearningConfigBean;

public class testRlConfigJsonFile {
	
	@Test
	public void testRLConfig() throws IOException {
		ReinforcementLearningConfigBean rlConfig = BaseJSONBean.toBean("testData\\\\Kundur-2area\\json\\kundur2area_RL_config.json", ReinforcementLearningConfigBean.class);
		
		System.out.println(rlConfig.toString());
		
		assertTrue(rlConfig.environmentName.equals("kunder-2area"));
		
		assertTrue(rlConfig.actionTypes[0].equals("BrakeAction"));
	}

}
