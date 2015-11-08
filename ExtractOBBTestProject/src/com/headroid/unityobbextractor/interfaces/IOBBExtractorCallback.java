package com.headroid.unityobbextractor.interfaces;

public interface IOBBExtractorCallback {
	void extractorProgress(float progress);

	void callback(int status, String msg);
}
