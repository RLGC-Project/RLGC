package org.pnnl.gov.json;

import java.util.List;

import org.interpss.pssl.plugin.cmd.json.BaseJSONBean;

import com.interpss.dstab.devent.DynamicSimuEventType;

public class ContingencyConfigBean extends BaseJSONBean {

	
	public String[] componentIdAry;
	
	public DynamicSimuEventType eventType = null;
	
	public String contingencyId ="";
	
	public double eventValue = 0.0;
	
	public double eventStartTime = 0.0;
	
	public double eventDuration= 0.0;
	

}
