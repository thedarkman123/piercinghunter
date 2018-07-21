package piercinghunter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class Main {
    /*
     *  1) using api to get all stock symbols
     *  2) get day data of each stock
     *  3) get hourly (last hour) of each stock
     *  endpoints: https://api.iextrading.com/1.0/ref-data/symbols
     * https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=MSFT&apikey=YVD6NCJOTY1PF66U
     */
	 static ArrayList<Stock> stocks = new ArrayList<Stock>();
	 static ArrayList<String> stocksWithPiercing = new ArrayList<String>();
	 static ObjectMapper objectMapper = new ObjectMapper();
	 
	public static void main(String[] args) throws Exception {
//		if (checkForPiercing("XXII")) {
//			System.out.println("We have a piercing here!!!!!");
//		} else {
//			System.out.println("No piercing");
//		}
		int requestNum = 0;
		stocks = objectMapper
				 .readValue(Unirest.get("https://api.iextrading.com/1.0/ref-data/symbols")
				 .asJson()
				 .getRawBody(),new TypeReference<List<Stock>>(){});
		for (Stock s : stocks) {
			if (s.type.equals("cs") || s.type.equals("et")) {
				System.out.println(++requestNum + ". Checking " + s.symbol);
				if (checkForPiercing(s.symbol)){
					stocksWithPiercing.add(s.symbol);
					System.out.println("****** piercing found:" + s.symbol + "*******");
				} else {
					System.out.println(s.symbol + " No Piercing");
					if (stocksWithPiercing.size() > 0) {
						System.out.println("piercing found on " + stocksWithPiercing.size());
					}
				}
				//Thread.sleep(500); //api call restrictions
			}		
		}
	
	}
	
	
	private static boolean checkForPiercing(String stock) throws Exception {
		HttpResponse<JsonNode> jsonResponse = Unirest
		.get("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+ stock + "&apikey=C0F6Z5D4ELEKDZ0U")
		.asJson();
		
		if (jsonResponse != null && jsonResponse.getStatus() == 200) {
			JSONObject obj = new JSONObject(jsonResponse.getBody());
			JSONArray arr = obj.getJSONArray("array");
			JSONObject stocks = new JSONObject();
			//System.out.println(obj);
			try {
				stocks = arr.getJSONObject(0).getJSONObject("Time Series (Daily)");
			} catch(Exception exception) {
				System.out.println("Something fucked up");
				System.out.println(obj);
				return false;
			}
			 
			//System.out.println(post_id.names());
			//Collections.sort();
			List<String> jsonValues = new ArrayList<String>();
		    for (int i = 0; i < stocks.names().length(); i++) {
		    	jsonValues.add(((String)stocks.names().get(i)));
		    }
		    Collections.sort(jsonValues);
//		    System.out.println("*****");
//		    System.out.println("Date of info: " + jsonValues.get(jsonValues.size()-1));
//		    System.out.println(stocks.getJSONObject(jsonValues.get(jsonValues.size()-1)));
//		   
		    BigDecimal lastDayClose = stocks.getJSONObject(jsonValues.get(jsonValues.size()-1)).getBigDecimal("4. close"); 
		   
//		    System.out.println("Last day close: " + lastDayClose);
		    BigDecimal lastDayOpen = stocks.getJSONObject(jsonValues.get(jsonValues.size()-1)).getBigDecimal("1. open"); 
//		    System.out.println("Last day open: " + lastDayOpen);
		    
//		    System.out.println("*****");
		    
//		    System.out.println("Day before Date: " + jsonValues.get(jsonValues.size()-3));
//		    
//		    System.out.println(stocks.getJSONObject(jsonValues.get(jsonValues.size()-3)));
		    BigDecimal dayBeforeClose = stocks.getJSONObject(jsonValues.get(jsonValues.size()-2)).getBigDecimal("4. close");
//		    System.out.println("Day before close: " + dayBeforeClose);
		    BigDecimal dayBeforeOpen = stocks.getJSONObject(jsonValues.get(jsonValues.size()-2)).getBigDecimal("1. open"); 
//		    System.out.println("Day before open: " + dayBeforeOpen);
		    
//		    System.out.println("****************");
		    //first of all, the day before, we need a down trend
		    if (dayBeforeOpen.compareTo(dayBeforeClose) > 0) {
//		    	System.out.println("Day before we going down");
		    	//now this day we need uptrend
		    	 if (lastDayOpen.compareTo(lastDayClose) < 0) {
			    	//now this day we need uptrend
//			    	System.out.println("This day, going up");
			    	//check for piercing potential
			    	if (lastDayOpen.compareTo(dayBeforeClose) < 0) { //check that last day open starts lower than before close
//			    		System.out.println("Potential piercing");
//			    		System.out.println(dayBeforeOpen.floatValue());
//			    		System.out.println(dayBeforeClose.floatValue());
//			    		
			    		BigDecimal middlePoint = new BigDecimal((dayBeforeOpen.floatValue() + ((dayBeforeClose.floatValue() - dayBeforeOpen.floatValue()))/2));
//			    		System.out.println("middle point: " + middlePoint);
			    		if (middlePoint.compareTo(lastDayClose) < 0) { //confirm piercing
			    			return true;
			    		} else {
			    			return false;
			    		}
			    		//float middleNum = (lastDayOpen.floatValue() - dayBeforeClose.floatValue()); 
//			    		System.out.println(middleNum);
			    		//System.out.println(dayBeforeOpen - dayBeforeClose);
			    		
			    	} else {
			    		return false;
			    	}
			    } else {
//			    	System.out.println("DownTrend, Nothing here");
			    	return false;
			    }
		    } else {
//		    	System.out.println("Uptrend, Nothing here");
		    	return false;
		    }
		} else {
			return false;
		}
	}
	

}
