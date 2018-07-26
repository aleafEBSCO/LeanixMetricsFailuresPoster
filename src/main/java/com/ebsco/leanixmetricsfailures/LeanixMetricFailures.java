package com.ebsco.leanixmetricsfailures;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import net.leanix.dropkit.apiclient.ApiException;
import net.leanix.metrics.api.PointsApi;
import net.leanix.metrics.api.SeriesApi;
import net.leanix.metrics.api.models.Field;
import net.leanix.metrics.api.models.Point;
import net.leanix.metrics.api.models.SeriesResponse;
import net.leanix.metrics.api.models.Value;

public class LeanixMetricFailures 
{
	//below are the object paths for leanix api clients and client builders
	//the common clients/builders are used for accessing information about factsheets
	//the dropkit client/builders are used for posting that information to leanix metrics
    /*
	private net.leanix.api.common.ApiClient commonClient;
	private net.leanix.api.common.ApiClientBuilder commonClientBuidler;
	private net.leanix.dropkit.apiclient.ApiClient metricClient;
	private net.leanix.dropkit.apiclient.ApiClientBuilder metricClientBuilder;
    */
	private String apiToken = "";
	private String workspaceID = "";
	private String measurementsName = "";
	
	public LeanixMetricFailures(String at, String wi, String mn) {
		this.apiToken = at;
		this.workspaceID = wi;
		this.measurementsName = mn;
	}
    
	//create a Metrics API object to post to leanix metrics
    public PointsApi LeanixMetricsAPI() {
    	net.leanix.dropkit.apiclient.ApiClient apiClient = new net.leanix.dropkit.apiclient.ApiClientBuilder()
    			//url for the post request
    			.withBasePath("https://us.leanix.net/services/metrics/v1")
    			//to get oauth2 token
    			.withTokenProviderHost("us.leanix.net")
    			//api token from the workspace you'll be using
    			.withApiToken(this.apiToken)
    			.build();
    	//create the api object and return it
    	PointsApi pointsApi = new PointsApi(apiClient);
    	return pointsApi;
    }
    
  //create a Metrics Series api to get the information on the existing series
    public SeriesApi LeanixMetricsSeriesAPI() {
    	net.leanix.dropkit.apiclient.ApiClient client = new net.leanix.dropkit.apiclient.ApiClientBuilder()
    			//url for get request
    			.withBasePath("https://us.leanix.net/services/metrics/v1")
    			//to get oauth2 token
    			.withTokenProviderHost("us.leanix.net")
    			//api token from the workspace you'll be using
    			.withApiToken(this.apiToken)
    			.build();
    	//create the api and return it
    	SeriesApi seriesApi = new SeriesApi(client);
    	return seriesApi;
    }
    
    
    //check all the measurements already in leanix to make sure you don't post twice in one day
    public boolean pointAlreadyExists(OffsetDateTime time) {
    	//get the series api
    	SeriesApi seriesApi = LeanixMetricsSeriesAPI();
    	
    	//All times will be in UTC. Zone offsets are used to make sure that posting twice in the same 
    	//utc day and local time zone day don't happen
    	
    	//get the current time in a object that give more options for comparisons
    	LocalDateTime currentTime = time.toLocalDateTime();
    	    	
    	//the zone offset will be the difference in time, including the + and -, between the local time zone
    	//and utc
    	ZoneOffset zo = OffsetDateTime.now().getOffset();
    	
    	//get midnight and the date of the utc day
    	LocalTime midnight = LocalTime.MIDNIGHT;
    	LocalDate today = LocalDate.now(ZoneOffset.UTC);
    	
    	//midnight of the utc minus the offset will be the cutoff. If something is posted before the
    	//cutoff, it's possible something could be posted twice in one day
    	LocalDateTime cutOff = LocalDateTime.of(today, midnight).minusSeconds(zo.getTotalSeconds());
    	
    	//if the current time is before the cutoff even though it's a new utc day, the day in the local time zone
    	//hasn't changed yet
    	if (currentTime.isBefore(cutOff)) {
    		//let the user know and don't post the point
    		System.out.println("In order to prevent posting twice in one day, you can no longer post points to LeanIX metrics until later.");
    		return true;
    	}
    	
    	//else get the points
    	try {
    		//get the measurements
			SeriesResponse response = seriesApi.getSeries("SELECT * FROM \"" + this.measurementsName + "\"", this.workspaceID);
			
			//if no data is returned
			if (response.getData() == null) {
				//no data has been posted yet so there aren't duplicates
				return false;
			}
			
			//go through the measurements
			for (Value value : response.getData().getValues()) {
				//get the date from the measurement
				LocalDate measurementDate = value.getT().toLocalDate();
				//if the date is the same as today's utc date
				if (measurementDate.equals(currentTime.toLocalDate())) {
					//the user already posted today
					System.out.println("You already posted to metrics today. You can't post again");
					return true;
				}
			}
		//catch an error getting the measurements
		} catch (ApiException e) {
			System.out.println("Error getting preexisiting measurements to prevent posting twice in a day.");
			e.printStackTrace();
		}
    	
    	//else the user hasn't posted yet so they can post
    	return false;
    }
    
    //post the point given the incomplete factsheet information
    public void pushPoint(Map<String, Integer> metrics) {
    	//create an empty point
    	Point point = new Point();
    	
    	//General Information
    	//get the current time from UTC. When I tried making it EST, the post request failed
    	OffsetDateTime currentTime= OffsetDateTime.now(ZoneOffset.UTC);
    	//use OffsetDateTime.parse("2018-03-17T13:21:42.318Z") to use a specific date/time
    	point.setTime(currentTime);
    	System.out.println(currentTime);
    	
    	if (pointAlreadyExists(currentTime)) {
    		return;
    	}
    	
    	//Title of the metrics.
    	point.setMeasurement(this.measurementsName);
    	//workspace id. Should be from same workspace as api token. It can be found in the API tokens tab
    	//in the Admin panel
    	point.setWorkspaceId(this.workspaceID);

    	//the field will hold the key value pair. The key is the failure type and the value is the
    	//number of failures
    	Field tempField = new Field();
    	for (String key : metrics.keySet()) {
    		tempField = new Field();
    		//set key to factsheet type
    		tempField.setK(key);
    		//set value to number of incomplete factsheets
    		tempField.setV(metrics.get(key).doubleValue());
    		//add the field to the list
    		point.addFieldsItem(tempField);
    	}
    	
    	//No tags are used now but the below code is how you would add it if you wanted to
    	/*
    	Tag t1 = new Tag();
    	t1.setK("factsheetID");
    	t1.setV("123456");
    	point.addTagsItem(t1);
    	*/
    	
    	System.out.println(point);
    	
    	//get the api object used to post the new point
       	PointsApi pa = LeanixMetricsAPI();
    	
       	//try to post the point, else there's an error
    	try {
			pa.createPoint(point);
			System.out.println("Point Created");
		} catch (net.leanix.dropkit.apiclient.ApiException e) {
			e.printStackTrace();
			System.out.println("Error posting point");
		}
		
    }
    
    //apiclient used to get information about factsheets
    public net.leanix.api.common.ApiClient QueryClient() {
    	net.leanix.api.common.ApiClient apiClient = new net.leanix.api.common.ApiClientBuilder()
    			//url to make get request from
    			.withBasePath("https://us.leanix.net/services/pathfinder/v1")
    			//api token from the workspace you will be using
    			.withApiToken(this.apiToken)
    			//leanix server
    			.withTokenProviderHost("us.leanix.net")
    			.build();
    	return apiClient;
    }
    
    //get the number of incomplete factsheets of the given type
    public FilterTools LoadFilterFactsheets() {
    	
    	//list of factsheet types, there should be a .graphql file for each type
    	String[] types = {"boundedContext", "domain", "dataObject", "ITComponent", "behavior",
    			"useCase", "epic", "persona"};
    	
    	FilterTools ft = new FilterTools();
    	
    	for (int i = 0; i < types.length; i++) {
    		//get the api client
        	net.leanix.api.common.ApiClient apiClient = QueryClient();
        	
        	//create a query object
        	Query query = new Query();
        	//send the api client and type of factsheet to query to get a map of factsheet information
    		Map<String, Map<String, Object>> data = query.getInfo(apiClient, "/" + types[i] + ".graphql");
    		//get the list of edges from the map
    		try {
    			List<Map<String, Object>> edgeList = (List<Map<String, Object>>) data.get("allFactSheets").get("edges");
    		
    			ft.setCurrentList(edgeList);
    			ft.setType(types[i]);
    			ft.filterData();
    		}
    		catch(NullPointerException e) {
    			System.out.println("Query returned null most likely because the API token is incorretct/expired");
    			e.printStackTrace();
    			return null;
    		}
    		
    	}

		return ft;

    }
    
}
