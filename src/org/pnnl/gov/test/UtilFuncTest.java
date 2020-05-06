package org.pnnl.gov.test;

import java.io.File;

public class UtilFuncTest {
	
	
	public static void main(String[] args) {
		
	  String folderPath = "testData\\IEEE39\\baseCases";
		
		if(folderPath!=null && !folderPath.isEmpty()) {
		
			File dir = new File(folderPath); 
			File[] listFiles = dir.listFiles((d, s) -> {
				return s.toLowerCase().endsWith(".raw");
			});
			for (File f: listFiles) {
				System.out.println("File: " + dir + File.separator + f.getName());
				
				
			}
		}
	}

}
