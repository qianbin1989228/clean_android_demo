package com.qb;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.opencv.core.Mat;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private String CV_TAG = "IDCARD";
    private int REQUEST_CAPTURE_IMAGE = 1;
    private Uri fileUri;
    private Button processBtn;
    private Button takePicBtn;
    private Button selectPicBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iniLoadOpenCV();//加载OpenCV的本地库
        processBtn = (Button) findViewById(R.id.process_btn);//实例化按钮并添加事件响应
        processBtn.setOnClickListener(this);

        takePicBtn = (Button)this.findViewById(R.id.select_pic_btn);
        takePicBtn.setOnClickListener(this);

        selectPicBtn = (Button)this.findViewById(R.id.take_pic_btn);
        selectPicBtn.setOnClickListener(this);

        // 解决android 7.0系统拍照奔溃的问题
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
    }

    //加载OpenCV的本地库
    private void iniLoadOpenCV(){
        boolean success = OpenCVLoader.initDebug();
        if(success){
            Log.i(CV_TAG,"OpenCV Libraries loaded...");
        }else{
            Toast.makeText(this.getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
        }
    }

    //按钮事件
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_pic_btn://打开摄像头拍摄图片
                start2Camera();
                break;
            case R.id.select_pic_btn://从本地相册选择图片
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                } else {
                    pickUpImage();
                }
                break;
            case R.id.process_btn://智能检测
                detect();
                break;
            default:
                break;
        }
    }

    //智能检测
    private void detect() {
        Mat src = new Mat();
        if(fileUri == null)
        {
            // 读取默认图像
            Bitmap bmtemp = BitmapFactory.decodeResource(this.getResources(),R.drawable.xiaoyan);
            Utils.bitmapToMat(bmtemp,src);
        }
        else
        {
            // 读取摄像头或相册中图像
            src = Imgcodecs.imread(fileUri.getPath());
        }
        if(src.empty())
            return;

        Mat dst = new Mat();

        // 智能处理
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_GRAY2BGR);

        // 输出结果
        if(dst.cols() > 1000 || dst.rows() > 1000) //防止图像过大显示奔溃
            Imgproc.resize(dst, dst, new Size(dst.cols() / 4, dst.rows() / 4));

        Bitmap bitmap = Bitmap.createBitmap(dst.cols(),dst.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(dst, bitmap);

        ImageView iv = (ImageView)this.findViewById(R.id.sample_img);
        iv.setImageBitmap(bitmap);
        src.release();
        dst.release();
    }

    //启动相机拍照
    private void start2Camera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = Uri.fromFile(ImageSelectUtils.getSaveFilePath());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
    }

    //从本地相册选择相片
    private void pickUpImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(Intent.createChooser(intent, "图像选择..."), REQUEST_CAPTURE_IMAGE);

    }

    //图片回调显示到设备屏幕上
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CAPTURE_IMAGE && resultCode == RESULT_OK) {
            if(data != null) {
                Uri uri = data.getData();
                File f = new File(ImageSelectUtils.getRealPath(uri, getApplicationContext()));
                fileUri = Uri.fromFile(f);
            }
        }
        displaySelectedImage();
    }

    //显示图片（自动缩放）
    private void displaySelectedImage() {
        if(fileUri == null) return;
        ImageView imageView = (ImageView)this.findViewById(R.id.sample_img);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileUri.getPath(), options);
        int w = options.outWidth;
        int h = options.outHeight;
        int inSample = 1;
        if(w > 1000 || h > 1000) {
            while(Math.max(w/inSample, h/inSample) > 1000) {
                inSample *=2;
            }
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bm = BitmapFactory.decodeFile(fileUri.getPath(), options);
        imageView.setImageBitmap(bm);
    }
}

