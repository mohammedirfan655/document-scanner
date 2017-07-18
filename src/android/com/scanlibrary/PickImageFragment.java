package com.scanlibrary;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jhansi on 04/04/15.
 */
public class PickImageFragment extends Fragment {

	private View view;
	private ImageButton cameraButton;
	private ImageButton galleryButton;
	private Uri fileUri;
	private IScanner scanner;
	public FakeR fakeR;
	public Activity cur_activity;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		fakeR = new FakeR(activity);
		cur_activity=activity;
		if (!(activity instanceof IScanner)) {
			throw new ClassCastException("Activity must implement IScanner");
		}
		this.scanner = (IScanner) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(fakeR.getId("layout", "sc_pick_image_fragment"), null);
		init();
		return view;
	}

	private void init() {
		cameraButton = (ImageButton) view.findViewById(fakeR.getId("id", "cameraButton"));
		cameraButton.setOnClickListener(new CameraButtonClickListener());
		galleryButton = (ImageButton) view.findViewById(fakeR.getId("id", "selectButton"));
		galleryButton.setOnClickListener(new GalleryClickListener());
		if (isIntentPreferenceSet()) {
			handleIntentPreference();
        } else {
			String response="There was an error during processing your request";
			ScanConstants.CBC.error(response);
			cur_activity.finish();
            getActivity().finish();
        }
    }

    private void clearTempImages() {
        try {
            File tempFolder = new File(ScanConstants.IMAGE_PATH);
            for (File f : tempFolder.listFiles())
                f.delete();
        } catch (Exception e) {
            e.printStackTrace();
		}
	}

	private void handleIntentPreference() {
		int preference = getIntentPreference();
		if (preference == ScanConstants.OPEN_CAMERA) {
			openCamera();
		} else if (preference == ScanConstants.OPEN_MEDIA) {
			openMediaContent();
		}
	}

	private boolean isIntentPreferenceSet() {
		int preference = getArguments().getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
		return preference != 0;
	}

	private int getIntentPreference() {
		int preference = getArguments().getInt(ScanConstants.OPEN_INTENT_PREFERENCE, 0);
		return preference;
	}


	private class CameraButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			openCamera();
		}
	}

	private class GalleryClickListener implements View.OnClickListener {
		@Override
		public void onClick(View view) {
			openMediaContent();
		}
	}

	public void openMediaContent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/*");
		startActivityForResult(intent, ScanConstants.PICKFILE_REQUEST_CODE);
	}

	public void openCamera() {
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		File file = createImageFile();
		file.getParentFile().mkdirs();
		fileUri = Uri.fromFile(file);
		if (file != null) {
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
			startActivityForResult(cameraIntent, ScanConstants.START_CAMERA_REQUEST_CODE);
		}
	}

	private File createImageFile() {
		clearTempImages();
//		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//		File file = new File(ScanConstants.IMAGE_PATH, "IMG_" + timeStamp + ".jpg");
		File file = new File(ScanConstants.IMAGE_PATH, "/scanned_image.png");
		return file;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("DOC_LOG","PickImageFragment - onActivityResult" + resultCode);
		Log.d("", "onActivityResult" + resultCode);
		Bitmap bitmap = null;
		if (resultCode == Activity.RESULT_OK) {
			Log.i("DOC_LOG","User captured image");
			try {
				switch (requestCode) {
					case ScanConstants.START_CAMERA_REQUEST_CODE:
						bitmap = getBitmap(fileUri);
						break;

					case ScanConstants.PICKFILE_REQUEST_CODE:
						bitmap = getBitmap(data.getData());
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			Log.i("DOC_LOG","User clicked cancel from PickImageFragment");
			String response="Scanning process cancelled by user";
			ScanConstants.CBC.error(response);
			cur_activity.finish();
			getActivity().finish();
		}
		if (bitmap != null) {
			postImagePick(bitmap);
		}
	}

	protected void postImagePick(Bitmap bitmap) {
		Uri uri = Utils.getUri(getActivity(), bitmap);
		bitmap.recycle();
		scanner.onBitmapSelect(uri);
	}

	private Bitmap getBitmap(Uri selectedimg) throws IOException {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 3;
		AssetFileDescriptor fileDescriptor = null;
		fileDescriptor =
				getActivity().getContentResolver().openAssetFileDescriptor(selectedimg, "r");
		Bitmap original
				= BitmapFactory.decodeFileDescriptor(
				fileDescriptor.getFileDescriptor(), null, options);
		return original;
	}
}