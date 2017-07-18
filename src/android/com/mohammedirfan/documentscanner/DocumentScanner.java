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
import java.io.*;


public class DocumentScanner extends CordovaPlugin {

	private static final String ACTION_SHOW_EVENT = "process";
	private static String source="camera";
	private static String output="uri";
	private ImageView scannedImageView;
	private static final int REQUEST_CODE = 99;
	private static final String D2A_docs="D2A_docs/";
	String response;

	@Override
	public boolean execute(String action, JSONArray options, final CallbackContext callbackContext) throws JSONException {
		ScanConstants.CBC=callbackContext;
		Log.i("DOC_LOG","execute() - START");
		Log.i("DOC_LOG","execute() - Options received = "+options.toString());
		if ( action.equalsIgnoreCase(ACTION_SHOW_EVENT) ) {
			this.process(options);
			return true;
		}
		response="Unknown method called "+action;
		ScanConstants.CBC.error(response);
		return false;
	}

	private void process(JSONArray options) throws JSONException {
		Log.i("DOC_LOG","process() - START");
		JSONObject option=(JSONObject)options.get(0);

		if(option.has("source")) source=(String)option.get("source");
		if(option.has("output")) output=(String)option.get("output");

		if ( source.equalsIgnoreCase("camera") ) {
			startScan(ScanConstants.OPEN_CAMERA);
		} else if ( source.equalsIgnoreCase("gallery") ) {
			startScan(ScanConstants.OPEN_MEDIA);
		} else {
			response="Invalid source passed [Source="+source+"]";
			ScanConstants.CBC.error(response);
		}
	}

	public void startScan(int preference) {
		Intent intent = new Intent(cordova.getActivity(), ScanActivity.class);
		intent.putExtra(ScanConstants.OPEN_INTENT_PREFERENCE, preference);
		if (this.cordova != null) {
			this.cordova.startActivityForResult((CordovaPlugin) this,intent, REQUEST_CODE);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
		Log.i("DOC_LOG","onActivityResult-start");
		Log.i("DOC_LOG","requestCode="+requestCode);
		if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
			Log.i("DOC_LOG",uri.toString());
			Bitmap bitmap = null;
			try {
				bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
				getContentResolver().delete(uri, null, null);

				FileOutputStream out = null;
				try {
					File dest = new File(Environment.getExternalStorageDirectory() + "/"+D2A_docs);
					if (!dest.exists()) {
						dest.mkdir();
					}
					String filename=Environment.getExternalStorageDirectory()+"/"+D2A_docs+"scanned_image.png";
					out = new FileOutputStream(filename);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
					Log.i("DOC_LOG","Filename="+filename);
					response="Your image was saved successfully";
					ScanConstants.CBC.success(response);
//                    cordova.getActivity().finish();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try { if (out != null) out.close();
					} catch (IOException e) { e.printStackTrace(); }
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Bitmap convertByteArrayToBitmap(byte[] data) {
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	public void permissionCheck() {
//        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},10);
	}

	private ContentResolver getContentResolver(){
		return cordova.getActivity().getContentResolver();
	}
}
