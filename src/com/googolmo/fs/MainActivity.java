package com.googolmo.fs;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.googolmo.fs.adapters.ImageAdapter;
import com.googolmo.fs.apps.CheckinActivity;
import com.googolmo.fs.apps.OAuthActivity;
import com.googolmo.fs.utils.PreferenceUtil;
import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.ResultMeta;
import fi.foyt.foursquare.api.entities.CompleteVenue;
import fi.foyt.foursquare.api.entities.Photo;
import fi.foyt.foursquare.api.entities.PhotoGroup;
import fi.foyt.foursquare.api.entities.VenueGroup;

import java.util.*;

public class MainActivity extends SherlockActivity {

    private String mOAuthToken;
    private CompleteVenue mVenue;
    private ActionBar mActionBar;
    private boolean mShowRefresh;

    private LoadAsyncTask mTask;
    private ArrayList<LoadAsyncTask> mTaskList;

    private ProgressBar mLoading;
//    private ScrollView mScrollView;
    private LinearLayout mTitleLayout;
    private TextView mTitleTextView;
    private TextView mSubTitleTextView;
    private TextView mBeenHeerTextView;
    private GridView mBeenHeerGridView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mActionBar = getSupportActionBar();



        mOAuthToken = PreferenceUtil.GetAccessToken(this);
        mTaskList = new ArrayList<LoadAsyncTask>();
        if (mOAuthToken.equals("")) {
            Intent intent = new Intent(this, OAuthActivity.class);
            startActivityForResult(intent, 1);
        } else {
            ((TAMApplication)getApplication()).getApi().setoAuthToken(mOAuthToken);
            mShowRefresh = false;
            refresh();
        }
        this.setupView();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MainActivity.class.getName(), "======================================= onResume" );
        //Toast.makeText(this, "Token:" + mOAuthToken, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(MainActivity.class.getName(), "==================================onActivityResult,requestCode=" + requestCode + "resultCode = " + resultCode + RESULT_OK);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                Bundle bundle = data.getExtras();
                mOAuthToken = bundle.getString("accessToken");
                Log.d(MainActivity.class.getName(), "accessToken = " + mOAuthToken);
                if (mOAuthToken != null && !mOAuthToken.equals("")) {
                    Log.d(MainActivity.class.getName(), "accessToken = " + mOAuthToken);
                    ((TAMApplication)getApplication()).getApi().setoAuthToken(mOAuthToken);
                    refresh();
                } else {
                    this.finish();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mShowRefresh) {
            menu.add(Menu.FIRST, Menu.FIRST, Menu.FIRST, "Refresh")
                    .setIcon(R.drawable.ic_action_refresh)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        if (mVenue != null) {
            menu.add(Menu.FIRST, Menu.FIRST + 1, Menu.FIRST, "Check in")
                    .setIcon(R.drawable.ic_action_checkin)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            refresh();
        } else if (item.getItemId() == Menu.FIRST + 1) {
            Intent intent = new Intent(this, CheckinActivity.class);
            intent.putExtra("venue", mVenue);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupView() {
        mLoading = (ProgressBar)findViewById(R.id.main_loading);
//        mScrollView = (ScrollView)findViewById(R.id.main_scrollview);
        mTitleLayout = (LinearLayout)findViewById(R.id.main_linearlayout1);
        mTitleTextView = (TextView)findViewById(R.id.main_title_text);
        mSubTitleTextView = (TextView)findViewById(R.id.main_subtitle_text);
        mBeenHeerTextView = (TextView)findViewById(R.id.main_beenheer_text);
        mBeenHeerGridView = (GridView)findViewById(R.id.main_beenheer_gridview);

        mLoading.setVisibility(View.VISIBLE);
//        mScrollView.setVisibility(View.GONE);
        mTitleLayout.setVisibility(View.GONE);
    }

    private void refresh() {
        mTask = new LoadAsyncTask();
        mTaskList.add(mTask);
        mTask.execute(Constants.TAMVENUE_ID);
    }

    private void getSuc(CompleteVenue result) {
        mVenue = result;
        mLoading.setVisibility(View.GONE);
//        mScrollView.setVisibility(View.VISIBLE);
        mTitleLayout.setVisibility(View.VISIBLE);

        mTitleTextView.setText(result.getName());
        String location = "";
        if (result.getLocation().getCountry() != null && result.getLocation().getCountry().length() > 0) {
            location += result.getLocation().getCountry() + ", ";
        }
        if (result.getLocation().getState() != null && result.getLocation().getState().length() > 0) {
            location += result.getLocation().getState() + ", ";
        }
        if (result.getLocation().getCity() != null && result.getLocation().getCity().length() > 0) {
            location += result.getLocation().getCity() + ", ";
        }
        if (result.getLocation().getAddress() != null && result.getLocation().getAddress().length() > 0) {
            location += result.getLocation().getAddress() + ", ";
        }
        mSubTitleTextView.setText(location);

        mBeenHeerTextView.setText(String.format(getString(R.string.main_beenheer_title_text), result.getBeenHere().getCount().toString()));

        List<Photo> photosList = new ArrayList<Photo>();
        for (int i = 0; i < result.getPhotos().getGroups().length; i ++) {
            PhotoGroup[] photoGroups = result.getPhotos().getGroups();
            for (int j = 0; j < photoGroups[i].getItems().length; j ++) {
                Photo photo = photoGroups[i].getItems()[j];
                photosList.add(photo);
            }
        }
        Toast.makeText(this, "Photo nums:" + photosList.size(), Toast.LENGTH_SHORT).show();
        ImageAdapter imageAdapter = new ImageAdapter(MainActivity.this, photosList);
        mBeenHeerGridView.setAdapter(imageAdapter);
        mBeenHeerGridView.setVisibility(View.VISIBLE);
    }

    private void getFail(ResultMeta result) {
        if (result != null) {
            Toast.makeText(this, result.getErrorDetail(), Toast.LENGTH_SHORT).show();
        }
    }


    private class LoadAsyncTask extends AsyncTask<String,FoursquareApiException,Result<CompleteVenue>> {
        @Override
        protected Result<CompleteVenue> doInBackground(String... params) {
            String venueid = Constants.TAMVENUE_ID;
            if (params.length > 0) {
                venueid = params[0];
            }
            try {
                return ((TAMApplication)getApplication()).getApi().venue(venueid);
            } catch (FoursquareApiException e) {
                e.printStackTrace();
                publishProgress(e);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mShowRefresh = false;
            setSupportProgressBarIndeterminate(true);
            setSupportProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }

        @Override
        protected void onPostExecute(Result<CompleteVenue> result) {
            super.onPostExecute(result);


            if (result == null) {
                getFail(null);
            } else {
                if (result.getMeta().getCode() == 200) {
                    getSuc(result.getResult());
                } else {
                    getFail(result.getMeta());
                }
            }
            mShowRefresh = true;
            setSupportProgressBarIndeterminate(false);
            setSupportProgressBarIndeterminateVisibility(false);
            invalidateOptionsMenu();
        }

        @Override
        protected void onProgressUpdate(FoursquareApiException... values) {
            super.onProgressUpdate(values);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (LoadAsyncTask task : mTaskList) {
            if (task.getStatus() == AsyncTask.Status.RUNNING) {
                task.cancel(true);
            }
        }
    }
}