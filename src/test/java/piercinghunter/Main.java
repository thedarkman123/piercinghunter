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
import java.util.List;

import javax.swing.text.StyledEditorKit.ForegroundAction;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class Main {
     //8633 stocks checked
	 static	SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyyHH_mm_ss");  
	 static Date date = new Date(); 
	 
	 static boolean findPatterns = true;
	 static boolean onlyHighVolume = true;
	 static float   priceUntil = 25;
	 static float   priceFrom  = 2.50f;
	 
	 //LOGN PATTERNS
	 public static final String HARAMI      = "HARAMI";
	 public static final String ERROR       = "ERROR";
	 public static final String LOWVOLUME   = "LOWVOLUME";
	 public static final String PIERCING    = "PIERCING";
	 public static final String NOTHING     = "NOTHING";
	 public static final String MORNINGSTAR = "MORNINGSTAR";
	 
	 //SHORT PATTERNS
	 public static final String EVENINGSTAR = "EVENINGSTAR";
	 
	 
	public static void main(String[] args) throws Exception {
		
		//confirmations, put file path
		//confirmation("23_07_201812_06_10",HARAMI);
		
		if (findPatterns) {
			ArrayList<String>  stocks = getAllStocks(true);
			initiatePatternSearch(stocks);
		}	
//		System.out.println(findPatterns("NEXT"));
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
						//System.out.println(obj.getString("symbol"));
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
		String filePathToEveningStar = System.getProperty("user.dir")+"\\src\\test\\resources\\eveningstar\\" + fileName +".txt";
		String filePathToMorningStar = System.getProperty("user.dir")+"\\src\\test\\resources\\morningstar\\" + fileName +".txt";
	    String filePathToPiercing    = System.getProperty("user.dir")+"\\src\\test\\resources\\piercing\\" + fileName +".txt";
	    String filePathToHarami      = System.getProperty("user.dir")+"\\src\\test\\resources\\harami\\" + fileName +".txt";
	    String filePathToErrors      = System.getProperty("user.dir")+"\\src\\test\\resources\\error\\" + fileName +".txt";
	    
	    
	    //initiate writers
	    PrintWriter piercingWriter    = new PrintWriter(filePathToPiercing, "UTF-8");
	    PrintWriter haramiWriter      = new PrintWriter(filePathToHarami, "UTF-8");
	    PrintWriter errorWriter       = new PrintWriter(filePathToErrors, "UTF-8");
	    PrintWriter morningStarWriter = new PrintWriter(filePathToMorningStar, "UTF-8");
	    PrintWriter eveningStarWriter = new PrintWriter(filePathToEveningStar, "UTF-8");

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
			}		
		}
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
		JSONObject dailyStockInfo = new JSONObject();
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
				dailyStockInfo = arr.getJSONObject(0).getJSONObject("Time Series (Daily)");
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

		//sort the data (dates), and check volume info
		List<String> jsonValues = new ArrayList<String>();
		int volume = 0;
		int biggestVolume = 0;
	    for (int i = 0; i < dailyStockInfo.names().length(); i++) {
	    	jsonValues.add(((String)dailyStockInfo.names().get(i)));
	    	volume = dailyStockInfo.getJSONObject(jsonValues.get(i)).getInt("5. volume");
	    	if (biggestVolume < volume) {
	    		biggestVolume = volume;
	    	} 	
	    }
	    if (onlyHighVolume) {
	    	if (biggestVolume < 500000) {
	    		return LOWVOLUME;
	    	}
	    }
        Collections.sort(jsonValues);
        
        try {
        	   //get all needed data	    
    	    float lastDayClose    = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-1)).getFloat("4. close"); 
    	    float lastDayOpen     = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-1)).getFloat("1. open"); 
    	    float dayBeforeClose  = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-2)).getFloat("4. close");
    	    float dayBeforeOpen   = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-2)).getFloat("1. open"); 
    	    
    	    //filter those with not wanted prices
    	    if (lastDayClose < priceFrom || lastDayClose > priceUntil) {
    	    	return NOTHING;
    	    }
    	    
    	    //for morning and evening star
    	    float twoDaysBeforeOpen  = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-3)).getFloat("1. open");
    	    float twoDaysBeforeClose = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-3)).getFloat("4. close");
    	    
    	    if (checkForHarami(lastDayClose,lastDayOpen,dayBeforeClose,dayBeforeOpen))   return HARAMI;
    	    if (checkForPiercing(lastDayClose,lastDayOpen,dayBeforeClose,dayBeforeOpen)) return PIERCING;
    	    if (checkForMorningStar(lastDayClose,lastDayOpen,dayBeforeClose,dayBeforeOpen,twoDaysBeforeOpen,twoDaysBeforeClose)) return MORNINGSTAR;
    	    if (checkForEveningStar(lastDayClose,lastDayOpen,dayBeforeClose,dayBeforeOpen,twoDaysBeforeOpen,twoDaysBeforeClose)) return EVENINGSTAR;
    	    
        } catch (Exception e) {
        	 return NOTHING;
        }
     
        return NOTHING;	    
	}
	
	private static boolean checkForEveningStar(float lastDayClose,float lastDayOpen,float dayBeforeClose,float dayBeforeOpen,float twoDaysBeforeOpen,float twoDaysBeforeClose) {
		if (twoDaysBeforeOpen < twoDaysBeforeClose) { //2 days before, trend up
			if (dayBeforeClose > twoDaysBeforeClose && dayBeforeOpen > twoDaysBeforeClose ) { // the day before candle is above the one before
				if (lastDayOpen > lastDayClose) { //downtrend in the last day
					//calculate middle point of 2 days ago
					float middlePoint = (twoDaysBeforeOpen + ((twoDaysBeforeClose - twoDaysBeforeOpen))/2);
					if (lastDayClose < middlePoint ) {
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
	
	private static boolean checkForMorningStar(float lastDayClose,float lastDayOpen,float dayBeforeClose,float dayBeforeOpen,float twoDaysBeforeOpen,float twoDaysBeforeClose) {
		if (twoDaysBeforeOpen > twoDaysBeforeClose) { //2 days before, trend down
			if (dayBeforeClose < twoDaysBeforeClose && dayBeforeOpen < twoDaysBeforeClose ) { // the day before candle is below the one before
				if (lastDayOpen < lastDayClose) { //uptrend in the last day
					//calculate middle point of 2 days ago
					float middlePoint = (twoDaysBeforeOpen + ((twoDaysBeforeClose - twoDaysBeforeOpen))/2);
					if (lastDayClose > middlePoint ) {
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
	
	private static boolean checkForHarami(float lastDayClose,float lastDayOpen,float dayBeforeClose,float dayBeforeOpen) {		
	    if (dayBeforeClose < dayBeforeOpen) { //day before trend down
		   	 if (lastDayClose > lastDayOpen) {//last day trend up
		   		  if ((lastDayOpen > dayBeforeClose) && (lastDayClose < dayBeforeOpen)) {
		   			//let's make it much better
		   			return true; //Potential Harami
		   		  } 
		    } else {
		    	return false; //Continues down, nothing here
		    }
	   } else {
		   return false; //day before uptrend
	   }
	   return false;
	}
	
	private static boolean checkForPiercing(float lastDayClose,float lastDayOpen,float dayBeforeClose,float dayBeforeOpen) {
	    if (dayBeforeClose < dayBeforeOpen) { //day before trend down
	   	 if (lastDayClose > lastDayOpen) {//last day trend up
		    	if (lastDayOpen < dayBeforeClose) { //the open last day, is lower then close day before
		    		float middlePoint = (dayBeforeOpen + ((dayBeforeClose - dayBeforeOpen))/2);
		    		if (middlePoint < lastDayClose) { //confirm piercing
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
