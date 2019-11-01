package org.pnnl.gov.rl.action;

import org.apache.commons.math3.complex.Complex;

import com.interpss.common.util.IpssLogger;
import com.interpss.dstab.DStabBus;
import com.interpss.dstab.DStabilityNetwork;
import com.interpss.dstab.dynLoad.DynLoadModel;

public class LoadChangeActionProcessor extends AbstractActionProcessor implements ActionProcessor{
	
	private double[] remainLoadFraction = null;
	
	public LoadChangeActionProcessor(DStabilityNetwork network){
		super(network);
	}
	
	@Override
	public double[] applyAction(double[] actionValueAry, String actionValueType, double duration) {
		double[] actualActionValues = null;
		
		if(actionValueAry!=null){
			actualActionValues = new double[actionValueAry.length];
			
			if(remainLoadFraction == null){
				this.remainLoadFraction = new double[actionValueAry.length];
				
				for( int i = 0; i < remainLoadFraction.length; i++){
					this.remainLoadFraction[i] = 1.0;
				}
				
			}
			
			// NOTE: actionValueAry must be corresponding to the actionScopeBusAry defined in the configuration file
			
			boolean isNoError = true;
			if(this.getActionLevels()==null){
				throw new Error("actionLevels array is null, but it is required. Actions cannot be applied!");
				
			}
			
		
		   //iterate the buses within action scope 
			int i = 0;
			for(String busId:this.actionScopeBusAry){
				
				double changeFraction = 0.0;
				
				if(actionValueType.length() == 0 || actionValueType.equalsIgnoreCase("discrete")) //by default the type is discrete
					changeFraction = this.getActionLevels()[(int)actionValueAry[i]];
				
				else if (actionValueType.equalsIgnoreCase("continuous")) {
					changeFraction = actionValueAry[i];
				}
				else {
					throw new Error("The <actionValueType> attribute has to be either 'discrete' or 'continuous'");
				}
				

				
				if(this.remainLoadFraction[i]+changeFraction <0.0){
					changeFraction = 0.0 - this.remainLoadFraction[i];
					IpssLogger.getLogger().warning("The change fraction for bus =" +busId+" is large than remaining fraction, it has been changed to : "+changeFraction);
				}
				
				System.out.println("Bus, remaining fraction, actual Load Shedding fraction = "+busId+", "+this.remainLoadFraction[i]+", "+changeFraction);
				
				this.remainLoadFraction[i] = this.remainLoadFraction[i]+changeFraction;
				
				
				
			    DStabBus bus = this.dstabNet.getBus(busId);
			    
			    if(bus == null){
			    	throw new Error("No corresponding bus for ID =" +busId+" . No action is applied!");
				    
			    }
			    
				
				
			    
			    if (changeFraction==0.0){
			    	IpssLogger.getLogger().info("Action at " +busId +" is NO-operation.");
			    	i++;
			    	continue;
			    }
			    
			    
			    IpssLogger.getLogger().info("Apply load-change action at " +busId +"fraction = "+changeFraction);
		        
			    if(bus.isActive()){
			    	
			    	actualActionValues[i] = changeFraction;
		    	   
		    	   // check if there is any dynamic load model
		    	   if(bus.getDynLoadModelList()!=null && !bus.getDynLoadModelList().isEmpty()){
		    		   for(DynLoadModel dynLoad: bus.getDynLoadModelList()){
		    			   if(dynLoad.isActive()){
		    				  
		    				   dynLoad.changeLoad(changeFraction);
		    				   
		    				   
		    			   }
		    		   }
		    	   }
		       
		
		        // process static loads, represented by netLoadResults. Need to change the system Ymatrix by updating Yii of the corresponding bus
		    	   
		    	   if(bus.getNetLoadResults().abs()>0){
		    		   double initVoltMag = bus.getInitVoltMag();
		    		   
		    		
		    		   Complex deltaPQ = bus.getNetLoadResults().multiply(changeFraction);
		    		   Complex deltaYii = deltaPQ.conjugate().divide(initVoltMag*initVoltMag);
		    		   
		    		   int sortNum = bus.getSortNumber();
		    		   
		    		   this.dstabNet.getYMatrix().addToA(deltaYii, sortNum, sortNum);
		    		   this.dstabNet.setYMatrixDirty(true);
		    	   }
		    	  
		       }
			    // update the index
			    i = i+1;
			}
		}
		else{
			IpssLogger.getLogger().severe("actionValueAry is null");
			return  null;
		}
		
		return actualActionValues;
	}
	
	@Override
	public double[] getAgentActionStatus() {
		
		if(remainLoadFraction == null){
			this.remainLoadFraction = new double[this.getActionScopeByBus().length];
			
			for( int i = 0; i < remainLoadFraction.length; i++){
				this.remainLoadFraction[i] = 1.0;
			}
			
		}
		
		return this.remainLoadFraction;
	}

}
