package com.example.m.divis;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
   
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;

public class FragmentData extends Fragment {
	private static final String TAG = "DIVISFragmentData";
	private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
	SharedPreferences sharedPrefs;

	// text views
	private TextView mTimestamp;
	private TextView mSecciDepth;
	private TextView mLivePixelsUpper;
	private TextView mLivePixelsLower;
	private TextView mWashedPixelsUpper;
	private TextView mWashedPixelsLower;
	private TextView mDataPixelsUpper;
	private TextView mDataPixelsLower;
	private TextView mAvgRUpper;
	private TextView mAvgRLower;
	private TextView mAvgGUpper;
	private TextView mAvgGLower;
	private TextView mAvgBUpper;
	private TextView mAvgBLower;
	private Button mButtonSave;

	// data
	int upperLive = 0;
	int upperWashed = 0;
	int upperData = 0;
	int upperRTotal = 0;
	int upperGTotal = 0;
	int upperBTotal = 0;
	int upperRAvg = 0;
	int upperGAvg = 0;
	int upperBAvg = 0;

	int lowerLive = 0;
	int lowerWashed = 0;
	int lowerData = 0;
	int lowerRTotal = 0;
	int lowerGTotal = 0;
	int lowerBTotal = 0;
	int lowerRAvg = 0;
	int lowerGAvg = 0;
	int lowerBAvg = 0;

	String sTime;

	// TODO: optimization: use raw instead of jpeg when available
	private RawCallback mRawCallback;
	class RawCallback implements Camera.ShutterCallback, Camera.PictureCallback {

		@Override
		public void onShutter() {
			Log.d(TAG, "onShutter");
			// notify the user, normally with a sound, that the picture has 
			// been taken
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			/*
			Log.d(TAG, "onPictureTaken (Raw)");
			if(data == null) {
				Log.d(TAG, "ERROR, Raw picture data is not available!");
			}
			// manipulate uncompressed image data
			*/
		}
	}

	private JpegCallback mJpegCallback;
	class JpegCallback implements Camera.PictureCallback {

		@Override
		public void onPictureTaken(byte[] jpeg, Camera camera) {
			Log.d(TAG, "onPictureTaken (Jpeg)");
			class AnalyzerTask implements Runnable {
				byte[] jpeg;
				AnalyzerTask(byte[] jpeg) { this.jpeg = jpeg; }
				public void run() {
					analyzeImage(jpeg);
				}
			}
			Thread t = new Thread(new AnalyzerTask(jpeg));
			t.start();
		}
	}

	void updateUi()
	{
		mTimestamp.setText(sTime);
//			mSecciDepth.setText();
		mLivePixelsUpper.setText(Integer.toString(upperLive));
		mWashedPixelsUpper.setText(Integer.toString(upperWashed));
		mDataPixelsUpper.setText(Integer.toString(upperData));
		mAvgRUpper.setText(Integer.toString(upperRAvg));
		mAvgGUpper.setText(Integer.toString(upperGAvg));
		mAvgBUpper.setText(Integer.toString(upperBAvg));

		mLivePixelsLower.setText(Integer.toString(lowerLive));
		mWashedPixelsLower.setText(Integer.toString(lowerWashed));
		mDataPixelsLower.setText(Integer.toString(lowerData));
		mAvgRLower.setText(Integer.toString(lowerRAvg));
		mAvgGLower.setText(Integer.toString(lowerGAvg));
		mAvgBLower.setText(Integer.toString(lowerBAvg));
	}

	// run in separate thread
	void analyzeImage(byte[] jpeg)
	{
		Bitmap bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

		int w = bmp.getWidth();
		int h = bmp.getHeight();

		Point upperCenter = new Point(
				sharedPrefs.getInt(getString(R.string.saved_upper_x), 100),
				sharedPrefs.getInt(getString(R.string.saved_upper_y), 100));
		int upperRadius = sharedPrefs.getInt(getString(R.string.saved_upper_radius), 100);

		Point lowerCenter = new Point(
				sharedPrefs.getInt(getString(R.string.saved_lower_x), 300),
				sharedPrefs.getInt(getString(R.string.saved_lower_y), 100));
		int lowerRadius = sharedPrefs.getInt(getString(R.string.saved_lower_radius), 100);

		upperLive = 0;
		upperWashed = 0;
		upperData = 0;
		upperRTotal = 0;
		upperGTotal = 0;
		upperBTotal = 0;
		upperRAvg = 0;
		upperGAvg = 0;
		upperBAvg = 0;

		lowerLive = 0;
		lowerWashed = 0;
		lowerData = 0;
		lowerRTotal = 0;
		lowerGTotal = 0;
		lowerBTotal = 0;
		lowerRAvg = 0;
		lowerGAvg = 0;
		lowerBAvg = 0;

		int upper_radius_squared = upperRadius*upperRadius;
		int lower_radius_squared = lowerRadius*lowerRadius;
		for(int i=0; i<h; i++) {
			for(int j=0; j<w; j++) {
				if(pixelWithinArea(upperCenter, upperRadius, upper_radius_squared, new Point(j, i))) {
					int c = bmp.getPixel(j, i);
					if(pixelIsLive(c))
						upperLive++;
					if(pixelIsWashed(c))
						upperWashed++;
					if(pixelIsData(c)) {
						upperData++;
						upperRTotal += Color.red(c);
						upperGTotal += Color.green(c);
						upperBTotal += Color.blue(c);
					}
				}

				if(pixelWithinArea(lowerCenter, lowerRadius, lower_radius_squared, new Point(j, i))) {
					int c = bmp.getPixel(j, i);
					if(pixelIsLive(c))
						lowerLive++;
					if(pixelIsWashed(c))
						lowerWashed++;
					if(pixelIsData(c)) {
						lowerData++;
						lowerRTotal += Color.red(c);
						lowerGTotal += Color.green(c);
						lowerBTotal += Color.blue(c);
					}
				}
			}
		}

		if(lowerData > 0 && upperData > 0) {
			upperRAvg = upperRTotal / upperData;
			upperGAvg = upperGTotal / upperData;
			upperBAvg = upperBTotal / upperData;

			lowerRAvg = lowerRTotal / lowerData;
			lowerGAvg = lowerGTotal / lowerData;
			lowerBAvg = lowerBTotal / lowerData;
		}

		Time now = new Time();
		now.setToNow();
		sTime = now.format("%Y_%m_%d_%H_%M_%S");

		MainActivity act = (MainActivity)getActivity();
		act.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateUi();
			}
		});

		// takePicture has finished, now safe to resume the preview
		// NOTE: preview must be started before takePicture
		act.mCamera.startPreview();

		// schedule next timer
		timerHandler.postDelayed(timerRunnable, timerInterval);
	}

	boolean accessExternalStorage()
	{
		int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
				Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
			Log.d(TAG, "Permission already granted!");
			return true;
		} else {
			Log.d(TAG, "Requesting");
			requestPermissions(
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
		}
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			String permissions[], int[] grantResults) {
		Log.d(TAG, "onRequestPermissionsResult");
		switch (requestCode) {
		case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
			// If request is cancelled, the result arrays are empty.
			if (grantResults.length > 0
					&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				writeToCsv();
			} else {
				// permission denied, boo! Disable the
				// functionality that depends on this permission.
			}
			return;
		}
	}

	String getS(int id)
	{
		return sharedPrefs.getString(getString(id), "");
	}

	int getI(int id)
	{
		return sharedPrefs.getInt(getString(id), 0);
	}

	void doWriteToCsv()
	{
		Log.d(TAG, "Writing to csv");

		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
		if(!dir.exists()) {
			if(!dir.mkdir()) {
				Log.d(TAG, "Target directory cannot be created : " + dir.toString());
				return;
			}
		}
		File file = new File(dir, "DIVIS.csv");

		// delimeter
		String c = ",";

		//Write to file
		try {
			FileWriter fileWriter = new FileWriter(file, true);
			fileWriter.append(
					getS(R.string.saved_device_id) + c +
					getS(R.string.saved_divis_id) + c +
					getS(R.string.saved_app_version) + c +
					getS(R.string.saved_location_id) + c +
					getS(R.string.saved_location_name) + c +
					getS(R.string.saved_location_detail) + c +
					getI(R.string.saved_upper_sensor_depth) + c +
					getI(R.string.saved_lower_sensor_depth) + c +
					sTime + c +
					getI(R.string.saved_secci_depth) + c +
					Integer.toString(upperLive) + c +
					Integer.toString(upperWashed) + c +
					Integer.toString(upperData) + c +
					Integer.toString(upperRAvg) + c +
					Integer.toString(upperGAvg) + c +
					Integer.toString(upperBAvg) + c +
					Integer.toString(lowerLive) + c +
					Integer.toString(lowerWashed) + c +
					Integer.toString(lowerData) + c +
					Integer.toString(lowerRAvg) + c +
					Integer.toString(lowerGAvg) + c +
					Integer.toString(lowerBAvg) + c +
					getS(R.string.saved_camera_exposure) + c +
					getI(R.string.saved_min_rgb_for_live_pixel) + c +
					getI(R.string.saved_upper_x) + c +
					getI(R.string.saved_upper_y) + c +
					getI(R.string.saved_upper_radius) + c +
					getI(R.string.saved_lower_x) + c +
					getI(R.string.saved_lower_y) + c +
					getI(R.string.saved_lower_radius) + "\n");
			fileWriter.flush();
			fileWriter.close();
			Toast.makeText(getActivity(), getString(R.string.msg_csv_written),
					Toast.LENGTH_SHORT).show();
		} catch (java.io.IOException e) {
			Log.d(TAG, e.toString());
			Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
			//Handle exception
		}
	}

	void writeToCsv()
	{
		// no data captured yet
		if(mTimestamp.getText().toString().isEmpty()) {
			Toast.makeText(getActivity(), getString(R.string.msg_no_data_yet), Toast.LENGTH_SHORT).show();
			return;
		}

		if(!accessExternalStorage())
			return;

		if(!mSecciDepth.getText().toString().isEmpty()) {
			doWriteToCsv();
		} else {
			new AlertDialog.Builder(getActivity())
				.setTitle("Title")
				.setMessage(getString(R.string.msg_write_despite_blank_secci))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						doWriteToCsv();
					}})
				.setNegativeButton(android.R.string.no, null).show();
		}
	}

	// capture timer
	long timerInterval = 1000;
	Handler timerHandler = new Handler();
	Runnable timerRunnable = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "Timer Callback");
			MainActivity act = (MainActivity)getActivity();
			if(act.mViewPager.getCurrentItem() != 2)
				return;
			if(act.mCamera != null) {
				SurfaceView preview = ((FragmentCalibrate)act.mSectionsPagerAdapter.getItem(1)).mPreview;

				// callbacks: shutter, raw, post view, jpeg
				Log.d(TAG, "Taking a picture!");
				try {
					act.mCamera.takePicture(null, null, null, mJpegCallback);
				} catch(Exception e) {
					// "E/Camera: Error 100" and "Camera service died!"
					// NOTE: fixed by not re-enabling preview until calibrate
					// screen shown.
					Log.d(TAG, e.toString());
					Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_LONG).show();
				}
			} else {
				timerHandler.postDelayed(this, timerInterval);
			}
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRawCallback = new RawCallback();
		mJpegCallback = new JpegCallback();
		sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		View v = inflater.inflate(R.layout.fragment_data, container, false);

		mTimestamp = (TextView)v.findViewById(R.id.timestamp);
		mSecciDepth = (EditText)v.findViewById(R.id.secci_depth);
		mLivePixelsUpper = (TextView)v.findViewById(R.id.live_pixels_upper);
		mLivePixelsLower = (TextView)v.findViewById(R.id.live_pixels_lower);
		mWashedPixelsUpper = (TextView)v.findViewById(R.id.washed_pixels_upper);
		mWashedPixelsLower = (TextView)v.findViewById(R.id.washed_pixels_lower);
		mDataPixelsUpper = (TextView)v.findViewById(R.id.data_pixels_upper);
		mDataPixelsLower = (TextView)v.findViewById(R.id.data_pixels_lower);
		mAvgRUpper = (TextView)v.findViewById(R.id.avg_r_upper);
		mAvgRLower = (TextView)v.findViewById(R.id.avg_r_lower);
		mAvgGUpper = (TextView)v.findViewById(R.id.avg_g_upper);
		mAvgGLower = (TextView)v.findViewById(R.id.avg_g_lower);
		mAvgBUpper = (TextView)v.findViewById(R.id.avg_b_upper);
		mAvgBLower = (TextView)v.findViewById(R.id.avg_b_lower);

		mButtonSave = (Button)v.findViewById(R.id.btn_write_csv);
		mButtonSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				writeToCsv();
			}
		});

		mSecciDepth.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					updatePrefs();
					InputMethodManager imm= (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mSecciDepth.getWindowToken(), 0);
					return true;
				}
				return false;
			}
		});
		mSecciDepth.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
					updatePrefs();
			}
		});

		// setup timer
		((MainActivity)getActivity()).mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { } 

			@Override
			public void onPageSelected(int position) {
				MainActivity act = (MainActivity)getActivity();
				if(position == 2) {
					timerHandler.removeCallbacks(timerRunnable);
					timerHandler.postDelayed(timerRunnable, 1000);
				} else {
					timerHandler.removeCallbacks(timerRunnable);
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) { }
		});

		return v;
	}

	void updatePrefs()
	{
		Log.d(TAG, "updatePrefs");
		SharedPreferences.Editor editor = sharedPrefs.edit();
		int i = 0;
		String txt = mSecciDepth.getText().toString();
		if(!txt.isEmpty())
			i = Integer.parseInt(txt);
		editor.putInt(getString(R.string.saved_secci_depth), i);
		editor.commit();
	}

	@Override
	public void onPause() {
		super.onPause();
		timerHandler.removeCallbacks(timerRunnable);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	boolean pixelIsLive(int c)
	{
		int min = sharedPrefs.getInt(getString(R.string.saved_min_rgb_for_live_pixel),
				Integer.parseInt(getString(R.string.saved_min_rgb_for_live_pixel_default)));
		return (Color.red(c) > min && Color.green(c) > min && Color.blue(c) > min);
	}

	boolean pixelIsWashed(int c)
	{
		return (Color.red(c) > 254 || Color.green(c) > 254 || Color.blue(c) > 254);
	}

	boolean pixelIsData(int c)
	{
		return pixelIsLive(c) && !pixelIsWashed(c);
	}

	boolean pixelWithinArea(Point center, int radius, int radius_squared, Point px)
	{
		// fast check
		if(center.x - radius > px.x || center.x + radius < px.x ||
				center.y - radius > px.y || center.y + radius < px.y)
			return false;

		// slow check
		int dx = center.x - px.x;
		int dy = center.y - px.y;
		int dist_squared = (int)Math.floor(Math.pow(dx, 2) + Math.pow(dy, 2));
		if(dist_squared > radius_squared)
			return false;
		return true;
	}
}
