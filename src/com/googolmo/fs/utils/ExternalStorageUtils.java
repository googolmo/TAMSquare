package com.googolmo.fs.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * User: googolmo
 * Date: 12-5-30
 * Time: 下午5:52
 */
public class ExternalStorageUtils {

    /**
     * 获得外部缓存目录，一般为：/sdcard/Android/data/<package_name>/cache/
     *
     * @param packageName
     * @return
     */
    public static File getExternalCacheDir(String packageName) {

        File extDir = Environment.getExternalStorageDirectory();
        if (extDir == null)
            return null;
        File dir = new File(extDir.getAbsolutePath() + File.separator + "Android" + File.separator + "data"
                + File.separator + packageName + File.separator + "cache");
        try{
            FileUtils.mkdirs(dir);
        }catch (Exception e){
            e.printStackTrace();
        }
        return dir;
    }

    /**
     * 获得外部存储文件目录，一般为：：/sdcard/Android/data/<package_name>/files/<type>/
     *
     * @param packageName
     * @param type
     * @return
     */
    public static File getExternalFilesDir(String packageName, String type) {
        File extDir = Environment.getExternalStorageDirectory();
        if (extDir == null)
            return null;

        if (type == null) type = "";

        File dir = new File(extDir.getAbsolutePath() + File.separator + "Android" + File.separator + "data"
                + File.separator + packageName + File.separator + "files" + File.separator + type);

        try{
            FileUtils.mkdirs(dir);
        }catch (Exception e){
            e.printStackTrace();
            Log.e(ExternalStorageUtils.class.getName(), "===============================================failed");
        }
        Log.d(ExternalStorageUtils.class.getName(), "file dir:" + dir);
        return dir;
    }

}
