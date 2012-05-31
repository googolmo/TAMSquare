package com.googolmo.fs.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.googolmo.fs.Constants;

/**
 * .
 * User: googolmo
 * Date: 12-5-29
 * Time: 下午1:41
 */
public class PreferenceUtil {

    public static String PREFERENCE_ACCESSTOKEN = "accessToken";

    public static String GetAccessToken(Context context) {
//        context.get
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREFERENCE_ACCESSTOKEN,"");
    }

    public static void SetAccessToken(Context context, String accessToken) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PREFERENCE_ACCESSTOKEN,accessToken);
        editor.commit();
    }
}
