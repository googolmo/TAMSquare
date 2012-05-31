package com.googolmo.fs.apps;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.googolmo.fs.Constants;
import com.googolmo.fs.R;
import com.googolmo.fs.utils.PreferenceUtil;

/**
 * .
 * User: googolmo
 * Date: 12-5-29
 * Time: 下午3:27
 */
public class OAuthActivity extends SherlockActivity {

    private WebView mWebView;
    private String mAccessToken;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauth);
        getSupportActionBar();
        setSupportProgressBarVisibility(true);

        this.setupView();
    }

    @Override
    public void finish() {

        mWebView.stopLoading();
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("accessToken", mAccessToken);
        intent.putExtras(bundle);
        if (getParent() == null) {
            setResult(RESULT_OK, intent);
        } else {
            getParent().setResult(RESULT_OK, intent);
        }
        super.finish();
    }

    private void setupView() {
        mWebView = (WebView)findViewById(R.id.oauth_webview);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(this.getClass().getName(), "onPageStarted");
                super.onPageStarted(view, url, favicon);
                String fragment = "#access_token=";
                int start = url.indexOf(fragment);

                if (start > -1) {
                    view.stopLoading();
                    mAccessToken = url.substring(start + fragment.length(), url.length());
                    PreferenceUtil.SetAccessToken(OAuthActivity.this, mAccessToken);
                    finish();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                setSupportProgress(Window.PROGRESS_END);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(this.getClass().getName(), "progress = " + newProgress);
                setSupportProgress(newProgress * 100);
                super.onProgressChanged(view, newProgress);
            }

        });
        String url = new StringBuilder("https://foursquare.com/oauth2/authenticate?client_id=")
                .append(Constants.CLIENTID)
                .append("&response_type=token")
                .append("&redirect_uri=")
                .append(Constants.REDIRECT_URL).toString();

        mWebView.loadUrl(url);

    }
}
