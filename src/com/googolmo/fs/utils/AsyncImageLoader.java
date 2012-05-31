package com.googolmo.fs.utils;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Handler;
import android.os.Message;

import android.text.TextUtils;

import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;

import android.widget.ImageView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.lang.Runnable;
import java.lang.ref.SoftReference;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: googolmo
 * Date: 12-5-30
 * Time: 下午5:48
 */
public class AsyncImageLoader {

        public interface ImageAttacher{
            /**
             * 当图片加载完成后，回调此函数，改函数在子线程执行
             *
             */
            public Bitmap prepare(Bitmap bitmap);

            /**
             * 当图片准备完毕，回调此函数。该函数在主线程（UI线程）执行
             *
             *
             */
            public void attach(Bitmap bitmap);
        }

        /**
         * 缺省的Attacher，当图片下载完成后，直接将图片显示在指定的ImageView中
         */
        public static class DefaultImageAttacher implements ImageAttacher {

            private ImageView v;
            private String fileName;

            public DefaultImageAttacher(ImageView v, String fileName) {
                this.v = v;
                this.fileName = fileName;
            }

            public Bitmap prepare(Bitmap bitmap){
                return bitmap;
            }

            public void attach(Bitmap bitmap) {
                String fileName0 = (String) v.getTag(v.toString().hashCode());
                if (v != null && (fileName0 == null || fileName0.equals(fileName))){
                    v.setImageBitmap(bitmap);
                }
            }
        }

        public static class FadeinImageAttacher implements ImageAttacher {

            private ImageView v;
            private String fileName;

            public FadeinImageAttacher(ImageView v, String fileName) {
                this.v = v;
                this.fileName = fileName;
            }

            public Bitmap prepare(Bitmap bitmap){
                return bitmap;
            }

            public void attach(Bitmap bitmap) {
                String fileName0 = (String) v.getTag(v.toString().hashCode());
                if (v != null && (fileName0 == null || fileName0.equals(fileName))){
                    v.setImageBitmap(bitmap);
                    final AlphaAnimation fadein = new AlphaAnimation(0.1f, 1.0f);
                    fadein.setDuration(500);
                    fadein.setInterpolator(new AccelerateDecelerateInterpolator());
                    fadein.setRepeatCount(0);
                    v.startAnimation(fadein);
                }
            }

        }

        /**
         * 设置图片最大宽度，图片加载时使用，默认640px
         *
         * @param width 图片最大宽度
         */
        public static void setMaxWidth(int width){
            MAX_WIDTH = width;
        }

        public static void enableLocalCache(boolean enable){
            LOCAL_CACHE = enable;
        }

        /**
         * 异步下载url所对应的图片，图片下载完成后，使用ImageView显示
         *
         * @param context
         * @param url 图片的网络URL地址
         * @param iv 要显示图片的ImageView
         */
        public static void loadImage(Context context, final String url, final ImageView iv) {
            loadImage(context, url, iv, -1, false);
        }


        /**
         * 异步下载url所对应的图片，图片下载完成后，使用ImageView显示
         *
         * @param context
         * @param url 图片的网络URL地址
         * @param iv 要显示图片的ImageView
         * @param defaultResource，在下载过程中使用的缺省图片资源
         * @param fadeinAnim，加载完显示时是否使用渐入的动画
         */
        public static void loadImage(final Context context, final String url, final ImageView iv,
                                     final int defaultResource, final boolean fadeinAnim) {

            // Failsafe：如果url为空，不做继续处理
            // （如果有指定则让ImageView显示默认资源）
            if (TextUtils.isEmpty(url)) {
                if (defaultResource > 0){
                    iv.setImageResource(defaultResource);
                }
                return;
            }

            final String fileName = getFileName(url);
            final ImageAttacher attacher = fadeinAnim ? new FadeinImageAttacher(iv, fileName) : new DefaultImageAttacher(iv, fileName);

            // 防止闪烁，在显示下载完成的图片时，判断此图片是否是应该显示的图片
            iv.setTag(iv.toString().hashCode(), fileName);

            // 如果命中Cache
            Bitmap bitmap = getBitmapFromCache(fileName);
            if (bitmap != null) {
                iv.setImageBitmap(bitmap);
                return;
            }

            // 如果需要由sdcard或者是网络获取图片，则异步处理，先设置缺省图片
            if (defaultResource > 0){
                iv.setImageResource(defaultResource);
            }else{
                iv.setImageDrawable(null);
            }

            async(context, url, fileName, attacher);

        }

        /**
         * 异步下载url所对应的图片，图片下载完成后，回调attacher
         *
         * @param context
         * @param url
         * @param attacher
         */
        public static void loadImage(final Context context, final String url, final ImageAttacher attacher) {
            // Failsafe：如果url为空，不做继续处理
            if (TextUtils.isEmpty(url)) {
                return;
            }

            String fileName = getFileName(url);
            async(context, url, fileName, attacher);
        }

        private static final String TAG = "AID";
        private static int MAX_WIDTH = 640;
        private static boolean LOCAL_CACHE = true;

        private static final int CORE_POOL_SIZE = 2;
        private static final int MAXIMUM_POOL_SIZE = 4;
        private static final int CORE_POOL_SIZE2 = 1;
        private static final int MAXIMUM_POOL_SIZE2 = 2;
        private static final int KEEP_ALIVE = 1;

        private static final BlockingQueue<Runnable> sWorkQueue = new LinkedBlockingQueue<Runnable>(30);
        private static final BlockingQueue<Runnable> sWorkQueue2 = new LinkedBlockingQueue<Runnable>(10);

        private static final int HARD_CACHE_CAPACITY = 64;
        private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

        // Hard cache, with a fixed maximum capacity and a life duration
        private static final HashMap<String, Bitmap> sHardBitmapCache =
                new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {

//                    public boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
//                        if (size() > HARD_CACHE_CAPACITY) {
//                            // Entries push-out of hard reference cache are transferred to soft reference cache
//                            sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
//                            return true;
//                        } else{
//                            return false;
//                        }
//                    }
                };

        // Soft cache for bitmaps kicked out of hard cache
        private static final ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache =
                new ConcurrentHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);

        private final static Handler purgeHandler = new Handler();

        private final static Runnable purger = new Runnable() {
            public void run() {
                clearCache();
            }
        };

        /**
         * Allow a new delay before the automatic cache clear is done.
         */
        private static void resetPurgeTimer() {
            purgeHandler.removeCallbacks(purger);
            purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
        }

        /**
         * Clears the image cache used internally to improve performance. Note that for memory
         * efficiency reasons, the cache will automatically be cleared after a certain inactivity delay.
         */
        public static void clearCache() {
            sHardBitmapCache.clear();
            sSoftBitmapCache.clear();
        }

        public static void clearFiles(Context context){
            String dir = getFileDir(context);
            try{
                File directory = new File(dir);
                if (directory.exists()){
                    directory.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private static final ThreadFactory sThreadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "AsyncImageLoader #" + mCount.getAndIncrement());
            }
        };

        private static final ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE, TimeUnit.SECONDS, sWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());

        private static final ThreadPoolExecutor sExecutor2 = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE2,
                KEEP_ALIVE, TimeUnit.SECONDS, sWorkQueue2, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());

        private static String getFileDir(Context context) {
            String type = "Pictures";
            return ExternalStorageUtils.getExternalFilesDir(context.getPackageName(), type).getAbsolutePath();
        }

        public static String getFileName(String url) {
            String md5 = DigestUtils.md5Hex(url);
            return md5;
        }

        private static Bitmap createBitmap(String fileDir, String fileName) {
            Bitmap pic = null;
            try {
                InputStream stream = FileUtils.openInputStream(fileDir, fileName);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                pic = BitmapFactory.decodeStream(stream, null, options);
                int be = options.outWidth / MAX_WIDTH;
                if (be < 0) be = 1;
                options.inSampleSize = be;
                options.inJustDecodeBounds = false;
                if (options.outWidth > MAX_WIDTH){
                    options.inScaled = true;
                    options.inDensity = options.outWidth;
                    options.inTargetDensity = MAX_WIDTH;
                }
                try {
                    if (stream != null) stream.close();
                } catch (Exception e) {
                }
                stream = FileUtils.openInputStream(fileDir, fileName);
                pic = BitmapFactory.decodeStream(stream, null, options);
                try {
                    if (stream != null) stream.close();
                } catch (Exception e) {
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
            return pic;
        }


        private static Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Object[] data = (Object[]) message.obj;
                Bitmap bitmap = (Bitmap) data[0];
                ImageAttacher attacher = (ImageAttacher) data[1];
                attacher.attach(bitmap);
            }
        };


        /**
         * Adds this bitmap to the cache.
         * @param bitmap The newly downloaded bitmap.
         */
        private static void addBitmapToCache(String url, Bitmap bitmap) {
            if (bitmap != null) {
                synchronized (sHardBitmapCache) {
                    sHardBitmapCache.put(url, bitmap);
                }
            }
        }

        /**
         * @param url The URL of the image that will be retrieved from the cache.
         * @return The cached bitmap or null if it was not found.
         */
        private static Bitmap getBitmapFromCache(String url) {
            // First try the hard reference cache
            synchronized (sHardBitmapCache) {
                final Bitmap bitmap = sHardBitmapCache.get(url);
                if (bitmap != null) {
                    // Bitmap found in hard cache
                    // Move element to first position, so that it is removed last
                    sHardBitmapCache.remove(url);
                    sHardBitmapCache.put(url, bitmap);
                    return bitmap;
                }
            }


            // Then try the soft reference cache
            SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(url);
            if (bitmapReference != null) {
                final Bitmap bitmap = bitmapReference.get();
                if (bitmap != null) {
                    // Bitmap found in soft cache
                    return bitmap;
                } else {
                    // Soft reference has been Garbage Collected
                    sSoftBitmapCache.remove(url);
                }
            }

            return null;
        }

        private static Bitmap getLocalBitmap(String fileDir, String fileName) {
            resetPurgeTimer();
            Bitmap bitmap = getBitmapFromCache(fileName);
            if (bitmap != null) {
                return bitmap;
            }else{
                bitmap = createBitmap(fileDir, fileName);
                if (LOCAL_CACHE){
                    addBitmapToCache(fileName, bitmap);
                }
            }
            return bitmap;
        }

        /**
         * 异步加载图片并回调
         *
         * @param context
         * @param url
         * @param fileName
         * @param attacher
         */
        private static void async(final Context context, final String url, final String fileName, final ImageAttacher attacher) {

            final String fileDir = getFileDir(context);
            Log.d(AsyncImageLoader.class.getName(), "==========================filedir:" + fileDir);

            if (FileUtils.isFileExists(fileDir, fileName)){
                sExecutor2.execute(new Runnable() {

                    public void run() {

                        // 由sdcard中加载图片
                        Bitmap bitmap = getLocalBitmap(fileDir, fileName);

                        if (LOCAL_CACHE){
                            addBitmapToCache(fileName, bitmap);
                        }

                        bitmap = attacher.prepare(bitmap);
                        Message msg = handler.obtainMessage();
                        msg.obj = new Object[] { bitmap, attacher };
                        handler.sendMessage(msg);
                    }
                });

            }else{



                sExecutor.execute(new Runnable() {

                    public void run() {

                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get(url, new AsyncHttpResponseHandler(){
                            /**
                             * Fired when a request returns successfully, override to handle in your own code
                             *
                             * @param content the body of the HTTP response from the server
                             */
                            @Override
                            public void onSuccess(byte[] content) {
                                super.onSuccess(content);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(content, 0, content.length);

                                if (LOCAL_CACHE) {
                                    addBitmapToCache(fileName, bitmap);
                                }
                                bitmap = attacher.prepare(bitmap);
                                Message msg = handler.obtainMessage();
                                msg.obj = new Object[] {bitmap, attacher};
                                handler.sendMessage(msg);
                            }
                        });

//                        Bitmap bitmap = getNetBitmap(url, fileDir, fileName);
//
//                        if (LOCAL_CACHE){
//                            addBitmapToCache(fileName, bitmap);
//                        }
//
//                        bitmap = attacher.prepare(bitmap);
//                        Message msg = handler.obtainMessage();
//                        msg.obj = new Object[] { bitmap, attacher };
//                        handler.sendMessage(msg);
                    }
                });

            }

        }


    }
