package com.ebsco.leanixmetricsfailures;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class LeanixMetricFailuresTests {
	String apiToken = "";
	String workspaceID = "";
	
	//check to make sure a filtertools is returned from LoadFilterFactsheets
	@Test
	public void loadFilterFactsheetsTest() {
		//create the object
		LeanixMetricFailures lm = new LeanixMetricFailures(apiToken, workspaceID);
		FilterTools ft = lm.LoadFilterFactsheets();
		
		//make sure that there are sizes for the types of failures
		assert(ft.getAccountableSize() >= 0);
		assert(ft.getBusinessCriticalitySize() >= 0);
		assert(ft.getBusinessValueRiskSize() >= 0);
		assert(ft.getDocumentsSize() >= 0);
		assert(ft.getFunctionalFitSize() >= 0);
		assert(ft.getLifecycleSize() >= 0);
		assert(ft.getModelStatusSize() >= 0);
		assert(ft.getOwnerPersonaSize() >= 0);
		assert(ft.getQualitySealSize() >= 0);
		assert(ft.getRelationSize() >= 0);
		assert(ft.getResponsibleSize() >= 0);
		assert(ft.getScoreSize() >= 0);
		assert(ft.getTechnicalFitSize() >= 0);
		
	}
	
	//test to make sure that an error will be thrown when trying to get info with the wrong api key
	@Test
	public void brokenKeyGetTest() {
		//object with fake key
		LeanixMetricFailures lm = new LeanixMetricFailures("1234", workspaceID);
		
		//try to load factsheets
		FilterTools ft = lm.LoadFilterFactsheets();
    	
		//it will return null
    	assertEquals(ft, null);
		
	}
	
	//test to make sure an error will be thrown when trying to post info with the wrong api key
	@Test
	public void brokenKeyPostTest() {
		//fake metrics
		Map<String, Integer> metrics = new HashMap<String, Integer>();
    	metrics.put("relation", 1);
    	metrics.put("accountable", 2);
    	metrics.put("responsible", 3);
    	metrics.put("businessCriticality", 4);
    	metrics.put("ownerPersona", 5);
    	metrics.put("functionalFit", 6);
    	metrics.put("technicalFit", 7);
    	metrics.put("qualitySeal", 8);
    	metrics.put("modelStatus", 9);
    	metrics.put("score", 10);
    	metrics.put("documents", 11);
    	metrics.put("lifecycle", 12);
    	metrics.put("businessValueRisk", 13);
    	
    	//create object with fake key
    	LeanixMetricFailures ft = new LeanixMetricFailures("1234", workspaceID);
    	
    	ft.pushPoint(metrics);
    	
	}
	

}
