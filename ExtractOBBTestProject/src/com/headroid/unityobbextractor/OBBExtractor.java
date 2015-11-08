package com.headroid.unityobbextractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.example.extractobbtest.R;
import com.headroid.unityobbextractor.interfaces.IOBBExtractorCallback;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class OBBExtractor {

	// status
	private static int EXTRACT_OBB_SUCCESS = 0;
	private static int EXTRACT_OBB_PENDING = 1;
	private static int EXTRACT_OBB_EXTRACTING = 3;

	private static int EXTRACT_OBB_WRITE_FILE_FAIL = -1;
	private static int EXTRACT_OBB_CREATE_FOLDER_FAIL = -2;
	private static int EXTRACT_OBB_READ_CONFIG_FAIL = -3;
	private static int EXTRACT_OBB_OBB_NOT_FOUND = -4;

	private String mTargetPath = "";
	private Context mContext;
	private String LOGTAG = "OBBExtractor";
	private IOBBExtractorCallback mCallback;
	private String sourceList = "sourceOBBList.txt";
	private String targetList = "targetOBBList.txt";
	private String prepareFolderList = "prepareOBBFoldersList.txt";

	private final ArrayList<String> extractList = new ArrayList<String>();
	private final ArrayList<String> destList = new ArrayList<String>();
	public final float MAX_PROGRESS = 100f;

	public float currentProgress = 0;

	public OBBExtractor(Context context, String targetPath, IOBBExtractorCallback callback) {
		mContext = context;
		mCallback = callback;
		mTargetPath = targetPath;

		new ExtractOBBAsync().execute();

		mCallback.callback(EXTRACT_OBB_PENDING, "Pending");
	}

	public static int startExtractOBB(Context context, String targetPath, final IOBBExtractorCallback callback)
			throws IOException {
		new OBBExtractor(context, targetPath, new IOBBExtractorCallback() {

			@Override
			public void extractorProgress(float progress) {
				callback.extractorProgress(progress);
			}

			@Override
			public void callback(int status, String msg) {
				callback.callback(status, msg);
			}
		});
		return 0;
	}

	protected boolean readConfigurationList() {
		try {
			readSourceList();
			readTargetList();
			return true;
		} catch (IOException e) {
			Log.e(LOGTAG, "Error opening asset " + mContext.getString(R.string.extract_obb_read_config_fail));
		}
		Log.e("string", "test");
		return false;
	}

	private void readSourceList() throws IOException {
		BufferedReader in = null;
		InputStream is = mContext.getAssets().open(sourceList);
		in = new BufferedReader(new InputStreamReader(is));
		String str;
		while ((str = in.readLine()) != null) {
			extractList.add(str);
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				Log.e(LOGTAG, "Error closing asset " + sourceList);
			}
		}
	}

	private void readTargetList() throws IOException {
		BufferedReader in = null;
		InputStream is = mContext.getAssets().open(targetList);
		in = new BufferedReader(new InputStreamReader(is));
		String str;
		while ((str = in.readLine()) != null) {
			destList.add(str);
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				Log.e(LOGTAG, "Error closing asset " + targetList);
			}
		}
	}

	private boolean prepareFolders() {
		String name = prepareFolderList;
		BufferedReader in = null;
		try {
			InputStream is = mContext.getAssets().open(name);
			in = new BufferedReader(new InputStreamReader(is));
			String str;
			while ((str = in.readLine()) != null) {
				File file = new File(mTargetPath, str);
				if (!file.exists()) {
					file.mkdirs();
				}
			}
			return true;
		} catch (IOException e) {
			Log.e(LOGTAG, "Error opening asset " + name);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.e(LOGTAG, "Error closing asset " + name);
				}
			}
		}
		return false;
	}

	private boolean isOBBExist() {
		return Helpers.doesFileExist(mContext,
				Helpers.getExpansionAPKFileName(mContext, true, Helpers.getVersionCode(mContext)));
	}

	private boolean extractExpansionFile() {
		try {
			mCallback.callback(EXTRACT_OBB_EXTRACTING, "Extracting");
			// Get a ZipResourceFile representing a merger of both the main and
			// patch files
			ZipResourceFile expansionFile = APKExpansionSupport.getAPKExpansionZipFile(mContext, 1, 0);

			// mkdir target folder
			File targetFile = new File(mTargetPath);
			if (!targetFile.exists()) {
				targetFile.mkdirs();
			}

			// calculate percentage per file
			float percentPerFile = 0;
			percentPerFile = (MAX_PROGRESS - currentProgress) / (float) extractList.size();

			for (int i = 0; i < extractList.size(); ++i) {
				// Get an input stream for a known file inside the expansion
				// file
				// ZIPs
				ZipEntry zi = expansionFile.getZipEntry(extractList.get(i));
				InputStream fileStream = expansionFile.getInputStream(extractList.get(i));

				String destPath = mTargetPath + destList.get(i);
				File destination = new File(destPath);
				if (zi.isDirectory()) {
					destination.mkdirs();
					continue;
				}
				copyInputStreamToFile(fileStream, new File(destPath));

				if (currentProgress < MAX_PROGRESS)
					currentProgress += percentPerFile;
				mCallback.extractorProgress(currentProgress);
			}
			return true;
		} catch (IOException e) {
			Log.e("error", e.getMessage());
			e.printStackTrace();
		}

		return false;
	}

	private void copyInputStreamToFile(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
	}

	private class ExtractOBBAsync extends AsyncTask<String, String, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			if (!prepareFolders()) {
				mCallback.callback(EXTRACT_OBB_CREATE_FOLDER_FAIL,
						mContext.getString(R.string.extract_obb_create_folder_fail));
				return false;
			}
			currentProgress += randomWithRange(1D, 10.99D);
			mCallback.extractorProgress(currentProgress);

			if (!readConfigurationList()) {
				mCallback.callback(EXTRACT_OBB_READ_CONFIG_FAIL,
						mContext.getString(R.string.extract_obb_read_config_fail));
				return false;
			}

			currentProgress += randomWithRange(1D, 10.99D);
			mCallback.extractorProgress(currentProgress);

			boolean isOBBexist = isOBBExist();
			if (!isOBBexist) {
				mCallback.callback(EXTRACT_OBB_OBB_NOT_FOUND,
						mContext.getString(R.string.extract_obb_obb_not_found_fail));
				return false;
			}
			currentProgress += randomWithRange(1D, 10.99D);
			mCallback.extractorProgress(currentProgress);

			return extractExpansionFile();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			currentProgress = MAX_PROGRESS;
			mCallback.extractorProgress(currentProgress);
			mCallback.callback(result ? EXTRACT_OBB_SUCCESS : EXTRACT_OBB_WRITE_FILE_FAIL,
					result ? mContext.getString(R.string.extract_obb_success)
							: mContext.getString(R.string.extract_obb_extract_fail));
		}

	}

	private double randomWithRange(double min, double max) {
		double range = Math.abs(max - min);
		return (Math.random() * range) + (min <= max ? min : max);
	}
}
