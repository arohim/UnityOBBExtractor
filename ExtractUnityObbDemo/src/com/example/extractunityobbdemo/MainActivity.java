package com.example.extractunityobbdemo;

import java.io.IOException;

import com.example.extractobbtest.R;
import com.headroid.unityobbextractor.OBBExtractor;
import com.headroid.unityobbextractor.interfaces.IOBBExtractorCallback;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static String mCacheDir;

	private String TAG = "MainActivity";

	private String targetPath;

	private TextView progressTVT;

	private TextView statusTVT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mCacheDir = HasSDCard()
				? Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + this.getPackageName()
				: this.getFilesDir().getAbsolutePath();

		targetPath = mCacheDir + "/Assets/";
		progressTVT = (TextView) findViewById(R.id.textView1);
		statusTVT = (TextView) findViewById(R.id.textView2);

		Button btn = (Button) findViewById(R.id.button1);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					OBBExtractor.startExtractOBB(MainActivity.this, targetPath, new IOBBExtractorCallback() {

						@Override
						public void extractorProgress(final float progress) {
							MainActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									progressTVT.setText(progress + "");
								}
							});
						}

						@Override
						public void callback(final int status, final String msg) {
							MainActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									statusTVT.setText(status + " : " + msg);
								}
							});
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static boolean HasSDCard() {
		String en = Environment.getExternalStorageState();
		if (en.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;
	}
}
