package com.ht1.dexterity.app;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import android.util.Log;
import com.ht1.dexterity.app.DexCollectionService; 


import java.io.IOException;
import android.os.Environment;

public class BootReciever extends BroadcastReceiver {

	private final static String TAG = "BootReciever";
	
  @Override
  public void onReceive(Context context, Intent intent) {
	  
	    Log.e(TAG, "New onRecieve called Threadid starting BT" + Thread.currentThread().getId());
     
	    startLogcat();
     
        // Start the bt collection service
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			context.startService(new Intent(context, DexCollectionService.class));
		}

  }
  
  static void startLogcat() {
      // Start logging to logcat
      String filePath = Environment.getExternalStorageDirectory() + "/tzachilogcat.txt";
      try {
      	String[] cmd = { "/system/bin/sh", "-c", "ps | grep logcat  || logcat -f " + filePath + " -v threadtime tzachi:V *:E -r 10240 -n 8" };
      	Runtime.getRuntime().exec(cmd);
      } catch (IOException e2) {
          // TODO Auto-generated catch block
          e2.printStackTrace();
      }
  }
  
} 