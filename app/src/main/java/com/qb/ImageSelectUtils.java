package com.qb;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gloomy fish on 2017/6/29.
 */

public class ImageSelectUtils {
    private static String TAG = "ImageSelectUtils";
    public static File getSaveFilePath() {
        String status = Environment.getExternalStorageState();
        if(!status.equals(Environment.MEDIA_MOUNTED)) {
            Log.i(TAG, "SD Card is not suitable...");
            return null;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String name = df.format(new Date(System.currentTimeMillis()))+ ".jpg";
        File filedir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "myOcrImages");
        filedir.mkdirs();
        String fileName = filedir.getAbsolutePath() + File.separator + name;
        File imageFile = new File(fileName);
        return imageFile;
    }


    public static String getRealPath(Uri uri, Context appContext) {
        String filePath = null;
//        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){//4.4及以上
//            String wholeID = DocumentsContract.getDocumentId(uri);
//            String id = wholeID.split(":")[1];
//            String[] column = { MediaStore.Images.Media.DATA };
//            String sel = MediaStore.Images.Media._ID + "=?";
//            Cursor cursor = appContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column,
//                    sel, new String[] { id }, null);
//            int columnIndex = cursor.getColumnIndex(column[0]);
//            if (cursor.moveToFirst()) {
//                filePath = cursor.getString(columnIndex);
//            }
//            cursor.close();
//        }else{//4.4以下，即4.4以上获取路径的方法
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = appContext.getContentResolver().query(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(column_index);
        //}
        Log.i(TAG, "selected image path : " + filePath);
        return filePath;
    }

    public static void saveImage(Mat image) {
        File fileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "mybook");
        if(!fileDir.exists()) {
            fileDir.mkdirs();
        }
        String name = String.valueOf(System.currentTimeMillis()) + "_book.jpg";
        File tempFile = new File(fileDir.getAbsoluteFile()+File.separator, name);
        Imgcodecs.imwrite(tempFile.getAbsolutePath(), image);
    }

    public static void RecursiveCreateDirectories(String fileDir) {
        String[] fileDirs = fileDir.split("\\/");
        String topPath = "";
        for (int i = 0; i < fileDirs.length; i++) {
            topPath += "/" + fileDirs[i];
            File file = new File(topPath);
            if (file.exists()) {
                continue;
            } else {
                file.mkdir();
            }
        }
    }

    public static void copyFileFromAssets(Context appCtx, String srcPath, String dstPath) {
        if (srcPath.isEmpty() || dstPath.isEmpty()) {
            return;
        }
        String dstDir = dstPath.substring(0, dstPath.lastIndexOf('/'));
        if (dstDir.length() > 0) {
            RecursiveCreateDirectories(dstDir);
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(appCtx.getAssets().open(srcPath));
            os = new BufferedOutputStream(new FileOutputStream(new File(dstPath)));
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyDirectoryFromAssets(Context appCtx, String srcDir, String dstDir) {
        if (srcDir.isEmpty() || dstDir.isEmpty()) {
            return;
        }
        try {
            if (!new File(dstDir).exists()) {
                new File(dstDir).mkdirs();
            }
            for (String fileName : appCtx.getAssets().list(srcDir)) {
                String srcSubPath = srcDir + File.separator + fileName;
                String dstSubPath = dstDir + File.separator + fileName;
                if (new File(srcSubPath).isDirectory()) {
                    copyDirectoryFromAssets(appCtx, srcSubPath, dstSubPath);
                } else {
                    copyFileFromAssets(appCtx, srcSubPath, dstSubPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap zoomImg2(Bitmap bm, int targetWidth, int targetHeight) {
        int srcWidth = bm.getWidth();
        int srcHeight = bm.getHeight();
        float widthScale = targetWidth * 1.0f / srcWidth;
        float heightScale = targetHeight * 1.0f / srcHeight;
        Matrix matrix = new Matrix();
        matrix.postScale(widthScale, heightScale, 0, 0);
        // 如需要可自行设置 Bitmap.Config.RGB_8888 等等
        Bitmap bmpRet = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpRet);
        Paint paint = new Paint();
        canvas.drawBitmap(bm, matrix, paint);
        return bmpRet;
    }

    public static Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        try {
            Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            return bm1;
        } catch (OutOfMemoryError ex) {
        }
        return null;
    }

}
