package com.scanlibrary;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import org.apache.cordova.*;
import android.util.Log;
import java.io.*;


public class ScanActivity extends Activity implements IScanner {

	public FakeR fakeR;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fakeR = new FakeR(this);
		setContentView(fakeR.getId("layout", "sc_scan_layout"));
		Log.i("D2A_DEBUG","ScanActivity start");
		init();
	}

	private void init() {
		PickImageFragment fragment = new PickImageFragment();
		Bundle bundle = new Bundle();
		bundle.putInt(ScanConstants.OPEN_INTENT_PREFERENCE, getPreferenceContent());
		fragment.setArguments(bundle);
		android.app.FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(fakeR.getId("id", "content"), fragment);
		fragmentTransaction.commit();
	}

	protected int getPreferenceContent() {
		return getIntent().getIntExtra(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
	}

	public void onBitmapSelect(Uri uri) {
		ScanFragment fragment = new ScanFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(ScanConstants.SELECTED_BITMAP, uri);
		fragment.setArguments(bundle);
		android.app.FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(fakeR.getId("id", "content"), fragment);
		fragmentTransaction.addToBackStack(ScanFragment.class.toString());
		fragmentTransaction.commit();
	}

	public void onScanFinish(Uri uri) {
		ResultFragment fragment = new ResultFragment();
		Bundle bundle = new Bundle();
		bundle.putParcelable(ScanConstants.SCANNED_RESULT, uri);
		fragment.setArguments(bundle);
		android.app.FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.add(fakeR.getId("id", "content"), fragment);
		fragmentTransaction.addToBackStack(ResultFragment.class.toString());
		fragmentTransaction.commit();
	}

	public native Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4);

	public native Bitmap getGrayBitmap(Bitmap bitmap);

	public native Bitmap getMagicColorBitmap(Bitmap bitmap);

	public native Bitmap getBWBitmap(Bitmap bitmap);

	public native float[] getPoints(Bitmap bitmap);

	static {
		System.loadLibrary("opencv_java3");
		System.loadLibrary("Scanner");
	}
}