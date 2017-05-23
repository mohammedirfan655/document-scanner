package com.mohammedirfan.documentscanner;

import android.os.*;
import org.apache.cordova.*;
import android.content.*;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.scanlibrary.*;
import java.io.IOException;


public class DocumentScanner extends CordovaPlugin {

	private static final String ACTION_SHOW_EVENT = "process";
	private static String source="camera";
	private static String output="uri";
    private ImageView scannedImageView;
    private static final int REQUEST_CODE = 99;

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

		if(option.has("source")) source=(String)option.get("source");
		if(option.has("output")) output=(String)option.get("output");

		if ( source.equalsIgnoreCase("camera") ) {
			startScan(ScanConstants.OPEN_CAMERA);
		} else {
			startScan(ScanConstants.OPEN_MEDIA);
		}
		/*
	        response.put("status", "ERROR");
	        response.put("message", "Their was an error processing your request");
		    callbackContext.error(response);
		    */
	}

	public void startScan(int preference) {

		Context context=this.cordova.getActivity().getApplicationContext();
        Intent intent = new Intent(context,ScanActivity.class);
        intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
        cordova.getActivity().startActivityForResult(intent, REQUEST_CODE);
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(cordova.getActivity().getContentResolver(), uri);
                cordova.getActivity().getContentResolver().delete(uri, null, null);
                scannedImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap convertByteArrayToBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

}
