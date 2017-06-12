package com.scanlibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentCallbacks2;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import org.apache.cordova.*;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.util.Log;
import java.io.*;


/**
 * Created by jhansi on 28/03/15.
 */
public class ScanActivity extends Activity implements IScanner, ComponentCallbacks2 {

	public FakeR fakeR;
	public static String TAG="DOC_LOG";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fakeR = new FakeR(this);
		setContentView(fakeR.getId("layout", "sc_scan_layout"));
		Log.i(TAG,"ScanActivity start");
		init();
	}

	private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status){
				case LoaderCallbackInterface.SUCCESS:
					Log.i("DOC_LOG","onManagerConnected");
					break;
				default:
					super.onManagerConnected(status);
					break;
			}
		}
	};

/*	public void onResume()
	{
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, baseLoaderCallback);
		} else {
			Log.d("OpenCV", "OpenCV library found inside package. Using it!");
			baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}*/

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

    @Override
    public void onTrimMemory(int level) {
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
                new AlertDialog.Builder(this)
                        .setTitle("Low Memory")
                        .setMessage("Your phone is low on memory, you may feel some lags while editing images.")
                        .create()
                        .show();
                break;
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
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