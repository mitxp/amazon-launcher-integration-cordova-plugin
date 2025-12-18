package com.amazon.cordova.plugins.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cordova.CordovaActivity;
import com.amazon.cordova.plugins.launcher.LauncherIntegrationUtils;

public class DeepLinkingCordovaActivity extends CordovaActivity {
    static final String QUERY_PARAMETER_NAME = "amazonLauncherIntegrationContentId";
    static final String TAG = "DeepLinkingCordovaActivity";
    static final String DEEP_LINK_REGEX_METADATA_KEY = "com.amazon.cordova.plugins.launcher.DEEP_LINK_REGEX";

    /**
     * Overrides the launchUrl member with a deep link url specifying content to be launched.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.appView == null || isBlank(this.appView.getUrl())) {
            // determine if the application was launched from a different application (such as the launcher app) and contains additional data needing to be passed to the web page
            this.launchUrl = createDeepLinkUrl(getIntent());
            Log.d(TAG, "Loading webview with url : " + launchUrl);
        } else {
            Log.d(TAG, "Webview is already loaded with url : " + this.appView.getUrl());
        }
    }

    /**
     * Creates a launchUrl with a specific parameter to enable app to directly launch video, if the launch intent includes launch data.
     * @return The full deep link url string to be launched by webview.
     */
    private String createDeepLinkUrl(Intent intent) {
        boolean dataIsInURIFormat = false;
        try {
            dataIsInURIFormat = LauncherIntegrationUtils.isVideoIdInIntentData(this);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not determine if videoID is in intent data", e);
            return this.launchUrl;
        }

        String launchIntentData;
        if (dataIsInURIFormat) {
            Uri dataUri = intent.getData();
            launchIntentData = dataUri == null ? null : dataUri.toString();
        } else {
            launchIntentData = intent.getStringExtra(LauncherIntegrationUtils.getVideoIdDataExtraName());
        }
        if (isBlank(launchIntentData) || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return this.launchUrl;
        }

        String videoID;
        try {
            videoID = extractId(launchIntentData);
        } catch (Exception e) {
            Log.e(TAG, "Error creating deep link URL", e);
            return this.launchUrl;
        }
        return addVideoIDQueryParam(this.launchUrl, videoID);
    }

    /**
     * If videoID is not blank, appends it to the given uri as a query parameter.  The Query parameter key is set by QUERY_PARAMETER_NAME.
     * @param uri The URI, in string format, to append the query parameter to
     * @param videoID The videoID to append to the given uri
     * @return The new uri, with query parameter added, in string format.
     */
    private static String addVideoIDQueryParam(String uri, String videoID) {
        if (isBlank(videoID)) {
            // if we didn't get a valid video ID, return original URI
            return uri;
        }

        // use the uri builder to properly append the new video path
        Uri.Builder builder = Uri.parse(uri).buildUpon();
        builder.appendQueryParameter(QUERY_PARAMETER_NAME, videoID);
        return builder.build().toString();
    }

    /**
     * Uses user provided regex to extract first matched group as video ID. Default regex captures an ID between single quotes.
     * @param launchData String to be parsed
     * @return The first match from the regex search of string. If no match, returns an empty string.
     * @throws PackageManager.NameNotFoundException
     */
    private String extractId(String launchData) throws PackageManager.NameNotFoundException, java.io.UnsupportedEncodingException  {

        ApplicationInfo appInfo = this.getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
        String regexString = appInfo.metaData.getString(DEEP_LINK_REGEX_METADATA_KEY);
        Pattern pattern = Pattern.compile(regexString);
        Matcher matcher = pattern.matcher(launchData);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    /**
     * Checks if string is not null, and contains more than just whitespace
     * @param string string to be checked is not null or empty
     * @return boolean: true if string is null or empty, false if it contains characters
     */
    private static boolean isBlank(String string) {
        if (string == null) {
            return true;
        }
        return TextUtils.isEmpty(string.trim());
    }

    /**
     * Handles intents that are received when the app is already running. Checks if query string with the deeplink would change,
     * if so the updated url will be loaded in the webview. This will reload the web app.
     * @param intent Intent with deeplink data.
     */
    @Override
    public void onNewIntent(Intent intent) {
      super.onNewIntent(intent);

      String originalUrl = this.launchUrl;
      this.launchUrl = this.dropQueryParam(this.launchUrl);
      String launchUrl = createDeepLinkUrl(intent);

      if (this.launchUrl==launchUrl) {
        Log.i(TAG, "onNewIntent: url didn't change - "+launchUrl);
        this.launchUrl = originalUrl;
        return;
      }
      Log.i(TAG, "inNewIntent: Loading new url : " + launchUrl);
      this.launchUrl = launchUrl;
      loadUrl(launchUrl);
    }

    private String dropQueryParam(String originalUri) {
      Uri uri = Uri.parse(originalUri);
      Uri.Builder builder = uri.buildUpon().clearQuery();
      for (String name : uri.getQueryParameterNames()) {
          if (!name.equals(QUERY_PARAMETER_NAME)) {
              for (String value : uri.getQueryParameters(name)) {
                  builder.appendQueryParameter(name, value);
              }
          }
      }
      return builder.build().toString();
    }
}
