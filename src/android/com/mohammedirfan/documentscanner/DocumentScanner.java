package com.mohammedirfan.documentscanner;

import android.os.*;
import org.apache.cordova.*;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

public class DocumentScanner extends CordovaPlugin {

	private static final String ACTION_SHOW_EVENT = "process";

	@Override
	public boolean execute(String action, JSONArray options, final CallbackContext callbackContext) throws JSONException {
        Log.i("KIRANCSE","execute() - START");
		if ( action.equals(ACTION_SHOW_EVENT) ) {
			this.process(options,callbackContext);
			return true;
		}
		return false;
	}

	private void process(JSONArray options, CallbackContext callbackContext) {
        Log.i("KIRANCSE","process() - START");
        JSONArray response= new JSONArray();
        JSONObject jo = new JSONObject();

        JSONObject option=(JSONObject)options.get(0);
 		if ( option.get("source").length > 0 ) {
	        jo.put("status", "OK");
	        response.put(jo);
		    callbackContext.success(response);
		} else {
	        jo.put("status", "ERROR");
	        jo.put("message", "Their was an error processing your request");
	        response.put(jo);
		    callbackContext.error(response);
		}		
	}
}
