package org.minijvm.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NativeActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;

import org.minijvm.activity.bridge.JsonPrinter;
import org.minijvm.activity.bridge.RMCDescriptor;
import org.minijvm.activity.bridge.ReflectUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

        // 沉浸式模式
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);


        // 键盘处理
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        decorView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                boolean isKeyboardVisible;
                int keyboardHeight;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
                    isKeyboardVisible = insets.isVisible(WindowInsets.Type.ime());
                    keyboardHeight = insets.getInsets(WindowInsets.Type.ime()).bottom;
                } else {
                    // Fallback for older APIs using deprecated methods
                    int bottomInset = insets.getSystemWindowInsetBottom();
                    // A common heuristic: if the bottom inset is larger than the stable inset,
                    // the keyboard is visible. The stable inset is for permanent system bars.
                    int stableBottomInset = insets.getStableInsetBottom();
                    isKeyboardVisible = bottomInset > stableBottomInset;
                    keyboardHeight = isKeyboardVisible ? bottomInset : 0;
                }

                onKeyboardHeightChanged(isKeyboardVisible, keyboardHeight);
                return insets;
            }
        });
        //test();
    }

    void test() {
        Window window = getWindow();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        View view = window.getDecorView();
        android.graphics.Rect rect = new android.graphics.Rect();
        view.getWindowVisibleDisplayFrame(rect);

        Context context = getApplicationContext();
        InputMethodManager service = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        service.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        view.getWindowVisibleDisplayFrame(rect);

        IBinder binder = view.getWindowToken();
        service.hideSoftInputFromWindow(binder, 0);
        view.getWindowVisibleDisplayFrame(rect);
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

    public long playVideo(String path, String mimeType) {
        Intent intent = new Intent(this, JvmNativeActivity.VideoViewPlayActivity.class);
        intent.putExtra("path", path);
        intent.putExtra("mimeType", mimeType);
        startActivity(intent);
        return 1;
    }


    native boolean onStringInput(String str);

    native void onKeyboardHeightChanged(boolean visible, int height);

    //=======================================================================================================
    private static final int CAMERA_IMAGE_CODE = 0;
    private static final int CAMERA_VIDEO_CODE = 1;
    private static final int GALLERY_CODE = 2;
    private static final int CROP_CODE = 3;
    private static final int CAPTURE_MEDIA_RESULT_CODE = 4;
    //用于展示选择的图片

    static final int PARA_REQUEST_UID = 0;
    static final int PARA_REQUEST_TYPE = 1;
    int[][] pick_para = new int[5][2];
    Uri imageUri, videoUri;

    private File PHOTO_DIR_SD;
    private File PHOTO_DIR_ROOT;
//    private final File PHOTO_DIR_SD = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
//    private final File PHOTO_DIR_ROOT = new File(Environment.getRootDirectory() + "/DCIM/Camera");


    public static int GLFMPickupTypeNoDef = 0;
    public static int GLFMPickupTypeImage = 1;
    public static int GLFMPickupTypeVideo = 2;

    /**
     * 拍照选择图片
     */
    public void pickFromCamera(int uid, int type) {
        //构建隐式Intent

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        pick_para[CAMERA_IMAGE_CODE][PARA_REQUEST_UID] = uid;
        pick_para[CAMERA_IMAGE_CODE][PARA_REQUEST_TYPE] = type;
//        imageUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
//        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        //takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);

        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
        videoUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
        //takeVideoIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        pick_para[CAMERA_VIDEO_CODE][PARA_REQUEST_UID] = uid;
        pick_para[CAMERA_VIDEO_CODE][PARA_REQUEST_TYPE] = type;

        Intent chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeVideoIntent});


        if (type == GLFMPickupTypeImage) {
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_IMAGE_CODE);
            }
        } else if (type == GLFMPickupTypeVideo) {
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takeVideoIntent, CAMERA_VIDEO_CODE);
            }
        } else {
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(chooserIntent, CAPTURE_MEDIA_RESULT_CODE);
            }
        }
    }

    /**
     * 从相册选择图片
     */
    public void pickFromAlbum(int uid, int type) {
        //构建一个内容选择的Intent
        String itype = "";
        if (type == GLFMPickupTypeImage) {
            itype = "image/*";
        } else if (type == GLFMPickupTypeVideo) {
            itype = "video/*";
        } else {
            itype = "*/*";
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        pick_para[GALLERY_CODE][PARA_REQUEST_UID] = uid;
        pick_para[GALLERY_CODE][PARA_REQUEST_TYPE] = type;
        intent.setType(itype);
//        String[] mimetypes = {"image/*", "video/*"};
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);//android 4.4 api>=19
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
        pick_para[CROP_CODE][PARA_REQUEST_UID] = uid;
        pick_para[CROP_CODE][PARA_REQUEST_TYPE] = 0;
        startActivityForResult(intent, CROP_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case CAPTURE_MEDIA_RESULT_CODE: {
                if (intent == null) {
                    return;
                } else {
                    Uri file = intent.getData();

                    if (file != null && file.getPath().endsWith(".mp4")) {
                        onActivityResult(CAMERA_VIDEO_CODE, resultCode, intent);
                    } else {
                        onActivityResult(CAMERA_IMAGE_CODE, resultCode, intent);
                    }
                }
                break;
            }
            case CAMERA_VIDEO_CODE: {
                try {
                    int uid = pick_para[CAMERA_VIDEO_CODE][PARA_REQUEST_UID];
                    Uri uri = intent.getData();
                    onPhotoPicked(uid, uri.getPath(), null);
                } catch (Exception io_e) {
                }
                break;
            }
            case CAMERA_IMAGE_CODE: {
                //用户点击了取消
                if (intent == null) {
                    return;
                } else {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        //获得拍的照片
                        Bitmap bm = extras.getParcelable("data");
                        int uid = pick_para[CAMERA_IMAGE_CODE][PARA_REQUEST_UID];
                        int type = pick_para[CAMERA_IMAGE_CODE][PARA_REQUEST_TYPE];
                        bm = resizeImage(bm);

                        //将Bitmap转化为uri
                        Uri uri = saveImage(bm);
                        //启动图像裁剪
                        //imageCrop(uri, uid);
                        onPhotoPicked(uid, uri.getPath(), null);
                    }
                }
                break;
            }
            case GALLERY_CODE: {
                if (intent == null) {
                    return;
                } else {
                    //用户从图库选择图片后会返回所选图片的Uri
                    Uri uri;
                    //获取到用户所选图片的Uri
                    uri = intent.getData();
                    int uid = pick_para[GALLERY_CODE][PARA_REQUEST_UID];
                    int type = pick_para[GALLERY_CODE][PARA_REQUEST_TYPE];
                    //返回的Uri为content类型的Uri,不能进行复制等操作,需要转换为文件Uri
                    uri = convertUri(uri);
                    if (type == 0) {
                        Bitmap bm = BitmapFactory.decodeFile(uri.getPath());
                        bm = resizeImage(bm);
                        uri = saveImage(bm);
                    }
                    onPhotoPicked(uid, uri.getPath(), null);
                }
                break;
            }
            case CROP_CODE: {
                if (intent == null) {
                    return;
                } else {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        //获取到裁剪后的图像
                        Bitmap bm = extras.getParcelable("data");
                        int uid = pick_para[CROP_CODE][PARA_REQUEST_UID];
                        int type = pick_para[CROP_CODE][PARA_REQUEST_TYPE];
                        //mImageView.setImageBitmap(bm);
                        Uri uri = saveImage(bm);
                        onPhotoPicked(uid, uri.getPath(), null);
                    }
                }
                break;
            }
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
            return saveImage(bm);
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
     * @return
     */
    private Uri saveImage(Bitmap bm) {


        //新建文件存储裁剪后的图片
        File img = getOutputMediaFile(MEDIA_TYPE_IMAGE);
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

//    private String getImgName() {
//        SimpleDateFormat sdf = new SimpleDateFormat();
//        sdf.applyPattern("yyyyMMdd_HHmmsss");
//        StringBuilder result = new StringBuilder("IMG_");
//        result.append(sdf.format(new Date())).append("_").append((System.currentTimeMillis() % 1000))
//                .append(".jpeg");
//        return result.toString();
//    }

//    private byte[] getFileData(Uri uri) {
//        try {
//            File f = new File(uri.getPath());
//            if (f.exists()) {
//                FileInputStream fis = new FileInputStream(f);
//                byte[] b = new byte[(int) f.length()];
//                fis.read(b);
//                return b;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


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

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a file Uri for saving an image or video
     */
    private Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = getPhotoStorage();
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("minipack", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    //=======================================================================================================
    //    playback
    //=======================================================================================================
    public static class VideoViewPlayActivity extends Activity {
        public MediaController mc;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Intent intent = getIntent();
            String path = intent.getStringExtra("path");
            String mimeType = intent.getStringExtra("mimeType");


            setContentView(R.layout.activity_video_view);
            final VideoView videoView = (VideoView) findViewById(R.id.videoView);
            final Button closeButton = (Button) findViewById(R.id.closeButton);
            closeButton.getBackground().setAlpha(0);
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {//实际处理button的click事件的方法
                    VideoViewPlayActivity.this.finish();
                }
            });

            //加载指定的视频文件
            //String path = "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4";//Environment.getExternalStorageDirectory().getPath()+"/20180730.mp4";
            videoView.setVideoPath(path);
            videoView.start();
            //创建MediaController对象
            MediaController mediaController = new MediaController(this) {
//                @Override
//                public void hide() {
//                    super.show();
//                }
            };
            mediaController.show();

            //VideoView与MediaController建立关联
            videoView.setMediaController(mediaController);
            mc = mediaController;

            //让VideoView获取焦点
            videoView.requestFocus();


        }
    }


    //=======================================================================================================
    //                   open app
    //=======================================================================================================

    /**
     * 检查包是否存在
     *
     * @param packname
     * @return
     */
    private boolean checkPackInfo(String packname) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null;
    }

    public int openOtherApp(String urls, String more, int detectAppInstalled) {
        try {

            if (more != null && more.equals("URL")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urls));
                startActivity(intent);
            } else {
                if (detectAppInstalled != 0) {
                    if (!checkPackInfo(urls)) {
                        return 1;
                    }
                }
                String[] paras = urls.split(" ");
                String pkgName = paras[0];
                String activityName = paras[1];

                Intent intent = new Intent(Intent.ACTION_MAIN);
                ComponentName cmp = new ComponentName(pkgName, activityName);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setComponent(cmp);
                startActivity(intent);
                return 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    //=======================================================================================================
    //                   alipay
    //=======================================================================================================


    /**
     * 支付宝支付业务示例
     */
    public Map<String, String> payV2(String orderInfo) {
        Thread t = new Thread(() -> {
            PayTask alipay = new PayTask(JvmNativeActivity.this);
            Map<String, String> result = alipay.payV2(orderInfo, true);
            Log.i("msp", result.toString());
        });
        t.start();
        return new HashMap<>();

    }

    /**
     * 支付宝账户授权业务示例
     */
    public Map<String, String> authV2(String authInfo) {
        Thread t = new Thread(() -> {
            // 构造AuthTask 对象
            AuthTask authTask = new AuthTask(JvmNativeActivity.this);
            // 调用授权接口，获取授权结果
            Map<String, String> result = authTask.authV2(authInfo, true);

        });
        t.start();
        return new HashMap<>();
    }

    /**
     * 获取支付宝 SDK 版本号。
     */
    public String showSdkVersion() {
        PayTask payTask = new PayTask(this);
        String version = payTask.getVersion();
        return version;
    }

    private static void showAlert(Context ctx, String info) {
        showAlert(ctx, info, null);
    }

    private static void showAlert(Context ctx, String info, DialogInterface.OnDismissListener onDismiss) {
        new AlertDialog.Builder(ctx)
                .setMessage(info)
                .setPositiveButton("OK", null)
                .setOnDismissListener(onDismiss)
                .show();
    }

    public String remoteMethodCall(String jsonStr) {

        //把jsonStr解析成为MethodCallDesc对象
        RMCDescriptor mcd = ReflectUtil.fromJson(jsonStr);

        Object ret = ReflectUtil.invokeJava(mcd.getClassName(), mcd.getMethodDesc(), mcd.getParaJson(), mcd.getInsJson(), this);

        JsonPrinter jsonPrinter = new JsonPrinter();
        return jsonPrinter.serial(ret);
    }

}
