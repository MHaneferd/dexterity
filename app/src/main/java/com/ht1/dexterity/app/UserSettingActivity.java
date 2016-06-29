package com.ht1.dexterity.app;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

class Params {
	String MongoUrl;
	final Preference button;
	Context context;
	
	Params(final Preference button, Context context) {
		this.button = button;
		this.context = context;
	}
}

class MongoAddressChecker extends AsyncTask<Params, Void, String> {
	
	Preference button;
	
    protected String doInBackground(Params... params) {
    	
    	button = params[0].button;
    	
	    MongoWrapper mt = SerialPortReader.CreateMongoWrapper(params[0].context);
	    boolean WritenToDb = mt.WriteDebugDataToMongo("Testing mongo addresses");
    	
    	if(WritenToDb) {
    		return "writen successfully";
    	} else {
    		return "Writing to mongodb failed";
    	}
    }

    protected void onPostExecute(String result) {
        this.button.setSummary(result);
    }
}	


public class UserSettingActivity extends PreferenceActivity {
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        addPreferencesFromResource(R.xml.settings);
        
        final Preference button = (Preference)findPreference("key_test_mongo");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
            	if(button.getSummary().equals("testing...")) {
            		// Don't run two simultanious tests
            		return false;
            	}
            	button.setSummary("testing...");
            	new MongoAddressChecker().execute(new Params(button, getApplicationContext()));
            	
                return true;
            }
        });
        
        
        final Preference tIdButton = (Preference)findPreference("key_copy_txid");
        tIdButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
            	List<TransmitterRawData> retryList = new DexterityDataSource(getApplicationContext()).getAllDataObjects(false, false, 1);
            	if(retryList.size() == 1) {
            		TransmitterRawData transmitterRawData = retryList.get(0);
            		Log.e("UserSettingActivity","setting key " + transmitterRawData.TransmitterId);
            		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            		prefs.edit().putString("transmitter_id", transmitterRawData.TransmitterId). commit();
            		
            		 // First get reference to edit-text view elements
                    EditTextPreference myPrefText = (EditTextPreference) findPreference("transmitter_id");
             
                    // Now, manually update it's value to default/empty
                    myPrefText.setText(transmitterRawData.TransmitterId); // Now, if you click on the item, you'll see the value you've just set here
            	}
            	
                return true;
            }
        });
 
    }
}