package com.ht1.dexterity.app;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.io.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import static java.lang.Math.*;

//import com.nightscout.android.dexcom.DataStore;
//import com.nightscout.android.dexcom.DexcomG4Activity;
//import com.nightscout.android.dexcom.EGVRecord;
// This class is a server that is listening to connections on the WIFI network and sends them data as expected.
//
// The protocol of the data should be: 1 recieve a command of the desired data.
// 2) the server sends the data and closes the connection. the connection will be serielized EGV records.
public class ServerSockets  extends Thread {
	// TODO: rename class...

    static public String mDebugString;
    private final String TAG = "ServerSockets";

    private volatile boolean mStop = false;
    
    public final static int PORT = 50005 ; // will have to be configurable at last...
    
    private final Context mContext;
	   
    public ServerSockets(Context ctx) throws IOException
    {
        mContext = ctx.getApplicationContext(); 
    }

    void PrintSocketStatus(String Str) 
    {
    	// The two lines below make debugging easy but consume infinite memory...
        // mDebugString += Str;
        // mContext.sendBroadcast(new Intent("NEW_PRINT"));
        Log.i(TAG, Str);
    }

    public void Stop()
    {
        mStop = true;
    }
   
    public void run()
    {
        ServerSocket ServerSocket = null;
        boolean closeSocket = false;
        int listenOnPort = -1;
        
        Log.e("tzachi", "Thread starting");
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        while(!mStop) {
            try {
                int currentRefsPort = Integer.parseInt( preferences.getString("portNumber", "50005"));
                if (listenOnPort != currentRefsPort) {
                    if(closeSocket) {
                        ServerSocket.close();
                        closeSocket = false;
                        listenOnPort = -1;
                    }
                    
                    ServerSocket = new ServerSocket(currentRefsPort);
                    // Socket was opened successfully, update parameters...
                    listenOnPort = currentRefsPort;
                    closeSocket = true;
                }
                
                ServerSocket.setSoTimeout(2000);
                Socket clientSocket = ServerSocket.accept();
                ClientWorker worker = new ClientWorker(clientSocket);
                Thread t = new Thread(worker);
                t.start();

                
            } catch(SocketTimeoutException s) {
                // We are not printing, since this can happen all the time
                //        PrintSocketStatus("Socket timed out(server)!, trying again...");
            } catch (SocketException e) {
                String stackTrace = Log.getStackTraceString(e);
                PrintSocketStatus("cought SocketException!, trying again..."  + e + " " + stackTrace);
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                continue;
                
            } catch (IOException e) {
                String stackTrace = Log.getStackTraceString(e);
                PrintSocketStatus("cought IOException!, trying again " + e + " " + stackTrace);
                
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                continue;
            } 
            catch (Exception e) {
                String stackTrace = Log.getStackTraceString(e);
                PrintSocketStatus("cought Excption!, trying again... " + e + " " + stackTrace);
            }
                
        }
        try {
            if (closeSocket) {
                ServerSocket.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
            
        
    }
    class ClientWorker implements Runnable {
        private Socket m_clientSocket;
    
        ClientWorker(Socket client) {
            this.m_clientSocket = client;
        }
        public void run(){
            Gson gson = new GsonBuilder().create();
            ComunicationHeader ch;

             Log.i(TAG, "ServerSockets: client thread starting...");

            try {
                m_clientSocket.setSoTimeout(4000);
                PrintSocketStatus("got connection from " + m_clientSocket.getRemoteSocketAddress() );
    
                PrintWriter out = new PrintWriter(m_clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader( new InputStreamReader(m_clientSocket.getInputStream()));
                
                

                String inputLine, outputLine;

                inputLine = in.readLine();
                Log.i(TAG, "Recieved the line " + inputLine);
                if(inputLine == null) {
                    PrintSocketStatus("Unexpected null value...\n");
                    m_clientSocket.close();
                    return;
                }
                ch = gson.fromJson(inputLine, ComunicationHeader.class);                   
                  
                if(ch.version != 1) {
                    PrintSocketStatus("Unexpected version...\n" + ch.version);
                    m_clientSocket.close();
                    return;
                }
                PrintSocketStatus("Recieved the line + correct version " + inputLine);

                // Get all the data that is currently stored
                DexterityDataSource source = new DexterityDataSource(mContext);
                PrintSocketStatus("source = " + source);
                List<TransmitterRawData> rawDataList = source.getAllDataObjects(false, false, ch.numberOfRecords);

                PrintSocketStatus("List size is " + rawDataList.size());
                source.close();

                int objectsToReturn = min(rawDataList.size(), ch.numberOfRecords);
                for (ListIterator<TransmitterRawData> iter = rawDataList.listIterator(); objectsToReturn > 0; ) {
                    TransmitterRawData element = iter.next();
                    // set the relative time
                    element.RelativeTime = new TimeWrapper().getTime() - element.CaptureDateTime;
                    
                    if (element.RelativeTime <= 0) {
                    	// we will remove objects that are in the future, this can happen in casses of errors with clock.
                    	// we will not send it, and the list will be shorter... 
                    	Log.e(TAG, "Error throwing objects with negative time");
                    } else {
                        outputLine = gson.toJson(element);
                    	out.println(outputLine);
                    }
                    objectsToReturn--;
                }
                // Mark that we have finished sending.
                out.println("");
        
                PrintSocketStatus("Data send, closing socket soon......");
                
            }catch(SocketTimeoutException s) {
        //        PrintSocketStatus("Socket timed out(server)!, trying again...");
            }catch(IOException e) {
                String stackTrace = Log.getStackTraceString(e);
                PrintSocketStatus("cought IOException!, trying again..." + e + " " + stackTrace);
            }
            catch (JsonSyntaxException je) {
                String stackTrace = Log.getStackTraceString(je);
                PrintSocketStatus("cought JsonSyntaxException!, trying again..."  + je + " " + stackTrace );
            }
            catch (Exception e) {
                String stackTrace = Log.getStackTraceString(e);
                PrintSocketStatus("cought Excption!, trying again... " + e + " " + stackTrace);
            }
            try {
                m_clientSocket.close();
            } catch (IOException e) {
                String stackTrace = Log.getStackTraceString(e);
                PrintSocketStatus("cought Excption!, when closing socket trying again... "  + e + " " + stackTrace);
            }
        }

    }

}