package com.ht1.dexterity.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

import org.apache.commons.net.time.TimeTCPClient;
import org.apache.commons.net.time.TimeUDPClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class TimeWrapper {
	private static final String TAG = "tzachi";

	final String host = "time.nist.gov";

	// android was calling the following site, and was probably parsing it. through wireshark.
    // upoll.umengcloud.com:

	// standard method to get the time, but did not work on a non rooted lg5 for some reason
	// this is using port 37, which is not that standard. Packets were getting out, but did not seem to return.
    public final Long ntpTcpTime()
    {
    	Long time = 0l;
    	try {
            TimeTCPClient client = new TimeTCPClient();
            try {
                // We want to timeout if a response takes longer than 10 seconds
                client.setDefaultTimeout(10000);
                client.connect(host);
                //System.out.println(client.getDate());
                time = (client.getTime() - TimeTCPClient.SECONDS_1900_TO_1970) * 1000;
                Log.e(TAG,"Time returned by ntpTcpTime (in MS)" + time);  
            } finally {
                client.disconnect();
            }
        } catch (IOException ie) {
            Log.e(TAG, "ntpTcpTime - Error cought IOException when looking for time" + ExceptionToString(ie));
        }
        return time;
    }

    // This is working on http, but who knows when this server will be allive?
    public Long getYahooTime() {
    	
    	try{
            Log.e(TAG, "getTime starting 1");
            //Make the Http connection so we can retrieve the time
            HttpClient httpclient = new DefaultHttpClient();
            // I am using yahoos api to get the time
            HttpResponse response = httpclient.execute(new 
                    HttpGet("http://developer.yahooapis.com/TimeService/V1/getTime?appid=YahooDemo"));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                // The response is an xml file and i have stored it in a string
                String responseString = out.toString();
                Log.d(TAG, responseString);
                //We have to parse the xml file using any parser, but since i have to 
                //take just one value i have deviced a shortcut to retrieve it
                int x = responseString.indexOf("<Timestamp>");
                int y = responseString.indexOf("</Timestamp>");
                //I am using the x + "<Timestamp>" because x alone gives only the start value
                //Log.d(TAG, responseString.substring(x + "<Timestamp>".length(),y) );
                String timestamp =  responseString.substring(x + "<Timestamp>".length(),y);
                // The time returned is in UNIX format so i need to multiply it by 1000 to use it
                Long timestampL = Long.parseLong(timestamp) * 1000;
                Log.d(TAG, "getYahooTime returning "  + timestampL + " " + new Date(timestampL));
                return timestampL;
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        }catch (ClientProtocolException e) {
            Log.e(TAG, "Error cought ClientProtocolException when looking for time" + ExceptionToString(e));
        }catch (IOException e) {
            Log.e(TAG, "Error cought IOException when looking for time" + ExceptionToString(e));
        }

        return 0l;
    }
    Long getTime() {
        Long ret = new Date().getTime();
    	try {
            if (ret < 1357688496000l) {
                // This is before 2013, the system time is wrong. let's do two tries to get a better time...
                Log.e(TAG, "Failed to get time through normal method, trying the network");

                ret = ntpTcpTime();
                if (ret < 1357688496000l) {
                    ret  = getYahooTime();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cought exception when looking for time" + ExceptionToString(e));
        }
        if (ret < 1357688496000l) {
            // nothing worked, we return to the system method. at least this clock will move forward.
            ret = new Date().getTime();
        }
        return ret;
    }

    // TODO: Move to some general util function
    static public String ExceptionToString(Exception e) {
        String theTrace = new String("Cought Exception + " + e.getMessage() + "\n");
        for(StackTraceElement line : e.getStackTrace())
        {
            theTrace += line.toString() + "\n";
        }
        return theTrace;
    }
}
