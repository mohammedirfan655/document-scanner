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
        Log.i("DOC_LOG","execute() - START");
        Log.i("DOC_LOG","execute() - Options received = "+options.toString());
		if ( action.equals(ACTION_SHOW_EVENT) ) {
			this.process(options,callbackContext);
			return true;
		}
		return false;
	}

	private void process(JSONArray options, CallbackContext callbackContext) throws JSONException {
        Log.i("DOC_LOG","process() - START");
        JSONObject response = new JSONObject();
        JSONObject option=(JSONObject)options.get(0);

 		if ( option.has("source") ) {
	        response.put("status", "OK");
		    callbackContext.success(response);
		} else {
	        response.put("status", "ERROR");
	        response.put("message", "Their was an error processing your request");
		    callbackContext.error(response);
		}		
	}
}
