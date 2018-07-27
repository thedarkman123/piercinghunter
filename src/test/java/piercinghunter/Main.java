package piercinghunter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.text.StyledEditorKit.ForegroundAction;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class Main {
	
	 //TODO - BETTER PIERCINGS ARE THE ONES WITH VOLUME BETTER THEN THE LAST DAY (MORE CHANCES)
	 //SHOULD BE CONFIRMED TODAY 
	
	//TODO - GET YEAR DATA, AVERAGE OF VOLUME MORE THAN 500000 AND WE ARE ALL GOOD 
	
	//TODO - WORKING WITH OBJECT CLOSE OPEN HIGH LOW
	
	
     //8633 stocks checked
	 static	SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyyHH_mm_ss");  
	 static Date date = new Date(); 
	 static TreeMap<String,DailyStockInfo> dailyStocksInfoObject = new TreeMap<>();
	 
	 static boolean checkPatterns  = false;
	 static boolean findPatterns   = true;
	 static boolean onlyHighVolume = true;
	 static float   priceUntil     = 50;
	 static float   priceFrom      = 2.50f;
	 
	 //LOGN PATTERNS
	 public static final String HARAMI       = "HARAMI";
	 public static final String ERROR        = "ERROR";
	 public static final String LOWVOLUME    = "LOWVOLUME";
	 public static final String PIERCING     = "PIERCING";
	 public static final String BESTPIERCING = "BESTPIERCING"; //much harsh statistics
	 public static final String NOTHING      = "NOTHING";
	 public static final String MORNINGSTAR  = "MORNINGSTAR";
	 
	 //SHORT PATTERNS
	 public static final String EVENINGSTAR = "EVENINGSTAR";
	 
	 
	public static void main(String[] args) throws Exception {
		
		if(checkPatterns) {
			confirmation("25_07_201813_35_58",EVENINGSTAR); //in the meantime, failed is pass
			confirmation("25_07_201813_35_58",PIERCING);
			confirmation("25_07_201813_35_58",HARAMI);
			confirmation("25_07_201813_35_58",MORNINGSTAR);
			confirmation("25_07_201813_35_58",BESTPIERCING);
		}
		
		if (findPatterns) {
			ArrayList<String>  stocks = getAllStocks(true);
			initiatePatternSearch(stocks);
		}	
	}
	
	private static ArrayList<String> getAllStocks(boolean stocksFromFile){
		ArrayList<String> stocks = new ArrayList<>();
		if (stocksFromFile) {
			ArrayList<String> file = readFileToArr(System.getProperty("user.dir")+"\\src\\test\\resources\\stocksList");
			for(String s : file) {
				stocks.add(s.split("[|]")[1].trim());
			}
		} else {
			HttpResponse<JsonNode> jsonResponse = null;
			try {
				jsonResponse = Unirest
						.get("https://api.iextrading.com/1.0/ref-data/symbols")
						.asJson();	
			} catch (Exception e) {
				System.exit(0);
			}
			if (jsonResponse != null && jsonResponse.getStatus() == 200) {
				try {
					JSONObject obj = new JSONObject(jsonResponse.getBody());
					JSONArray arr = obj.getJSONArray("array");
					for (int i=0; i<arr.length();i++) {
						obj = (JSONObject) arr.get(i);
						stocks.add(obj.getString("symbol"));
					}
				} catch(Exception exception) {
					System.out.println("stock list from web is not found");
					System.exit(0);
				}
			}
		}
		
		return stocks;
	}
	
	private static void initiatePatternSearch(ArrayList<String> stocks) throws Exception {
		System.out.println("program started");
		String fileName = formatter.format(date);
		
		//paths to files
		String filePathToEveningStar  = System.getProperty("user.dir")+"\\src\\test\\resources\\eveningstar\\" + fileName +".txt";
		String filePathToMorningStar  = System.getProperty("user.dir")+"\\src\\test\\resources\\morningstar\\" + fileName +".txt";
	    String filePathToPiercing     = System.getProperty("user.dir")+"\\src\\test\\resources\\piercing\\" + fileName +".txt";
	    String filePathToHarami       = System.getProperty("user.dir")+"\\src\\test\\resources\\harami\\" + fileName +".txt";
	    String filePathToErrors       = System.getProperty("user.dir")+"\\src\\test\\resources\\error\\" + fileName +".txt";
	    String filePathToBestPiercing = System.getProperty("user.dir")+"\\src\\test\\resources\\bestpiercing\\" + fileName +".txt";
	    
	    //initiate writers
	    PrintWriter bestPiercingWriter = new PrintWriter(filePathToBestPiercing, "UTF-8");
	    PrintWriter piercingWriter     = new PrintWriter(filePathToPiercing, "UTF-8");
	    PrintWriter haramiWriter       = new PrintWriter(filePathToHarami, "UTF-8");
	    PrintWriter errorWriter        = new PrintWriter(filePathToErrors, "UTF-8");
	    PrintWriter morningStarWriter  = new PrintWriter(filePathToMorningStar, "UTF-8");
	    PrintWriter eveningStarWriter  = new PrintWriter(filePathToEveningStar, "UTF-8");

		int stockNum = 0;	
		String patternFound;
		for (String s : stocks) {
			System.out.println(++stockNum + ". Checking " + s);
			patternFound = findPatterns(s);
			if (patternFound.equals(ERROR)) {
				System.out.println("Found ERROR in stock " + s);
				errorWriter.println(s);
				haramiWriter.flush();
			} else if (patternFound.equals(HARAMI)) {
				System.out.println("Found HARAMI in stock " + s);
				haramiWriter.println(s);
				haramiWriter.flush();
			} else if (patternFound.equals(PIERCING)) {
				System.out.println("Found PIERCING in stock " + s);
				piercingWriter.println(s);
				piercingWriter.flush();
			} else if (patternFound.equals(LOWVOLUME)) {
				//MAYBE I WILL DO SOMETHING WITH THIS LATER
			} else if (patternFound.equals(MORNINGSTAR)) {
				System.out.println("Found MORNINGSTAR in stock " + s);
				morningStarWriter.println(s);
				morningStarWriter.flush();
			} else if (patternFound.equals(EVENINGSTAR)) {
				System.out.println("Found EVENINGSTAR in stock " + s);
				eveningStarWriter.println(s);
				eveningStarWriter.flush();
			} else if (patternFound.equals(BESTPIERCING)) {
				System.out.println("Found BESTPIERCING in stock " + s);
				bestPiercingWriter.println(s);
				bestPiercingWriter.flush();
			}
		}
		bestPiercingWriter.close();
		piercingWriter.close();
		haramiWriter.close();
		errorWriter.close();
		morningStarWriter.close();
		eveningStarWriter.close();
		System.out.println("program Done");
	}
	
	private static String findPatterns(String stock) throws Exception {
		//get stock data
		HttpResponse<JsonNode> jsonResponse = null;
		JSONObject dailyStockInfoJsonObject = new JSONObject();
		try {
			jsonResponse = Unirest
					.get("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+ stock + "&apikey=C0F6Z5D4ELEKDZ0U")
					.asJson();	
		} catch (Exception e) {
			System.out.println("error with "+ stock + " moving on");
			return ERROR;
		}
		//check of data received with 200 code
		if (jsonResponse != null && jsonResponse.getStatus() == 200) {
			//get stock data
			JSONObject obj = new JSONObject(jsonResponse.getBody());
			JSONArray arr = obj.getJSONArray("array");
			try {
				//try get stock info
				dailyStockInfoJsonObject = arr.getJSONObject(0).getJSONObject("Time Series (Daily)");
			} catch(Exception exception) {
				//if error get all the errors stocks into a list
				System.out.println("Error here, this is the json for stock " + stock);
				System.out.println(obj);
				return ERROR;
			}
		} else {
			System.out.println("Response is null or status incorrect, moving on");
			return ERROR;
		}

		DailyStockInfo stockInfo;
		String key;
	    for (int i = 0; i < dailyStockInfoJsonObject.names().length(); i++) {
	    	key = (String)dailyStockInfoJsonObject.names().get(i);
	    	stockInfo = new DailyStockInfo();
	    	stockInfo.date   = key;
			stockInfo.open   = dailyStockInfoJsonObject.getJSONObject(key).getFloat("1. open");
			stockInfo.high   = dailyStockInfoJsonObject.getJSONObject(key).getFloat("2. high");
			stockInfo.low    = dailyStockInfoJsonObject.getJSONObject(key).getFloat("3. low");
			stockInfo.close  = dailyStockInfoJsonObject.getJSONObject(key).getFloat("4. close");
			stockInfo.volume = dailyStockInfoJsonObject.getJSONObject(key).getInt("5. volume"); 
			dailyStocksInfoObject.put(key, stockInfo);
	    }
	    
	    //only high volume stocks
	    if (onlyHighVolume) { //get all data, do average volume on it
	    	long volumeAverage = 0;
	    	for (Map.Entry<String, DailyStockInfo> entry : dailyStocksInfoObject.entrySet())
	    	{
	    		volumeAverage += entry.getValue().volume;
	    	}
	    	if (volumeAverage/dailyStocksInfoObject.size() < 500000){		
	    		return NOTHING;
	    	}
	    }
	    	      
        try {
		    
		    //only stocks between 2 prices
		    if (getDailyData(1).close < priceFrom ||
	    		getDailyData(1).close > priceUntil) {
		    	return NOTHING;
		    }
		    
		    if (checkForSoftBullishHarami()) return HARAMI;
		    if (checkForPiercing())          return PIERCING;
		    if (checkForBestPiercing())      return BESTPIERCING;
		    if (checkForMorningStar())       return MORNINGSTAR;
		    if (checkForEveningStar())       return EVENINGSTAR;
		   
        } catch (Exception e) {
        	 return NOTHING;
        }
     
        return NOTHING;	    
	}
	
	private static DailyStockInfo getDailyData(int daysAgo) {
		Object[] keys = dailyStocksInfoObject.keySet().toArray();
		return dailyStocksInfoObject.get(keys[keys.length-daysAgo]);
	}
	
	private static boolean checkForEveningStar() {
		if (getDailyData(3).open < getDailyData(3).close) { //2 days before, trend up
			if (getDailyData(2).close > getDailyData(3).close
		     && getDailyData(2).open > getDailyData(3).close ) { // the day before candle is above the one before
				if (getDailyData(1).open > getDailyData(1).close) { //downtrend in the last day
					//calculate middle point of 2 days ago
					float middlePoint = (getDailyData(2).open + ((getDailyData(2).close - getDailyData(2).open))/2);
					if (getDailyData(1).close < middlePoint ) {
						return true; //potential evening star
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private static boolean checkForMorningStar() {
		if (getDailyData(3).open > getDailyData(3).close) { //2 days before, trend down
			if (getDailyData(2).close < getDailyData(3).close 
			 && getDailyData(2).open < getDailyData(3).close ) { // the day before candle is below the one before
				if (getDailyData(1).open < getDailyData(1).close) { //uptrend in the last day
					//calculate middle point of 2 days ago
					float middlePoint = (getDailyData(3).open + ((getDailyData(3).close - getDailyData(3).open))/2);
					if (getDailyData(1).close > middlePoint ) {
						return true; //potential morning star
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private static boolean checkForSoftBullishHarami() {//true to be in middle there as much as possible	
	    if (getDailyData(2).close < getDailyData(2).open) { //day before trend down
		   	 if (getDailyData(1).close > getDailyData(1).open) {//last day trend up
		   		  if ((getDailyData(1).open > getDailyData(2).close) && (getDailyData(1).close < getDailyData(2).open)) {
		   			float middlePoint = (getDailyData(2).open + ((getDailyData(2).close - getDailyData(2).open))/2);
		   			return getDailyData(1).close > middlePoint;
		   		  } 
		    } else {
		    	return false; //Continues down, nothing here
		    }
	   } else {
		   return false; //day before uptrend
	   }
	   return false;
	}
	
	//better piercing checker right now
	private static boolean checkForPiercing() {
	    if (getDailyData(2).close < getDailyData(2).open) { //day before trend down
	   	 if (getDailyData(1).close > getDailyData(1).open) {//last day trend up
		    	if (getDailyData(1).open < getDailyData(2).close && 
	    			getDailyData(2).low >  getDailyData(1).low) { //the open last day, is lower then close day before
		    		float middlePoint = (getDailyData(2).open + ((getDailyData(2).close - getDailyData(2).open))/2);
		    		if (middlePoint < getDailyData(1).close) { //confirm piercing
		    			return true;  //piercing!
		    		} else {
		    			return false; //no piercing
		    		}		
		    	} else {
		    		return false; //the open is higher that the day before, or the same
		    	}
	    } else {
	    	return false; //Continues down, nothing here
	    }
	   } else {
		   return false; //day before uptrend
	   }
	}
	
	//better piercing checker right now
	private static boolean checkForBestPiercing() {
	    if (getDailyData(2).close < getDailyData(2).open) { //day before trend down
	   	 if (getDailyData(1).close > getDailyData(1).open) {//last day trend up
		    	if (getDailyData(1).open < getDailyData(2).close && 
	    			getDailyData(2).low >  getDailyData(1).low) { //the open last day, is lower then close day before
		    		float middlePoint = (getDailyData(2).open + ((getDailyData(2).close - getDailyData(2).open))/2);
		    		if (middlePoint < getDailyData(1).close) { //confirm good piercing
		    			//now here we check if this is the best piercing
		    			//3 days before,down down down!
		    			if (getDailyData(3).close < getDailyData(3).open 
					     && getDailyData(4).close < getDailyData(4).open
					     && getDailyData(5).close < getDailyData(5).open) {
		    			  return true;  //bestpiercingpattern,after downtrend!
		    			} else {
		    			  return false;  //piercing!
		    			}
		    			
		    		} else {
		    			return false; //no piercing
		    		}		
		    	} else {
		    		return false; //the open is higher that the day before, or the same
		    	}
	    } else {
	    	return false; //Continues down, nothing here
	    }
	   } else {
		   return false; //day before uptrend
	   }
	}
	
	private static ArrayList<String> readFileToArr(String filePath) {
		 ArrayList<String> stocksToCheck = new ArrayList<String>();	 
		 try {	 
			File f = new File(filePath);
            BufferedReader b = new BufferedReader(new FileReader(f));
            String readLine = "";
            System.out.println("Reading file using Buffered Reader");
            while ((readLine = b.readLine()) != null) {
            	stocksToCheck.add(readLine);
            }
            b.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
		return stocksToCheck;
	}
	
	
	//confirmation setup
	private static void confirmation(String fileNameToCheck,String pattern) throws Exception {
		 //file to write to
		String filePathToCheck = System.getProperty("user.dir")+"\\src\\test\\resources\\" + pattern.toLowerCase() + "\\" + fileNameToCheck +".txt";	
		//System.out.println(filePathToCheck);
		ArrayList<String> stocksToCheck = readFileToArr(filePathToCheck);
		//System.out.println("We need to check " + stocksToCheck.size());
		String filePathToWrite = System.getProperty("user.dir")+"\\src\\test\\resources\\confirmations\\" 
		+ pattern.toLowerCase() 
		+ fileNameToCheck
		+".txt";
		PrintWriter writer = new PrintWriter(filePathToWrite, "UTF-8");
		System.out.println("We need to check " + stocksToCheck.size());
		for (String s : stocksToCheck) {
			writer.println(getPatternConfirmation(s));
			writer.flush();		
		}  
		writer.close();
	}
	
	//confirmation checker
	private static String getPatternConfirmation(String stock){	
		String stringToReturn = stock+": ";
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
			try {
				JSONObject obj = new JSONObject(jsonResponse.getBody());
				JSONArray arr = obj.getJSONArray("array");
				System.out.println(obj);
				stocks = arr.getJSONObject(0).getJSONObject("Time Series (Daily)");
			} catch(Exception exception) {
				stringToReturn += " Error occured";
				return stringToReturn;
			}
			 
			List<String> jsonValues = new ArrayList<String>();
		    for (int i = 0; i < stocks.names().length(); i++) {
		    	jsonValues.add(((String)stocks.names().get(i)));	    	
		    }
		    Collections.sort(jsonValues);
		    
		    float lastDayClose = stocks.getJSONObject(jsonValues.get(jsonValues.size()-1)).getFloat("4. close");
		    float lastDayHigh  = stocks.getJSONObject(jsonValues.get(jsonValues.size()-1)).getFloat("2. high"); 
		    
		    float dayBeforeClose = stocks.getJSONObject(jsonValues.get(jsonValues.size()-2)).getFloat("4. close");
		    if (dayBeforeClose < lastDayClose || dayBeforeClose < lastDayHigh) {
		    	stringToReturn += "*** SUCCESS *** ";
		    } else {
		    	stringToReturn += "*FAILED* ";
		    }
		    float lastDayClosePercentage = (lastDayClose - dayBeforeClose)/lastDayClose * 100;
		    float lastDayHighPercentage  = (lastDayHigh  - dayBeforeClose)/lastDayHigh  * 100;
			
		    stringToReturn += " from close: " + lastDayClosePercentage +"%, from high: "+lastDayHighPercentage+"%";
		}
		return stringToReturn;
	}
	

}
