package org.minijvm.activity;

import android.Manifest;
import android.app.NativeActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gust on 2018/4/19.
 */

public class JvmNativeActivity extends NativeActivity {
    static {
        System.loadLibrary("minijvm");
    }

    ClipboardManager mClipboardManager;
    InputMethodManager inputMethodManager;
    private final static String TAG = "JvmNativeActivity";

    // android:name="android.app.NativeActivity"
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClipboardManager = mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        PHOTO_DIR_SD = new File(getExternalFilesDir("").getAbsolutePath() + "/tmp");
        PHOTO_DIR_ROOT = new File(getFilesDir().getAbsolutePath() + "/tmp");
        requestAudioPermissions();
    }

    //============================================= audio record permission==================================================
    //Requesting run-time permissions
    //Create placeholder for user's consent to record_audio permission.
    //This will be used in handling callback
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            //recordAudio();
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    //recordAudio();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String str = event.getCharacters();
        if (str != null) {
            if (onStringInput(str)) return true;
        }
        return super.dispatchKeyEvent(event);
    }


    // Need this for screen rotation to send configuration changed callbacks to native
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void showKeyboard() {
        inputMethodManager.showSoftInput(this.getWindow().getDecorView(), InputMethodManager.SHOW_FORCED);
    }


    public void hideKeyboard() {
        inputMethodManager.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
    }

    public void setClipBoardContent(String str) {
        ClipData mClipData;
        mClipData = ClipData.newPlainText("", str);
        mClipboardManager.setPrimaryClip(mClipData);
    }

    public String getClipBoardContent() {
        ClipData clipData = mClipboardManager.getPrimaryClip();
        ClipData.Item item = clipData.getItemAt(0);
        String text = item.getText().toString();
        return text;
    }


    native boolean onStringInput(String str);

    //=======================================================================================================
    private static final int CAMERA_CODE = 0;
    private static final int GALLERY_CODE = 1;
    private static final int CROP_CODE = 2;
    //用于展示选择的图片

    static final int PARA_REQUEST_UID = 0;
    static final int PARA_REQUEST_TYPE = 1;
    int[][] pick_para=new int[3][2];

    private File PHOTO_DIR_SD;
    private File PHOTO_DIR_ROOT;
//    private final File PHOTO_DIR_SD = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
//    private final File PHOTO_DIR_ROOT = new File(Environment.getRootDirectory() + "/DCIM/Camera");

    /**
     * 拍照选择图片
     */
    public void pickFromCamera(int uid, int type) {
        //构建隐式Intent
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        pick_para[CAMERA_CODE][PARA_REQUEST_UID]=uid;
        pick_para[CAMERA_CODE][PARA_REQUEST_TYPE]=type;
        //调用系统相机
        startActivityForResult(intent, CAMERA_CODE);
    }

    /**
     * 从相册选择图片
     */
    public void pickFromAlbum(int uid, int type) {
        //构建一个内容选择的Intent
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        pick_para[GALLERY_CODE][PARA_REQUEST_UID]=uid;
        pick_para[GALLERY_CODE][PARA_REQUEST_TYPE]=type;
        //设置选择类型为图片类型
        intent.setType("image/*");
        //打开图片选择
        startActivityForResult(intent, GALLERY_CODE);
    }

    /**
     * 通过Uri传递图像信息以供裁剪
     *
     * @param uris
     */
    public void imageCrop(int uid, String uris, int x, int y, int width, int height) {
        //构建隐式Intent来启动裁剪程序
        Intent intent = new Intent("com.android.camera.action.CROP");
        //设置数据uri和类型为图片类型
        Uri uri = Uri.parse(uris);
        intent.setDataAndType(uri, "image/*");
        //显示View为可裁剪的
        intent.putExtra("crop", true);
        //裁剪的宽高的比例为1:1
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        //输出图片的宽高均为150
        intent.putExtra("outputX", width);
        intent.putExtra("outputY", height);
        //裁剪之后的数据是通过Intent返回
        intent.putExtra("return-data", true);
        pick_para[CROP_CODE][PARA_REQUEST_UID]=uid;
        pick_para[CROP_CODE][PARA_REQUEST_TYPE]=0;
        startActivityForResult(intent, CROP_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CAMERA_CODE:
                //用户点击了取消
                if (data == null) {
                    return;
                } else {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        //获得拍的照片
                        Bitmap bm = extras.getParcelable("data");
                        int uid = pick_para[CAMERA_CODE][PARA_REQUEST_UID];
                        int type = pick_para[CAMERA_CODE][PARA_REQUEST_TYPE];
                        if (type == 0) {
                            bm = resizeImage(bm);
                        }
                        //将Bitmap转化为uri
                        Uri uri = saveImage(bm, getPhotoStorage());
                        //启动图像裁剪
                        //imageCrop(uri, uid);
                        onPhotoPicked(uid, uri.getPath(), null);
                    }
                }
                break;
            case GALLERY_CODE:
                if (data == null) {
                    return;
                } else {
                    //用户从图库选择图片后会返回所选图片的Uri
                    Uri uri;
                    //获取到用户所选图片的Uri
                    uri = data.getData();
                    int uid = pick_para[GALLERY_CODE][PARA_REQUEST_UID];
                    int type = pick_para[GALLERY_CODE][PARA_REQUEST_TYPE];
                    //返回的Uri为content类型的Uri,不能进行复制等操作,需要转换为文件Uri
                    uri = convertUri(uri);
                    if (type == 0) {
                        Bitmap bm = BitmapFactory.decodeFile(uri.getPath());
                        bm = resizeImage(bm);
                        uri = saveImage(bm, getPhotoStorage());
                    }
                    onPhotoPicked(uid, uri.getPath(), null);
                }
                break;
            case CROP_CODE:
                if (data == null) {
                    return;
                } else {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        //获取到裁剪后的图像
                        Bitmap bm = extras.getParcelable("data");
                        int uid = pick_para[CROP_CODE][PARA_REQUEST_UID];
                        int type = pick_para[CROP_CODE][PARA_REQUEST_TYPE];
                        //mImageView.setImageBitmap(bm);
                        Uri uri = saveImage(bm, getPhotoStorage());
                        onPhotoPicked(uid, uri.getPath(), null);
                    }
                }
                break;
            default:
                break;
        }
    }

    static native void onPhotoPicked(int uid, String path, byte[] data);

    /**
     * 将content类型的Uri转化为文件类型的Uri
     *
     * @param uri
     * @return
     */
    private Uri convertUri(Uri uri) {
        InputStream is;
        try {
            //Uri ----> InputStream
            is = getContentResolver().openInputStream(uri);
            //InputStream ----> Bitmap
            Bitmap bm = BitmapFactory.decodeStream(is);
            //关闭流
            is.close();
            return saveImage(bm, getPhotoStorage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap resizeImage(Bitmap bm) {
        float iw = bm.getWidth();
        float ih = bm.getHeight();
        float ratio = 1024;
        float scale = iw / ratio > ih / ratio ? (iw / ratio) : (ih / ratio);
        if (scale > 1) {
            Matrix matrix = new Matrix();
            matrix.postScale(1 / scale, 1 / scale);
            // 得到新的图片
            Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, (int) iw, (int) ih, matrix, true);
            return newbm;
        }
        return bm;
    }

    /**
     * 将Bitmap写入SD卡中的一个文件中,并返回写入文件的Uri
     *
     * @param bm
     * @param dirPath
     * @return
     */
    private Uri saveImage(Bitmap bm, File dirPath) {


        //新建文件存储裁剪后的图片
        File img = new File(dirPath.getAbsolutePath() + "/" + getImgName());
        try {
            //打开文件输出流
            FileOutputStream fos = new FileOutputStream(img);
            //将bitmap压缩后写入输出流(参数依次为图片格式、图片质量和输出流)
            bm.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            //刷新输出流
            fos.flush();
            //关闭输出流
            fos.close();
            //返回File类型的Uri
            return Uri.fromFile(img);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private String getImgName() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyyMMdd_HHmmsss");
        StringBuilder result = new StringBuilder("IMG_");
        result.append(sdf.format(new Date())).append("_").append((System.currentTimeMillis() % 1000))
                .append(".jpeg");
        return result.toString();
    }

    private byte[] getFileData(Uri uri) {
        try {
            File f = new File(uri.getPath());
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                byte[] b = new byte[(int) f.length()];
                fis.read(b);
                return b;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private File getPhotoStorage() {
        File dir = null;
        // showToast(activity, "若添加实时拍摄照片导致重启，请尝试在应用外拍照，再选择从相册中获取进行添加！");
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {// 判断是否有SD卡
            dir = PHOTO_DIR_SD;
        } else {
            dir = PHOTO_DIR_ROOT;
        }
        if (!dir.exists()) {
            dir.mkdirs();// 创建照片的存储目录
        }
        return dir;
    }
//=======================================================================================================

}
