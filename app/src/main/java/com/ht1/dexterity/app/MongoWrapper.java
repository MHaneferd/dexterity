package com.ht1.dexterity.app;

import java.io.IOException;
import java.net.UnknownHostException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoClientURI;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.util.Log;



import org.apache.http.HttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

import java.io.UnsupportedEncodingException;

public class MongoWrapper {
	
	private static final String TAG = "tzachi";
	
/*
 old start code 
	
public static void main(String[] args) {
    MongoClientURI dbUri = new MongoClientURI("mongodb://tzachi_dar:tzachi_dar@ds053958.mongolab.com:53958/nightscout");
    System.out.println( "Starting");
    try {
        
        MongoClient mongoClient = new MongoClient(dbUri);
        DB db = mongoClient.getDB( "nightscout" );
        
        DBCollection coll = db.getCollection("try1");
        
        coll.createIndex(new BasicDBObject("i", 1));  // create index on "i", ascending

        
        for (int i =0; i < 10; i++) {
            BasicDBObject doc = new BasicDBObject("name", "MongoDB")
            .append("type", "database")
            .append("count", i);
            
            coll.insert(doc);
        }

        
        
        DBCursor cursor = coll.find();
        try {
           while(cursor.hasNext()) {
               System.out.println(cursor.next());
           }
        } finally {
           cursor.close();
        }
        
        System.out.println("Now with index...");
        System.out.println("===================");
        
        DBObject query = new BasicDBObject();
        cursor = coll.find(query);
        cursor.sort(new BasicDBObject("count", -1));
        try {
            while(cursor.hasNext()) {
                System.out.println(cursor.next());
            }
         } finally {
            cursor.close();
         }
        
        // now get the numbers that are bigger than 7
        System.out.println("Now the numbers bigger than 7");
        System.out.println("===================");
        
        query = new BasicDBObject(new BasicDBObject("count", new BasicDBObject("$gt", 7)));
        cursor = coll.find(query);
        cursor.sort(new BasicDBObject("count", 1));
        try {
            while(cursor.hasNext()) {
                System.out.println(cursor.next());
            }
         } finally {
            cursor.close();
         }        
        
        
        
        //        Set<String> colls = db.getCollectionNames();

//        for (String s : colls) {
//            System.out.println(s);
//        }
    } catch (UnknownHostException e) {
        //throw new IOException("Error connecting to mongo host " + dbUri, e);
        System.out.println( "Failed to open table");
    }
      
    }

*/


	MongoClient mongoClient_;
	String dbUriStr_;
	String dbName_;
	String collection_;
	String index_;
	String machineName_;
	
	// rest parameters
    String APIKEY = "D2a6iaurh-oihXrraOquZSySx9QnT_Gs";
	String DBNAMEREST = "nightscout";

	private static final String UPSERT = "&u=true";
    private static final int SOCKET_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final String BASE_URL = "https://api.mongolab.com/api/1/databases/";

    private final boolean do_mongo = false;
    private final boolean do_rest = true;
    
	 public boolean sendToMongo(String dbName, String apiKey, String collectionName, String jsonString) {
	     Log.d(TAG, "sendToMongo " + jsonString);
	     String url = BASE_URL + dbName + "/collections/" + collectionName + "?apiKey=" + apiKey + UPSERT;
	     boolean success = false;
         try {
             HttpParams params = new BasicHttpParams();
             HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
             HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
             DefaultHttpClient httpclient = new DefaultHttpClient(params);
             HttpPost post = new HttpPost(url);
             //String jsonString = json.toString();
             StringEntity se = new StringEntity(jsonString);
             post.setEntity(se);
             //post.setHeader("Accept", "application/json");
		     post.setHeader("Content-type", "application/json");
		     HttpResponse response = httpclient.execute(post);
		     Log.d(TAG, "Send returned code is " + response.getStatusLine().getStatusCode());
		     if( response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201) {
		    	 success  = true;
		     }
		 } catch (ClientProtocolException e) {
		     Log.e(TAG, "sendToMongo ClientProtocolException: ", e);
		     return false;
		 } catch (UnsupportedEncodingException e) {
		     Log.e(TAG, "sendToMongo UnsupportedEncodingException: ", e);
		     return false;
		 } catch (IOException e) {
		     Log.e(TAG, "sendToMongo IOException: ", e);
		     return false;
		 } catch (Exception e) {
		     Log.e(TAG, "sendToMongo Exception: ", e);
		     return false;
		 }
         
	     return success;
	 }	
	
	
	public MongoWrapper(String dbUriStr, String collection, String index, String machineName) {
		dbUriStr_ = dbUriStr;
		// dbName is the last part of the string starting with /dbname
		dbName_ = dbUriStr.substring(dbUriStr.lastIndexOf('/') + 1);
		collection_ = collection;
		index_ = index;
		machineName_ = machineName;
	}
	
	// Unfortunately, this also throws other exceptions that are not documetned...
    private DBCollection openMongoDb() throws UnknownHostException {

    	if (!do_rest) {
	    	MongoClientURI dbUri = new MongoClientURI(dbUriStr_); //?? thros
		    mongoClient_ = new MongoClient(dbUri);
		    
		    DB db = mongoClient_.getDB( dbName_ );
		    DBCollection coll = db.getCollection(collection_);
		    coll.createIndex(new BasicDBObject(index_, 1));  // create index on "i", ascending
		    
		    return coll;
    	}
    	return null;
	 
    }
     
     public void closeMongoDb() {
         if(mongoClient_ != null) {
    	 	mongoClient_.close();
         }
     }

 	class DebugMessage {
		 String DebugMessage;
		 long CaptureDateTime;
	}
     
     public boolean WriteDebugDataToMongo(String message)
     {
    	 Long CaptureDateTime = new TimeWrapper().getTime();
    	 
    	 SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    	 String time = sdf.format(new Date(CaptureDateTime));
    	 String complete = machineName_ + " " +time + " " + message;
    	 
    	 if (do_rest) {
    		Gson gson = new GsonBuilder().create();
    		 
    		DebugMessage debug_message = new DebugMessage();
    		debug_message.CaptureDateTime = CaptureDateTime;
    		debug_message.DebugMessage = complete;
    		
    	    String flat = gson.toJson(debug_message);
	        return sendToMongo(DBNAMEREST, APIKEY , "SnirData", flat);
    	 } else {
	    	 BasicDBObject doc = new BasicDBObject("DebugMessage", complete).append("CaptureDateTime", CaptureDateTime);
	    	 return WriteToMongo(doc);    		 
    	 }
     }

     
     public boolean WriteToMongo(TransmitterRawData trd)
     {
    	 SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    	 String time = sdf.format(new Date(trd.CaptureDateTime));
    	 String DebugInfo = machineName_ + " " + time;
    	 if(do_rest) {
	        
    		
    		Gson gson = new Gson();
    		JsonElement jsonElement = gson.toJsonTree(trd);
    		jsonElement.getAsJsonObject().addProperty("DebugInfo", DebugInfo);
    		String flat = gson.toJson(jsonElement);
    		
	        return sendToMongo(DBNAMEREST, APIKEY , "SnirData", flat);
    	 } else {
    		 BasicDBObject bdbo = trd.toDbObj(DebugInfo);
    		 return WriteToMongo(bdbo);    		 
    	 }
     }
     
     public boolean WriteToMongo(BasicDBObject bdbo)
     {
     	DBCollection coll;
     	try {
     		coll = openMongoDb();
         	coll.insert(bdbo);

 		} catch (UnknownHostException e) {
 			e.printStackTrace();
 			return false; 
 		} catch (MongoException e) {
 			e.printStackTrace();
 			return false; 
 		} catch (Exception e) {
 			e.printStackTrace();
 			closeMongoDb();
 			return false; 
 		}
     	finally {
 			closeMongoDb();
 		}
     	return true;
     }
     
     // records will be marked by their timestamp
     public List<TransmitterRawData> ReadFromMongo(int numberOfRecords) {
    	System.out.println( "Starting to read from mongodb"); 
    	 
    	List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
    	/*
        This function can not compile here and is not used in this project...?s
      	DBCollection coll;
      	TransmitterRawData lastTrd = null;
      	try {
      		coll = openMongoDb();
      		DBCursor cursor = coll.find();
            cursor.sort(new BasicDBObject("CaptureDateTime", -1));
            try {
                while(cursor.hasNext() && trd_list.size() < numberOfRecords) {
                    //System.out.println(cursor.next());
                    TransmitterRawData trd = new TransmitterRawData((BasicDBObject)cursor.next());
                    // Do our best to fix the relative time...
                    trd.RelativeTime = new Date().getTime() - trd.CaptureDateTime;
                    // since we are reading it from the db, it was uploaded...
                    trd.Uploaded = 1;
                    if(lastTrd == null) {
                    	trd_list.add(0,trd);
                    	lastTrd = trd;
                    	System.out.println( trd.toTableString());
                    } else if(!ReadData.almostEquals(lastTrd, trd)) {
                    	lastTrd = trd;
                    	trd_list.add(0,trd);
                    	System.out.println( trd.toTableString());
                    }
                    
                }
             } finally {
                cursor.close();
             }

  		} catch (UnknownHostException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  			return null; 
  		} catch (MongoException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  			return trd_list; 
  		} catch (Exception e) {
 			e.printStackTrace();
 			closeMongoDb();
 			return null; 
 		}finally {
  			closeMongoDb();
  		}
  		*/
      	return trd_list;
    	 
     }


}
