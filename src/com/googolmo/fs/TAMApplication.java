package com.googolmo.fs;

import android.app.Application;
import com.googolmo.fs.utils.PreferenceUtil;
import fi.foyt.foursquare.api.FoursquareApi;

/**
 * .
 * User: googolmo
 * Date: 12-5-29
 * Time: 下午2:07
 */
public class TAMApplication extends Application {

    private FoursquareApi foursquareApi;

    public FoursquareApi getApi() {
        if(foursquareApi == null) {
            foursquareApi = new FoursquareApi(Constants.CLIENTID, Constants.CLIENTSECRET, Constants.REDIRECT_URL);
            foursquareApi.setUseCallback(false);
            String token = PreferenceUtil.GetAccessToken(getApplicationContext());
            if (!token.equals("")) {
                foursquareApi.setoAuthToken(token);
            }
        }
        return foursquareApi;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

}
