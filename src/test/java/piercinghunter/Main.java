package piercinghunter;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
     *  1) using api to get all stock symbols done
     *  2) get day data of each stock done
     *  3) get hourly (last hour) of each stock - later
     *  endpoints: https://api.iextrading.com/1.0/ref-data/symbols
     * https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=MSFT&apikey=YVD6NCJOTY1PF66U
        
        4) create txt file and put the found stocks there :)
        
  		5) check if peircing confirmed:
  		
     */
	
	 static boolean getCheckedData   = true;
	 static ArrayList<Stock>  stocks = new ArrayList<Stock>();
	 static ArrayList<String> stocksWithPiercing = new ArrayList<String>();
	 static ArrayList<String> stocksWithErrors   = new ArrayList<String>();
	 static ObjectMapper objectMapper = new ObjectMapper();
	 static boolean piercingConfirmer = true;
	 static boolean piercingfinder = false;
	 
	public static void main(String[] args) throws Exception {

	    //for debuging
//		if (checkForPiercing("XXII")) {
//			System.out.println("We have a piercing here!!!!!");
//		} else {
//			System.out.println("No piercing");
//		}
		if (piercingConfirmer) {
			System.out.println("confirmer is workings");
		}
		
		if (piercingfinder) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyyHH_mm_ss");  
		    Date date = new Date();  
		    String fileName = formatter.format(date);
		    String filePath = System.getProperty("user.dir")+"\\src\\test\\resources\\piercings\\" + fileName +".txt";
			PrintWriter writer = new PrintWriter(filePath, "UTF-8");
			try {
				stocks = objectMapper
						 .readValue(Unirest.get("https://api.iextrading.com/1.0/ref-data/symbols")
						 .asJson()
						 .getRawBody(),new TypeReference<List<Stock>>(){});
			} catch (Exception e) {
				System.exit(0);
			}
			
			int requestNum = 0;
			int foundPiercings = 0;
			
			String symbol;
			for (Stock s : stocks) {
				if (s.type.equals("cs") || s.type.equals("et")) {
					System.out.println(++requestNum + ". Checking " + s.symbol);
					symbol = s.symbol.replaceAll("[^A-Za-z0-9()\\[\\]]", "");
					if (getSymbolData(symbol)){
						stocksWithPiercing.add(symbol);
						writer.println(++foundPiercings + " : "+symbol);
						writer.flush();
					} else {
						System.out.println(symbol + " No Piercing");
						if (stocksWithPiercing.size() > 0) {
							System.out.println("piercing found on " + stocksWithPiercing.size());
						}
					}
					//Thread.sleep(500); //api call restrictions
				}		
			}
			writer.close();
		}
	   
	}
	
	
	private static boolean getSymbolData(String stock) throws Exception {
		HttpResponse<JsonNode> jsonResponse = null;
		JSONObject stocks = new JSONObject();
		try {
			jsonResponse = Unirest
					.get("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+ stock + "&apikey=C0F6Z5D4ELEKDZ0U")
					.asJson();	
		} catch (Exception e) {
			System.out.println("program terminated");
			System.exit(0);
		}
		
		if (jsonResponse != null && jsonResponse.getStatus() == 200) {
			JSONObject obj = new JSONObject(jsonResponse.getBody());
			JSONArray arr = obj.getJSONArray("array");
			
			//System.out.println(obj);
			try {
				stocks = arr.getJSONObject(0).getJSONObject("Time Series (Daily)");
			} catch(Exception exception) {
				stocksWithErrors.add(stock);
				System.out.println("Error here");
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
			    	return false; //"DownTrend, Nothing here"
			    }
		    } else {
		    	return false; //day before uptrend
		    }
		} else {
			return false;
		}
	}
	

}
