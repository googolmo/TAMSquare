package com.googolmo.fs.utils;

import java.io.*;

/**
 * User: googolmo
 * Date: 12-5-30
 * Time: 下午5:52
 */
public class FileUtils {

    public static boolean isFileExists(String dir, String fileName){
        File file = new File(dir + File.separator + fileName);
        return file.exists();
    }


    /**
     * 将数据写入目录下的文件，如果文件不存在，会自动创建，如果文件存在，会覆盖。
     *
     * @param in 要存放的数据
     * @param dir 数据存放到的目录
     * @param fileName 数据存放到的文件名称
     */
    public static boolean writeStreamToFile(InputStream in, String dir, String fileName) {
        if (in == null || dir == null || fileName == null)
            return false;

        File directory = new File(dir);

        if (!mkdirs(directory))
            return false;

        File target = new File(dir + File.separator + fileName);

        try {
            if (!target.exists()) {
                target.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(target);

            int b;
            do {
                b = in.read();
                if (b != -1) {
                    out.write(b);
                }
            } while (b != -1);
            in.close();
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                target.delete();
            } catch (Exception ex) {
            }
            return false;
        }

    }


    /**
     * 如果目录不存在，创建目录
     *
     * @param dir
     * @return
     */
    public static boolean mkdirs(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return dir.mkdirs();
        }
        return true;
    }

    /**
     * 使用FileInputStream打开指定的文件
     *
     * @param dir 文件的目录
     * @param fileName 文件的名称
     * @return
     */

    public static FileInputStream openInputStream(String dir, String fileName) {
        return openInputStream(dir + File.separator + fileName);
    }

    /**
     * 使用FileInputStream打开指定的文件
     *
     * @param filePath 文件的绝对路径
     * @return
     */
    public static FileInputStream openInputStream(String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获得文件的扩展名
     *
     * @param fileName
     * @return
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null)
            return null;
        int pos = fileName.lastIndexOf(".");

        if (pos > -1 && pos < fileName.length()) {
            return fileName.substring(pos + 1);
        } else {
            return "";
        }
    }

    /**
     * 获得url的文件名
     *
     * @param url
     * @return
     */
    public static String getFileName(String url) {
        if (url == null)
            return null;
        int pos = url.lastIndexOf("/");

        if (pos > -1 && pos < url.length()) {
            return url.substring(pos + 1);
        } else {
            return "";
        }
    }

}
