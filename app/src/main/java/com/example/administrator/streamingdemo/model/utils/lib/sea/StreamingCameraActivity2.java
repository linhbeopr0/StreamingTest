package com.example.administrator.streamingdemo.model.utils.lib.sea;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.administrator.streamingdemo.R;
import com.example.administrator.streamingdemo.control.BaseApplication;
import com.example.administrator.streamingdemo.model.BasicInfo;
import com.example.administrator.streamingdemo.model.StreamSettingInfo;
import com.example.administrator.streamingdemo.model.utils.lib.sea.rtmp.RtmpPublisher;

import java.io.IOException;
import java.util.List;

public class StreamingCameraActivity2 extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "Yasea";

    private AudioRecord mic = null;
    private boolean aloop = false;
    private Thread aworker = null;

    private SurfaceView mCameraView = null;
    private Camera mCamera = null;

    private int mPreviewRotation = 90;
    private int mDisplayRotation = 90;
    private int mCamId = Camera.getNumberOfCameras() - 1; // default camera
    private byte[] mYuvFrameBuffer = new byte[SrsEncoder.VWIDTH * SrsEncoder.VHEIGHT * 3 / 2];

    private String mNotifyMsg;
    private SharedPreferences sp;

    private SrsEncoder mEncoder = new SrsEncoder(new RtmpPublisher.EventHandler() {
        @Override
        public void onRtmpConnecting(String msg) {

            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpConnected(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpVideoStreaming(String msg) {
        }

        @Override
        public void onRtmpAudioStreaming(String msg) {
        }

        @Override
        public void onRtmpStopped(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpDisconnected(String msg) {
            mNotifyMsg = msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onRtmpOutputFps(final double fps) {
            Log.i(TAG, String.format("Output Fps: %f", fps));
        }
    });

    private String getStreamLink() {
        String url = "";
        BasicInfo info = BasicInfo.getInstance();
        url = info.getStreamServer() + "/" + info.getStreamKey();
        return url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_streaming_camera_2);

        // restore data.
        sp = getSharedPreferences("SrsPublisher", MODE_PRIVATE);
        SrsEncoder.rtmpUrl = sp.getString("rtmpUrl", SrsEncoder.rtmpUrl);
        SrsEncoder.vbitrate = sp.getInt("vbitrate", SrsEncoder.vbitrate);
        Log.i(TAG, String.format("init rtmp url %s, vbitrate=%dkbps", SrsEncoder.rtmpUrl, SrsEncoder.vbitrate));

        SrsEncoder.rtmpUrl = getStreamLink();

        // initialize url.
        final EditText efu = (EditText) findViewById(R.id.url);
        efu.setText(SrsEncoder.rtmpUrl);

        // initialize video bitrate.
        final EditText evb = (EditText) findViewById(R.id.vbitrate);
        evb.setText(String.format("%dkbps", SrsEncoder.vbitrate / 1000));

        // for camera, @see https://developer.android.com/reference/android/hardware/Camera.html
        final Button btnPublish = (Button) findViewById(R.id.publish);
        final Button btnStop = (Button) findViewById(R.id.stop);
        final Button btnSwitch = (Button) findViewById(R.id.swCam);
        final Button btnRotate = (Button) findViewById(R.id.rotate);
        mCameraView = (SurfaceView) findViewById(R.id.preview);
        mCameraView.getHolder().addCallback(this);
        // mCameraView.getHolder().setFormat(SurfaceHolder.SURFACE_TYPE_HARDWARE);
        btnPublish.setEnabled(true);
        btnStop.setEnabled(false);

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int vb = Integer.parseInt(evb.getText().toString().replaceAll("kbps", ""));
                SrsEncoder.vbitrate = vb * 1000;
                SrsEncoder.rtmpUrl = efu.getText().toString();
                Log.i(TAG, String.format("RTMP URL changed to %s", SrsEncoder.rtmpUrl));
                Log.i(TAG, String.format("Video bitrate changed to %skbps", SrsEncoder.vbitrate / 1000));
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("vbitrate", SrsEncoder.vbitrate);
                editor.putString("rtmpUrl", SrsEncoder.rtmpUrl);
                editor.commit();
                btnPublish.setEnabled(false);
                btnStop.setEnabled(true);
                startPublish();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPublish();
                btnPublish.setEnabled(true);
                btnStop.setEnabled(false);
            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null && mEncoder != null) {
                    mCamId = (mCamId + 1) % Camera.getNumberOfCameras();
                    stopCamera();
                    mEncoder.swithCameraFace();
                    startCamera();
                }
            }
        });

        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    mPreviewRotation = (mPreviewRotation + 90) % 360;
                    mCamera.setDisplayOrientation(mPreviewRotation);
                }
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                mNotifyMsg = ex.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), mNotifyMsg, Toast.LENGTH_SHORT).show();
                        btnPublish.setEnabled(true);
                        btnStop.setEnabled(false);
                        stopPublish();
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startCamera() {
        if (mCamera != null) {
            Log.d(TAG, "start camera, already started. return");
            return;
        }
        if (mCamId > (Camera.getNumberOfCameras() - 1) || mCamId < 0) {
            Log.e(TAG, "####### start camera failed, inviald params, camera No.=" + mCamId);
            return;
        }

        mCamera = Camera.open(mCamId);

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCamId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayRotation = (mPreviewRotation + 180) % 360;
            mDisplayRotation = (360 - mDisplayRotation) % 360;
        } else {
            mDisplayRotation = mPreviewRotation;
        }

        Camera.Parameters params = mCamera.getParameters();

        Size size = mCamera.new Size(SrsEncoder.VWIDTH, SrsEncoder.VHEIGHT);
        if (!params.getSupportedPreviewSizes().contains(size)) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(),
                    new IllegalArgumentException(String.format("Unsupported preview size %dx%d", size.width, size.height)));
        }


        if (!params.getSupportedPictureSizes().contains(size)) {
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(),
                    new IllegalArgumentException(String.format("Unsupported picture size %dx%d", size.width, size.height)));
        }


        //params.set("orientation", "portrait");
        //params.set("orientation", "landscape");
        //params.setRotation(90);
        params.setPictureSize(SrsEncoder.VWIDTH, SrsEncoder.VHEIGHT);
        params.setPreviewSize(SrsEncoder.VWIDTH, SrsEncoder.VHEIGHT);
        int[] range = findClosestFpsRange(SrsEncoder.VFPS, params.getSupportedPreviewFpsRange());
        params.setPreviewFpsRange(range[0], range[1]);
        params.setPreviewFormat(SrsEncoder.VFORMAT);
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(mPreviewRotation);

        mCamera.addCallbackBuffer(mYuvFrameBuffer);
        mCamera.setPreviewCallbackWithBuffer(this);
        try {
            mCamera.setPreviewDisplay(mCameraView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    private void stopCamera() {
        if (mCamera != null) {
            // need to SET NULL CB before stop preview!!!
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void onGetYuvFrame(byte[] data) {
        mEncoder.onGetYuvFrame(data);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera c) {
        onGetYuvFrame(data);
        c.addCallbackBuffer(mYuvFrameBuffer);
    }

    private void onGetPcmFrame(byte[] pcmBuffer, int size) {
        mEncoder.onGetPcmFrame(pcmBuffer, size);
    }

    private void startAudio() {
        if (mic != null) {
            return;
        }

        int bufferSize = 2 * AudioRecord.getMinBufferSize(SrsEncoder.ASAMPLERATE, SrsEncoder.ACHANNEL, SrsEncoder.AFORMAT);
        mic = new AudioRecord(MediaRecorder.AudioSource.MIC, SrsEncoder.ASAMPLERATE, SrsEncoder.ACHANNEL, SrsEncoder.AFORMAT, bufferSize);
        mic.startRecording();

        byte pcmBuffer[] = new byte[4096];
        while (aloop && !Thread.interrupted()) {
            int size = mic.read(pcmBuffer, 0, pcmBuffer.length);
            if (size <= 0) {
                Log.e(TAG, "***** audio ignored, no data to read.");
                break;
            }
            onGetPcmFrame(pcmBuffer, size);
        }
    }

    private void stopAudio() {
        aloop = false;
        if (aworker != null) {
            Log.i(TAG, "stop audio worker thread");
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                aworker.interrupt();
            }
            aworker = null;
        }

        if (mic != null) {
            mic.setRecordPositionUpdateListener(null);
            mic.stop();
            mic.release();
            mic = null;
        }
    }

    private void startPublish() {
        StreamSettingInfo info = BasicInfo.getInstance().getStreamInfo();
        BaseApplication.getInstance().getService().startStream(BasicInfo.getInstance().getApitoken(), info.getTitle(), info.getDescription()
                , info.getIsArchiving(), info.getIsMakeArhieve(), info.getIsLiveChat(), info.getRestriction());
        Log.d(TAG, info.getTitle() + " - " + info.getDescription() + " - " + info.getIsArchiving() +
                " - " + info.getIsMakeArhieve() + " - " + info.getIsLiveChat() + " - " + info.getRestriction());

        int ret = mEncoder.start();
        if (ret < 0) {
            return;
        }

        startCamera();

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                startAudio();
            }
        });
        aloop = true;
        aworker.start();
    }

    private void stopPublish() {
        stopAudio();
        stopCamera();
        mEncoder.stop();
    }

    private int[] findClosestFpsRange(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }
        return closestRange;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        Log.d(TAG, "surfaceDestroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Button btn = (Button) findViewById(R.id.publish);
        btn.setEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPublish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPublish();
    }
}
