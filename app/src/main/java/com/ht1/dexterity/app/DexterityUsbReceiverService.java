package com.ht1.dexterity.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.lang.ProcessBuilder;
/**
 * Created by John Costik on 6/7/14.
 */
public class DexterityUsbReceiverService extends Service
{
    private static final String TAG = "tzachi";
    private SerialPortReader mReader;
    private final IBinder mBinder = new DexterityUsbServiceBinder();
    private BroadcastReceiver mDetachReceiver;
    private Runnable mUsbMonitorLoop;
    private boolean mDetached = true;
    private ServerSockets mServerSocket;

    @Override
    public IBinder onBind(Intent intent)
	{
        Log.i(TAG, "onBind");
        startSerialRead();
        return mBinder;
    }

    public class DexterityUsbServiceBinder extends Binder
    {
        DexterityUsbReceiverService getService()
        {
            Log.i(TAG, "DexterityUsbServiceBinder");
            return DexterityUsbReceiverService.this;
        }
    }

    public void startSerialRead()
	{
        // returns a static member, so safe to just call every time
        mReader = SerialPortReader.getInstance(this);
        mReader.StartThread();
    }

    public void stopSerialRead()
    {
        if (mReader != null)
        {
            Log.i(TAG, "stopSerialRead: Setting stop...");
            mReader.StopThread();
        }
    }

    public void ShowToast(final String toast)
    {
        // push a notification rather than toast.
        NotificationManager NM = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = new Notification.Builder(this)
                .setContentTitle("Dexterity receiver")
                .setContentText(toast)
                .setTicker(toast)
                .setSmallIcon(getResources().getIdentifier("ic_launcher", "drawable", getPackageName()))
                .build();

        NM.notify(R.string.notification_ReceiverAttached, n);
    }


    @Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// the intent can contain the device that launched us
		return START_STICKY;
	}

	@Override
    public void onCreate()
	{
        super.onCreate();


        
        BootReciever.startLogcat();
        
        // Start the socket thread
        try
        {
            mServerSocket = new ServerSockets(this);
            mServerSocket.start();
        }catch(IOException e)
        {
            // TODO: handle exceptions
           Log.e(TAG, "cought IOException...");
           e.printStackTrace();
        }    

        StartBroadcastReceiver();
        StartUsbWatcher();
        
        Log.i(TAG, "DexterityUsbReceiverService Service started");
    }

    @Override
    public void onDestroy()
	{
    	Log.i(TAG, "DexterityUsbReceiverService Service stoped");
        super.onDestroy();
        // stop the socket thread
        mServerSocket.Stop();
        try {
            mServerSocket.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "cought InterruptedException...");
            e.printStackTrace();
        }
        unregisterReceiver(mDetachReceiver);
    }

    private void StartBroadcastReceiver()
    {
        // set up for notification of disconnect
        mDetachReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))
                {
                    Log.i(TAG, "StartBroadcastReceiver: ACTION_USB_DEVICE_DETACHED");
                    rebootSystemOnDetach(context);
                    mDetached = true;
                    stopSerialRead();
//                    StartUsbWatcher();
                }
                else if(intent.getAction().equals("USB_DEVICE_ATTACH"))
                {
                    Log.i(TAG, "StartBroadcastReceiver: USB_DEVICE_ATTACH");
                    mDetached = false;
                    startSerialRead();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("USB_DEVICE_ATTACH");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mDetachReceiver, filter);
    }

    private void StartUsbWatcher()
    {
        mUsbMonitorLoop = new Runnable()
        {
            public void run()
            {
            	Log.e(TAG, "DexterityUsbReceiverService Thread starting");
            	try {
	                UsbManager manager = (UsbManager)getSystemService(USB_SERVICE);
	
	                while(true)
	                {
	                    if(mDetached)
	                    {
	                        // we only do anything if we're currently detached.  We have no problem
	                        // getting detach notifications, only attachments.  So we're only polling for attach
	                        for (final UsbDevice usbDevice : manager.getDeviceList().values())
	                        {
	                            // ok iterating all USB devices.  Let's see if it matches our requirements
	                            if(usbDevice.getVendorId() == 8187)
	                            {
	                                // ok, it's by pololu!
	                            	Log.e(TAG, "Sending USB_DEVICE_ATTACH");
	                                sendBroadcast(new Intent("USB_DEVICE_ATTACH"));
	                                mDetached = false;
	                                break;
	                            }
	                        }
	                    }
	
	                    // wait 1/2s and exit this if we're interrupted (terminated?)
	                    try { 
	                    	Thread.sleep(500); 
	                   }
	                    catch (InterruptedException exception) { 
	                		Log.e(TAG, "DexterityUsbReceiverService StartUsbWatcher cought InterruptedException exception - continuing");
	                    }
	                }
                } finally {
                	Log.e(TAG, "DexterityUsbReceiverService Thread stoping. in finaly");
                }
            }
        };

        Thread t = new Thread(mUsbMonitorLoop);
        t.start();
    }


    static void RunCommand(String[] cmd ) {
        // Start logging to logcat
        try {
        	Runtime.getRuntime().exec(cmd);
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Error running command");
        }
    }
    
    public static void rebootSystemOnDetach(final Context context) {
    	Log.e(TAG, "recieved ACTION_USB_DEVICE_DETACHED rebooting system");
    	
	   	 Long CaptureDateTime = new TimeWrapper().getTime();
		 
	   	 SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
	   	 String time = sdf.format(new Date(CaptureDateTime));
	   	 String file_name = Environment.getExternalStorageDirectory() + "/dmesg_" +time + ".txt";
    	
	   	ProcessBuilder pb = new ProcessBuilder(new String[] {"su", "-c", "dmesg > "+ file_name});
	   	Log.e(TAG, "after dmesg");
	   	try {
			pb.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "got exception, runnging dmesg", e);
			e.printStackTrace();
		}
	   	
	   	// Do the send on another thread since we are on the UI thread. We give this operation a few seconds to complete. 
        new Thread() {
            @Override
            public void run() {
            	SerialPortReader.WriteDebugDataToMongo(context, "Rebooting system because of detach uptime sec = " + (SystemClock.elapsedRealtime() / 1000 ));
            }
        }.start();
    	
    	
    	try { 
        	Thread.sleep(15000); 
       }
        catch (InterruptedException exception) { 
    		Log.e(TAG, "DexterityUsbReceiverService rebootSystemOnDetach  cought InterruptedException exception - continuing");
        }
    	
    	Log.e(TAG,"before reboot (trying both path locations)");
    	String[] cmd = {"/system/xbin/su", "-c", "reboot now"}; // might be /system/bin/su without the x this has worked on some systems...
    	RunCommand(cmd);
    	String[] cmd1 = {"/system/bin/su", "-c", "reboot now"}; 
    	RunCommand(cmd1);
    	Log.e(TAG,"after reboot");
    	
    }



}
