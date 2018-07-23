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

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

public class Main {

	 static	SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyyHH_mm_ss");  
	 static Date date = new Date(); 
	 
	 static boolean findPatterns = true;
	 static boolean onlyHighVolume = true;
	 
	 public static final String HARAMI      = "HARAMI";
	 public static final String ERROR       = "ERROR";
	 public static final String LOWVOLUME   = "LOWVOLUME";
	 public static final String PIERCING    = "PIERCING";
	 public static final String NOTHING     = "NOTHING";
	 public static final String MORNINGSTAR = "MORNINGSTAR";
	 
	public static void main(String[] args) throws Exception {
		
		//confirmations, put file path
		//confirmation("23_07_201810_25_22",PIERCING);
		
		if (findPatterns) {
			ArrayList<Stock>  stocks = getAllStocks();
			System.out.println(stocks);
			initiatePatternSearch(stocks);
		}		   
	}
	
	private static void initiatePatternSearch(ArrayList<Stock> stocks) throws Exception {
		System.out.println("program started");
		String fileName = formatter.format(date);
	    String filePathToPiercing = System.getProperty("user.dir")+"\\src\\test\\resources\\piercing\\" + fileName +".txt";
	    String filePathToHarami   = System.getProperty("user.dir")+"\\src\\test\\resources\\harami\\" + fileName +".txt";
	    String filePathToErrors   = System.getProperty("user.dir")+"\\src\\test\\resources\\error\\" + fileName +".txt";
	    PrintWriter piercingWriter = new PrintWriter(filePathToPiercing, "UTF-8");
	    PrintWriter haramiWriter   = new PrintWriter(filePathToHarami, "UTF-8");
	    PrintWriter errorWriter    = new PrintWriter(filePathToErrors, "UTF-8");

		int stockNum = 0;	
		String symbol;
		String patternFound;
		for (Stock s : stocks) {
			if (s.type.equals("cs") || s.type.equals("et")) {
				System.out.println(++stockNum + ". Checking " + s.symbol);
				symbol = s.symbol.replaceAll("[^A-Za-z0-9()\\[\\]]", "");
				patternFound = findPatterns(symbol);
				if (patternFound.equals(ERROR)) {
					System.out.println("Found ERROR in stock " + symbol);
					errorWriter.println(symbol);
					haramiWriter.flush();
				} else if (patternFound.equals(HARAMI)) {
					System.out.println("Found HARAMI in stock " + symbol);
					haramiWriter.println(symbol);
					haramiWriter.flush();
				} else if (patternFound.equals(PIERCING)) {
					System.out.println("Found PIERCING in stock " + symbol);
					piercingWriter.println(symbol);
					piercingWriter.flush();
				} else if (patternFound.equals(LOWVOLUME)) {
					//MAYBE I WILL DO SOMETHING WITH THIS LATER
				}
			}		
		}
		piercingWriter.close();
		haramiWriter.close();
		errorWriter.close();
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
        
        //get all needed data	    
	    float lastDayClose = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-1)).getFloat("4. close"); 
	    float lastDayOpen = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-1)).getFloat("1. open"); 
	    float dayBeforeClose = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-2)).getFloat("4. close");
	    float dayBeforeOpen = dailyStockInfo.getJSONObject(jsonValues.get(jsonValues.size()-2)).getFloat("1. open"); 
	    
	    if (checkForHarami(lastDayClose,lastDayOpen,dayBeforeClose,dayBeforeOpen))   return HARAMI;
	    if (checkForPiercing(lastDayClose,lastDayOpen,dayBeforeClose,dayBeforeOpen)) return PIERCING;
	    return NOTHING;
	    
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
	
	
	private static ArrayList<Stock> getAllStocks(){
		ArrayList<Stock> stocks = new ArrayList<>();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			stocks = objectMapper
					 .readValue(Unirest.get("https://api.iextrading.com/1.0/ref-data/symbols")
					 .asJson()
					 .getRawBody(),new TypeReference<List<Stock>>(){});
		} catch (Exception e) {
			System.exit(0);
		}
		return stocks;
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
