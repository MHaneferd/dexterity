import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
//import com.nightscout.android.dexcom.ComunicationHeader;
//import com.nightscout.android.dexcom.SyncingService;
//import com.nightscout.android.dexcom.TransmitterRawData;


class Log {
	static public void e(String tag, String data) {
	}
	static public void i(String tag, String data) {
	}
}

class ReadData {
    
    private final static String TAG = ReadData.class.getName();

    
    
    public static void main(String[] args) {
    	
    	//CheckMongo();
        Read(args[0], Integer.parseInt(args[1]));
        return;
        
/*        
        int numberOfRecords = 5;
        if(args.length >= 1) {
            numberOfRecords = Integer.parseInt(args[0]);
        }
        Read("192.168.1.13", 50005, numberOfRecords);
*/
    }


    public static boolean ReadOld(String hostName,int port, int numberOfRecords)
    {
        try
        {

            Gson gson = new GsonBuilder().create();

            // An example of using gson.
            ComunicationHeader ch = new ComunicationHeader();
            ch.version = 1;
            ch.numberOfRecords = numberOfRecords;
            String flat = gson.toJson(ch);
            ComunicationHeader ch2 = gson.fromJson(flat, ComunicationHeader.class);  
            System.out.println("Results code" + flat + ch2.version);


            // Real client code
            Socket MySocket = new Socket(hostName, port);

            System.out.println("After the new socket \n");
            MySocket.setSoTimeout(5000); 
                     
            System.out.println("client connected... " );
            
            PrintWriter out = new PrintWriter(MySocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(MySocket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            out.println(flat);
            
            while(true) {
                String data = in.readLine();
                if(data == null) {
                    System.out.println("recieved null exiting");
                    break;
                }
                if(data.equals("")) {
                    System.out.println("recieved \"\" exiting");
                    break;
                }

                System.out.println( "data size " +data.length() + " data = "+ data);
                TransmitterRawData trd = gson.fromJson(data, TransmitterRawData.class);
                System.out.println( trd.toTableString());
            }


            MySocket.close();
            return true;
        }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!...");
        }
        catch(IOException e) {
            e.printStackTrace();
            System.out.println("cought exception " + e.getMessage());
        }
        return false;
   }
    public static boolean almostEquals( TransmitterRawData e1, TransmitterRawData e2) 
    {
        // relative time is in ms
        if ((Math.abs(e1.RelativeTime - e2.RelativeTime) < 120 * 1000 ) &&
                (e1.TransmissionId == e2.TransmissionId)) {
            return true;
        }
        return false;
    }
    
 // last in the array, is first in time
    public static List<TransmitterRawData> Merge2Lists(List<TransmitterRawData> list1 , List<TransmitterRawData> list2)
    {
        List<TransmitterRawData> merged = new LinkedList <TransmitterRawData>();
        while (true) {
            if(list1.size() == 0 && list2.size() == 0) { 
                break;
            }
            if (list1.size() == 0) {
                merged.addAll(list2);
                break;
            }
            if (list2.size() == 0) {
                merged.addAll(list1);
                break;
            }
            if (almostEquals(list1.get(0), list2.get(0))) {
                list2.remove(0);
                merged.add(list1.remove(0));
                continue;
            }
            if(list1.get(0).RelativeTime > list2.get(0).RelativeTime) {
                merged.add(list1.remove(0));
            } else {
                merged.add(list2.remove(0));
            }

        }
        return merged;
    }
    
    public static List<TransmitterRawData> MergeLists(List <List<TransmitterRawData>> allTransmitterRawData)
    {
        List<TransmitterRawData> MergedList;
        MergedList = allTransmitterRawData.remove(0);
        for (List<TransmitterRawData> it : allTransmitterRawData) {
            MergedList = Merge2Lists(MergedList, it);
        }
        
        return MergedList;
    }
            
    public static List<TransmitterRawData> ReadHost(String hostAndIp, int numberOfRecords)
    {
        int port;
        System.out.println("Reading From " + hostAndIp);
        Log.i(TAG,"Reading From " + hostAndIp);
        String []hosts = hostAndIp.split(":");
        if(hosts.length != 2) {
            System.out.println("Invalid hostAndIp " + hostAndIp);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);
            
            return null;
        }
        try {
            port = Integer.parseInt(hosts[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid port " +hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);
            return null;
            
        }
        if (port < 10 || port > 65536) {
            System.out.println("Invalid port " +hosts[1]);
            Log.e(TAG, "Invalid hostAndIp " + hostAndIp);
            return null;
            
        }
        System.out.println("Reading from " + hosts[0] + " " + port);
        List<TransmitterRawData> ret;
        try {
            ret = Read(hosts[0], port, numberOfRecords);
        } catch(Exception e) {
            // We had some error, need to move on...
            System.out.println("read from host failed cought expation" + hostAndIp);
            Log.e(TAG, "read from host failed " + hostAndIp);

            return null;
            
        }
        return ret;
    }
    
    public static List<TransmitterRawData> ReadFromMongo(String dbury, int numberOfRecords)
    {
    	List<TransmitterRawData> tmpList;
    	// format is dburi/db/collection. We need to find the collection and strip it from the dburi.
    	int indexOfSlash = dbury.lastIndexOf('/');
    	if(indexOfSlash == -1) {
    		// We can not find a collection name
    		Log.e(TAG, "Error bad dburi. Did not find a collection name starting with / " + dbury);
    		// in order for the user to understand that there is a problem, we return null
    		return null;
    		
    	}
    	String collection = dbury.substring(indexOfSlash + 1);
    	dbury = dbury.substring(0, indexOfSlash);
    	
    	// Make sure that we have another /, since this is used in the constructor.
    	indexOfSlash = dbury.lastIndexOf('/');
    	if(indexOfSlash == -1) {
    		// We can not find a collection name
    		Log.e(TAG, "Error bad dburi. Did not find a collection name starting with / " + dbury);
    		// in order for the user to understand that there is a problem, we return null
    		return null;
    	}
    	
        //MongoWrapper mt = new MongoWrapper("mongodb://tzachi_dar:tzachi_dar@ds053958.mongolab.com:53958/nightscout", "SnirData", "CaptureDateTime", "MachineNameNotUsed");
    	MongoWrapper mt = new MongoWrapper(dbury, collection, "CaptureDateTime", "MachineNameNotUsed");
    	return mt.ReadFromMongo(numberOfRecords);
    }
    
    // format of string is ip1:port1,ip2:port2;
    public static TransmitterRawData[] Read(String hostsNames, int numberOfRecords)
    {
        String []hosts = hostsNames.split(",");
        if(hosts.length == 0) {
            Log.e(TAG, "Error no hosts were found " + hostsNames);
            return null;
        }
        List <List<TransmitterRawData>> allTransmitterRawData =  new LinkedList <List<TransmitterRawData>>();
        
        // go over all hosts and read data from them
        for(String host : hosts) {
        	
            List<TransmitterRawData> tmpList;
            if (host.startsWith("mongodb://")) {
            	tmpList = ReadFromMongo(host ,numberOfRecords);
            } else {
            	tmpList = ReadHost(host, numberOfRecords);            	
            }
            if(tmpList != null && tmpList.size() > 0) {
                allTransmitterRawData.add(tmpList);
            }
        }
        // merge the information
        if (allTransmitterRawData.size() == 0) {
            System.out.println("Could not read anything from " + hostsNames);
            Log.e(TAG, "Could not read anything from " + hostsNames);
            return null;

        }
        List<TransmitterRawData> mergedData= MergeLists(allTransmitterRawData);
        
        int retSize = Math.min(numberOfRecords, mergedData.size());
        TransmitterRawData []trd_array = new TransmitterRawData[retSize];
        mergedData.subList(mergedData.size() - retSize, mergedData.size()).toArray(trd_array);
        
        System.out.println("Final Results========================================================================");
        for (int i= 0; i < trd_array.length; i++) {
            System.out.println( trd_array[i].toTableString());
        }
        return trd_array;
        
    }
    
    public static List<TransmitterRawData> Read(String hostName,int port, int numberOfRecords)
    {
        List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
        try
        {
            Log.i(TAG, "Read called");
            Gson gson = new GsonBuilder().create();

            // An example of using gson.
            ComunicationHeader ch = new ComunicationHeader();
            ch.version = 1;
            ch.numberOfRecords = numberOfRecords;
            String flat = gson.toJson(ch);
            ComunicationHeader ch2 = gson.fromJson(flat, ComunicationHeader.class);  
            System.out.println("Results code" + flat + ch2.version);


            // Real client code
            InetSocketAddress ServerAdress = new InetSocketAddress(hostName, port);
            Socket MySocket = new Socket();
            MySocket.connect(ServerAdress, 10000);

            System.out.println("After the new socket \n");
            MySocket.setSoTimeout(2000); 
                     
            System.out.println("client connected... " );
            
            PrintWriter out = new PrintWriter(MySocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(MySocket.getInputStream()));

            out.println(flat);
            
            while(true) {
                String data = in.readLine();
                if(data == null) {
                    System.out.println("recieved null exiting");
                    break;
                }
                if(data.equals("")) {
                    System.out.println("recieved \"\" exiting");
                    break;
                }

                System.out.println( "data size " +data.length() + " data = "+ data);
                TransmitterRawData trd = gson.fromJson(data, TransmitterRawData.class);
                trd.CaptureDateTime = System.currentTimeMillis() - trd.RelativeTime;

                trd_list.add(0,trd);
                System.out.println( trd.toTableString());
                if(trd_list.size() == numberOfRecords) {
                	// We have the data we want, let's get out
                	break;
                }
            }


            MySocket.close();
            return trd_list;
        }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!...");
        }
        catch(IOException e) {
            e.printStackTrace();
            System.out.println("cought exception " + e.getMessage());
        }
        return trd_list;
    }
    public static void CheckMongo() {
        MongoWrapper mt = new MongoWrapper("mongodb://tzachi_dar:tzachi_dar@ds053958.mongolab.com:53958/nightscout", "SnirData", "CaptureDateTime", "NoMachineName");
//        mt.WriteToMongo(trd);
        mt.ReadFromMongo(20);
    }
 
    
}