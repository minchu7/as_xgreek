package org.oldservant.greekinterlinear;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import android.os.Vibrator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;

import static android.os.VibrationEffect.*;

public class MainActivity extends Activity {
    static phonegapSpeech speechObj =null;
    private WebView mWebView;
    private static final String LOG_TAG = "MainActivity";
    class JsObject {
        @JavascriptInterface
        public boolean isConnected(){
            return isConnect();
        }
        @JavascriptInterface
        public String exec( final String action, String jsonData){
            if( speechObj == null)
                speechObj = new phonegapSpeech(  getApplicationContext());
            if(jsonData==null){
                String ret = speechObj.execute( action, null);
                return ret;
            }
            try {
                JSONArray data = new JSONArray(jsonData); //Convert from string to object, can also use JSONArray
                return speechObj.execute( action, data);
            } catch (Exception ex) {
                Log.d( LOG_TAG, ex.getMessage());
            }
            return "error";
        }
        @JavascriptInterface
        public void vibrate() {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE));
            }else{
                //deprecated in API 26
                v.vibrate(500);
            }
        }
        @JavascriptInterface
        public String readXML( String filename) {
            AssetManager assets = MainActivity.this.getAssets();

            StringBuilder ReturnString = new StringBuilder();
            InputStream fIn = null;
            InputStreamReader isr = null;
            BufferedReader input = null;
            try {
                fIn = assets.open("verses/"+filename);
                isr = new InputStreamReader(fIn);
                input = new BufferedReader(isr);
                String line = "";
                while ((line = input.readLine()) != null) {
                    ReturnString.append(line);
                }
            } catch (Exception e) {
                e.getMessage();
            } finally {
                try {
                    if (isr != null)
                        isr.close();
                    if (fIn != null)
                        fIn.close();
                    if (input != null)
                        input.close();
                } catch (Exception e2) {
                    e2.getMessage();
                }
            }
            return ReturnString.toString();
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //setContentView( mWebView );//to prevant scroll to top
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        } else{
            Toast.makeText(this, "others"+newConfig.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    //check if internet is connected
    public boolean isConnect(){
        final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        //final View controlsView = findViewById(R.id.fullscreen_content_controls);
        String packageName = this.getPackageName();
        mWebView = (WebView) findViewById(R.id.fullscreen_content);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new JsObject(), "injectedObject");

        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        //mWebView.getSettings().setDatabasePath("/data/data/" + packageName + "/databases");
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        mWebView.setClickable( true);
        WebView.setWebContentsDebuggingEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // Make the bar disappear after URL is loaded, and changes
                // string to Loading...
                setProgressBarIndeterminateVisibility(true);
                MainActivity.this.setTitle("  Loading . . . " + progress + "%");
                MainActivity.this.setProgress(progress * 100); // Make the bar
                if (progress == 100) {
                    setTitle("  APP Greek Interlinear");
                    setProgressBarIndeterminateVisibility(false);
                }
            }
        });
        mWebView.setWebViewClient( new WebViewClient(){
            public boolean shouldOverrideUrlLoading( WebView view, String Url){
                // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Url));
                startActivity(intent);
                return true;
            }
            public void onPageStarted (WebView view, String url, Bitmap favicon){
                Toast.makeText( MainActivity.this, "PageStarted:"+url, Toast.LENGTH_SHORT).show();
            }
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }
        });
        mWebView.loadUrl("file:///android_asset/index.html");
    }
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "onBackPressed", Toast.LENGTH_SHORT).show();
        mWebView.loadUrl("javascript:goBackMain()");
    }
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event){
        if( keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()){
            mWebView.loadUrl("javascript:goBackMain()");
            return true;
        }
        return super.onKeyDown( keyCode, event);
    }
    @Override
    public void onSaveInstanceState( Bundle outState){
        super.onSaveInstanceState( outState);
        //setContentView(mWebView);
        mWebView.saveState( outState);
    }
    boolean fStop;
    @Override
    protected void onStop() {
        super.onStop();
        fStop = true;
    }
    @Override
    public void onRestoreInstanceState( Bundle savedState){
        super.onRestoreInstanceState( savedState);
        if( !fStop)
            setContentView(mWebView);
        else
            mWebView.restoreState( savedState);
    }
    /*
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     *
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     *
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
    */
}
