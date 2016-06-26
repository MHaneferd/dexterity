
package com.ht1.dexterity.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by John Costik on 6/7/14.
 */
public class SerialPortReader
{
	private Thread mThread = null;
	private Context mContext = null;
	private static SerialPortReader _instance;
	private String mError = "";
	private static final int  MAX_RECORDS_TO_UPLOAD = 6;
    private boolean mStop = false;
    static private long mLastDbWriteTime = 0;
    private final static String TAG = "tzachi";

	// private constructor so can only be instantiated from static member
	private SerialPortReader(Context context)
	{
		mContext = context;
	}

	private void SetContext(Context context)
	{
		mContext = context;
	}

	public static SerialPortReader getInstance(Context context)
	{
		if (_instance == null)
		{
			_instance = new SerialPortReader(context);
		}

		// set service.  This survives change in service, since this is a static member
		_instance.SetContext(context);
		return _instance;
	}

	public void StartThread()
   	{
        // prevent any pending stop
        mStop = false;

        if (mThread == null)
		{
			mThread = new Thread(mMainRunLoop);
            mThread.start();
		}
	}

	public void StopThread()
	{
	    Log.w(TAG, "SerialPortReader StopThread Called");
		if(mThread != null)
			mStop = true;
			}

	public String getErrorString()
	{
		return mError;
	}

	public void ShowToast(final String toast)
	{
		// push a notification rather than toast.
	    Log.w(TAG, "SerialPortReader ShowToast Called " + toast);
		NotificationManager NM = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification.Builder(mContext)
				.setContentTitle("Dexterity receiver")
				.setContentText(toast)
				.setTicker(toast)
				.setSmallIcon(mContext.getResources().getIdentifier("ic_launcher", "drawable", mContext.getPackageName()))
				.build();

		NM.notify(R.string.notification_ReceiverAttached, n);
	}

	private Runnable mMainRunLoop = new Runnable()
	{
        private void done()
        {
            // clear object in parent so that the thread can be restarted if required
            ShowToast("USB port closed / suspended");
            mThread = null;
            mContext.sendBroadcast(new Intent("USB_DISCONNECT"));
            mStop = false;
        }

		@Override
		public void run()
		{
			NotifyAliveIfNeeded();
		    try {
    		    Log.w(TAG, "SerialPortReader run called ");
    			Looper.prepare();
    
                UsbManager manager = (UsbManager)mContext.getSystemService(mContext.USB_SERVICE);
                if (manager == null)
                {
                    SetError("Failed to obtain USB device manager");
                    done();
                    return;
                }
                UsbSerialDriver SerialPort = UsbSerialProber.findFirstDevice(manager);
                if (SerialPort == null)
                {
                    SetError("Failed to obtain serial device");
                    done();
                    return;
                }
    
                try
    			{
                    SerialPort.open();
    			}
    			catch (IOException e)
    			{
    				SetError(e.getLocalizedMessage());
    				e.printStackTrace();
                    done();
    				return;
    			}
    
    			try
    			{
                    ShowToast("Reading from USB port");
                    mContext.sendBroadcast(new Intent("USB_CONNECT"));
    
    				byte[] rbuf = new byte[4096];
    
    				while(!mStop)
    				{
    					// this is the main loop for transferring
    					try
    					{
                            //??????????Log.i(TAG, "Reading the wixel...");
    						// read aborts when the device is disconnected
    						long Start = new Date().getTime();
                            int len = SerialPort.read(rbuf, 30000);
                            long End = new Date().getTime();
                            if (End - Start > 60000) {
                            	Log.wtf(TAG, "Read took " + (End-Start) + " Instead of 30,000 Start = " + Start);
                            }
                            if (len > 0)
                            {
                                rbuf[len] = 0;
                                {
                                	// Log the debug data
                                	String debugString = new String(rbuf, 0 , len);
                                	Log.e(TAG,"NEWSTRING" + debugString);
                                	String lines[] = debugString.split("\\r?\\n");
                                	for(String s : lines) {
                                		Log.e(TAG,"NEWSTRING2" + s);
                                	}
                                }
                                Log.i(TAG, "Reading we have new data... ignored");
                                //setSerialDataToTransmitterRawData(rbuf, len);
                            }
                        }
    					catch (IOException e)
    					{
    						//Not a G4 Packet?
                            ShowToast("Worker thread IOException: " + e.hashCode());
                            // abort this thread
                            Log.e(TAG, "cought io exception WTF ???????????/ why are we getting out ?????????");
                            mStop = true;
    						e.printStackTrace();
    					}
    					NotifyAliveIfNeeded();
    				}
    
    				// do this last as it can throw
                    SerialPort.close();
    				Log.i(TAG, "SerialPortReader mStop is true, stopping read process");
    			}
    			catch (IOException e)
    			{
    				Log.e(TAG,"SerialPortReader cought io exception");
    				e.printStackTrace();
    			}
                done();
		    }
		    finally {
		      Log.e(TAG, "SerialPortReader We are leaving the SerialPortReader run do we know why???? ");
		    }
		}
	};

	private void SetError(String sError)
	{
		mError = sError;
		ShowToast(mError);
		Log.e(TAG, mError);
	}

	
	static private MongoWrapper CreateMongoWrapper(Context context)
	{
		// Create the mongo writer
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String MachineName = preferences.getString("machineName", "MachineUnknown");
        String dbUri = preferences.getString("dbUri", "mongodb://tzachi_dar:tzachi@ds053958.mongolab.com:53958/nightscout");
        
        MongoWrapper mt = new MongoWrapper(dbUri, "SnirData", "CaptureDateTime", MachineName);
        return mt;
	}

	private void NotifyAliveIfNeeded() 
	{
		if (new Date().getTime() - mLastDbWriteTime > 330000) {
		    MongoWrapper mt = CreateMongoWrapper(mContext);
		    boolean WritenToDb = mt.WriteDebugDataToMongo("Allive, usb connected to wixler");
	        if(WritenToDb) {
	        	Log.e(TAG,"Writing to mongodb that I'm alive... ");
	        	mLastDbWriteTime = new Date().getTime();
	        }
		}
	}
	
	private void setSerialDataToTransmitterRawData(byte[] buffer, int len){
		TransmitterRawData trd = new TransmitterRawData(buffer, len, mContext);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String transmitter_id = preferences.getString("transmitter_id", "6ABW4");

        if(trd.TransmitterId.equals(transmitter_id) || transmitter_id.equals("0")  || transmitter_id.length() ==0) {
    		setSerialDataToTransmitterRawData(mContext, trd);
        	return;
        }
    	Log.e(TAG,"Throwing away packet with wrong tramsission id. transmitter_id= "+transmitter_id + "object =" + trd.toTableString());
        
	}
	
	static public void setSerialDataToTransmitterRawData(Context context, TransmitterRawData trd)
	{
		boolean WritenToDb = false;

		DexterityDataSource source = new DexterityDataSource(context);
		trd = source.createRawDataEntry(trd);
		Log.e(TAG,"Just created a TRD, " + trd.TransmissionId + " CaptureDateTime " + trd.CaptureDateTime);
		List<TransmitterRawData> retryList = source.getAllDataObjects(true, true, 10000);
		
		Log.e(TAG, "retry list size is "+ retryList.size());
		

        // we got the read, we should notify
        context.sendBroadcast(new Intent("NEW_READ"));
        
        // upload the data to the database
        MongoWrapper mt = CreateMongoWrapper(context);

		for (int j = 0; j < retryList.size(); ++j) {
			trd = retryList.get(j);
			Log.e(TAG,"Before write TRD, " + trd.TransmissionId + " CaptureDateTime " + trd.CaptureDateTime);
			WritenToDb = mt.WriteToMongo(trd);
	        if(WritenToDb) {
	        	Log.e(TAG,"Write succeeded TRD, " + trd.TransmissionId + " CaptureDateTime " + trd.CaptureDateTime);
	        	mLastDbWriteTime = new Date().getTime();
	        	trd.setUploaded(1);
	        	source.updateRawDataEntry(trd);
	        } else {
	        	Log.e(TAG,"Write failed, " + trd.TransmissionId + " CaptureDateTime " + trd.CaptureDateTime);
	        	break;
	        }
     	}
		source.close();
	}
}
