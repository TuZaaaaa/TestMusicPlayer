package com.example.simplemusic.activity;

import static com.example.simplemusic.activity.LocalMusicActivity.getFilesAllName;
import static com.example.simplemusic.config.GlobalConfig.AUDIO_FORMAT;
import static com.example.simplemusic.config.GlobalConfig.CHANNEL_CONFIG;
import static com.example.simplemusic.config.GlobalConfig.SAMPLE_RATE_INHZ;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.simplemusic.R;
import com.example.simplemusic.util.PcmToWavUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MusicRecognition extends AppCompatActivity {

    private ImageView imageView;
    private RippleAnimationView rippleAnimationView;

    private TextView recognizeTextView;
    private TextView recognizeTextView2;

    private OkHttpClient client;

    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段

    private boolean isRecording;
    /**
     * 需要申请的运行时权限
     */
    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * 被用户拒绝的权限列表
     */
    private List<String> mPermissionList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_recognition);

        imageView = findViewById(R.id.ImageView);
        rippleAnimationView = findViewById(R.id.layout_RippleAnimation);
        recognizeTextView = findViewById(R.id.recognize_text);
        recognizeTextView2 = findViewById(R.id.recognize_text2);

        checkPermissions();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rippleAnimationView.isRippleRunning()) {
                    rippleAnimationView.stopRippleAnimation();
                    recognizeTextView.setText("识别完成");
                    recognizeTextView2.setText("");
                    stopRecord();
                } else {
                    rippleAnimationView.startRippleAnimation();
                    recognizeTextView.setText("识别中...");
                    recognizeTextView2.setText("点击停止识别");
                    startRecord();
                }
            }
        });

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)//设置连接超时时间
                .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        super.onDestroy();
        client.dispatcher().cancelAll();
    }

    public void startRecord() {
        final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSizeInBytes);

        final byte data[] = new byte[bufferSizeInBytes];
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
        // 测试输出文件夹名称
        Log.i("locate", getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString());
        if (!file.mkdirs()) {
            Log.e("locate", "Directory not created");
        }
        if (file.exists()) {
            file.delete();
        }

        Log.i("locate", String.valueOf(audioRecord.getState()));
        audioRecord.startRecording();
        isRecording = true;

        // TODO: 2018/3/10 pcm数据无法直接播放，保存为WAV格式。

        Log.i("locate", "启动新线程");
        new Thread(new Runnable() {
            @Override
            public void run() {

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (null != os) {
                    while (isRecording) {
                        int read = audioRecord.read(data, 0, bufferSizeInBytes);
                        // 如果读取音频数据没有出现错误，就将数据写入到文件
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        Log.i("locate", "run: close file output stream !");
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    public void stopRecord() {
        isRecording = false;
        // 释放资源
        if (null != audioRecord) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            //recordingThread = null;
        }

        // 停止录制后 格式转换
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        File pcmFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
        File wavFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav");
        if (!wavFile.mkdirs()) {
            Log.e("locate", "wavFile Directory not created");
        }
        if (wavFile.exists()) {
            wavFile.delete();
        }
        pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());
        Log.i("locate", "convert success!");


        List<String> filesAllName = getFilesAllName(String.valueOf(getExternalFilesDir(Environment.DIRECTORY_MUSIC)));

        Log.i("locate", String.valueOf(filesAllName.size()));

        // 传输文件
        uploadFile(wavFile, "http://49.232.214.47:5000/upload");
//        uploadFile(wavFile, "http://192.168.43.45:5000/upload");
        Log.i("locate", wavFile.getAbsolutePath());
    }

    private void checkPermissions() {
        // Marshmallow开始才用申请运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("locate", permissions[i] + " 权限被用户禁止！");
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }

    // 使用OkHttp上传文件
    private void uploadFile(File file, String url) {

        // 构建请求体
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), file))
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Log.i("locate", "准备发送");

        // 异步发送请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 处理请求失败
                Log.e("locate", "请求失败");
                Log.e("locate", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 处理请求成功
                Log.i("locate", "请求成功");
                // 待处理
//                Toast.makeText(MusicRecognition.this, "未识别到音乐，请重新识别", Toast.LENGTH_SHORT).show();
                String recognition_result = response.body().string();
                Intent intent = new Intent(MusicRecognition.this, RecognitionMusicList.class);
                intent.putExtra("data", recognition_result);
                startActivity(intent);
            }
        });
    }

}
