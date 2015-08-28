package com.affectiva.framedetectordemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.List;

/**
 * This is a sample app using the FrameDetector object, which is not multi-threaded, and running it on a background thread in a custom object called
 * AsyncFrameDetector.
 *
 * This app also contains sample code for using the camera.
 */
public class MainActivity extends Activity implements CameraView.OnCameraViewEventListener, AsyncFrameDetector.OnDetectorEventListener {

    private static final String LOG_TAG = "Affectiva";

    MetricsPanel metricsPanel; //Fragment to display metric scores

    //UI Elements
    Button sdkButton;
    Button cameraButton;
    TextView processorFPS;
    TextView cameraFPS;
    ToggleButton frontBackToggle;

    //state booleans
    boolean isCameraStarted  = false;
    boolean isCameraFront = true;
    boolean isCameraRequestedByUser = false;
    boolean isSDKRunning = false;

    //variables used to determine the FPS rates of frames sent by the camera and processed by the SDK
    long numberCameraFramesReceived = 0;
    long lastCameraFPSResetTime = -1L;
    long numberSDKFramesReceived = 0;
    long lastSDKFPSResetTime = -1L;

    //floats to ensure the timestamps we send to FrameDetector are sequentially increasing
    float lastTimestamp = -1f;
    final float epsilon = .01f;

    CameraView cameraView; // controls the camera
    AsyncFrameDetector asyncDetector; // runs FrameDetector on a background thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up metrics view
        metricsPanel = new MetricsPanel();
        getFragmentManager().beginTransaction().add(R.id.fragment_container,metricsPanel).commit();

        //Init TextViews
        cameraFPS = (TextView) findViewById(R.id.camera_fps_text);
        processorFPS = (TextView) findViewById(R.id.processor_fps_text);

        //set up CameraView
        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.setOnCameraViewEventListener(this);

        //set up CameraButton
        cameraButton = (Button) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCameraRequestedByUser) { //Turn camera off
                    isCameraRequestedByUser = false;
                    cameraButton.setText("Start Camera");
                    stopCamera();
                } else { //Turn camera on
                    isCameraRequestedByUser = true;
                    cameraButton.setText("Stop Camera");
                    startCamera();
                }
                resetFPS();
            }
        });

        //Set up front toggle button
        frontBackToggle = (ToggleButton) findViewById(R.id.front_back_toggle_button);
        frontBackToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCameraFront = !isChecked;
                if (isCameraRequestedByUser) {
                    startCamera();
                }
            }
        });

        asyncDetector = new AsyncFrameDetector(this);
        asyncDetector.setOnDetectorEventListener(this);

        //Set up SDK Button
        sdkButton = (Button) findViewById(R.id.start_sdk_button);
        sdkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKRunning) {
                    isSDKRunning = false;
                    asyncDetector.stop();
                    sdkButton.setText("Start SDK");
                } else {
                    isSDKRunning = true;
                    asyncDetector.start();
                    sdkButton.setText("Stop SDK");
                }
                resetFPS();
            }
        });
        sdkButton.setText("Start SDK");
    }

    void resetFPS() {
        lastCameraFPSResetTime = lastSDKFPSResetTime = SystemClock.elapsedRealtime();
        numberCameraFramesReceived = numberSDKFramesReceived = 0;
    }

    void startCamera() {
        if (isCameraStarted) {
            cameraView.stopCamera();
        }
        cameraView.startCamera(isCameraFront ? CameraHelper.CameraType.CAMERA_FRONT : CameraHelper.CameraType.CAMERA_BACK);
        isCameraStarted = true;
        asyncDetector.reset();
    }

    void stopCamera() {
        if (!isCameraStarted)
            return;

        cameraView.stopCamera();
        isCameraStarted = false;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (isSDKRunning) {
            asyncDetector.start();
        }
        if (isCameraRequestedByUser) {
            startCamera();
        }

        resetFPS();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (asyncDetector.isRunning()) {
            asyncDetector.stop();
        }
        stopCamera();
    }

    float getScore(Metrics metric, Face face) {

        float score;

        switch (metric) {
            case ANGER:
                score = face.emotions.getAnger();
                break;
            case CONTEMPT:
                score = face.emotions.getContempt();
                break;
            case DISGUST:
                score = face.emotions.getDisgust();
                break;
            case FEAR:
                score = face.emotions.getFear();
                break;
            case JOY:
                score = face.emotions.getJoy();
                break;
            case SADNESS:
                score = face.emotions.getSadness();
                break;
            case SURPRISE:
                score = face.emotions.getSurprise();
                break;
            case ATTENTION:
                score = face.expressions.getAttention();
                break;
            case BROW_FURROW:
                score = face.expressions.getBrowFurrow();
                break;
            case BROW_RAISE:
                score = face.expressions.getBrowRaise();
                break;
            case CHIN_RAISER:
                score = face.expressions.getChinRaise();
                break;
            case ENGAGEMENT:
                score = face.emotions.getEngagement();
                break;
            case EYE_CLOSURE:
                score = face.expressions.getEyeClosure();
                break;
            case INNER_BROW_RAISER:
                score = face.expressions.getInnerBrowRaise();
                break;
            case LIP_DEPRESSOR:
                score = face.expressions.getLipCornerDepressor();
                break;
            case LIP_PRESS:
                score = face.expressions.getLipPress();
                break;
            case LIP_PUCKER:
                score = face.expressions.getLipPucker();
                break;
            case LIP_SUCK:
                score = face.expressions.getLipSuck();
                break;
            case MOUTH_OPEN:
                score = face.expressions.getMouthOpen();
                break;
            case NOSE_WRINKLER:
                score = face.expressions.getNoseWrinkle();
                break;
            case SMILE:
                score = face.expressions.getSmile();
                break;
            case SMIRK:
                score = face.expressions.getSmirk();
                break;
            case UPPER_LIP_RAISER:
                score = face.expressions.getUpperLipRaise();
                break;
            case VALENCE:
                score = face.emotions.getValence();
                break;
            case YAW:
                score = face.measurements.orientation.getYaw();
                break;
            case ROLL:
                score = face.measurements.orientation.getRoll();
                break;
            case PITCH:
                score = face.measurements.orientation.getPitch();
                break;
            case INTER_OCULAR_DISTANCE:
                score = face.measurements.getInterocularDistance();
                break;
            default:
                score = Float.NaN;
                break;
        }
        return score;
    }

    @Override
    public void onCameraFrameAvailable(byte[] frame, int width, int height, Frame.ROTATE rotation) {
        numberCameraFramesReceived += 1;
        cameraFPS.setText(String.format("CAM: %.3f", 1000f * (float) numberCameraFramesReceived / (SystemClock.elapsedRealtime() - lastCameraFPSResetTime)));

        float timeStamp = (float)SystemClock.elapsedRealtime()/1000f;
        if (timeStamp > (lastTimestamp + epsilon)) {
            lastTimestamp = timeStamp;
            asyncDetector.process(createFrameFromData(frame,width,height,rotation),timeStamp);
        }
    }

    @Override
    public void onCameraStarted(boolean success, Throwable error) {
        //TODO: change status here
    }

    @Override
    public void onSurfaceViewSizeChanged() {
        asyncDetector.reset();
    }

    float lastReceivedTimestamp = -1f;

    @Override
    public void onImageResults(List<Face> faces, Frame image, float timeStamp) {
        //statusTextView.setText(String.format("Most recent time stamp: %.4f",timeStamp));
        if (timeStamp < lastReceivedTimestamp)
            throw new RuntimeException("Got a timestamp out of order!");
        lastReceivedTimestamp = timeStamp;
        Log.e("MainActivity", String.valueOf(timeStamp));

        if (faces == null)
            return; //No Face Detected
        if (faces.size() ==0) {
            for (Metrics metric : Metrics.values()) {
                metricsPanel.setMetricNA(metric);
            }
        } else {
            Face face = faces.get(0);
            for (Metrics metric : Metrics.values()) {
                metricsPanel.setMetricValue(metric,getScore(metric,face));
            }
        }

        numberSDKFramesReceived += 1;
        processorFPS.setText(String.format("SDK: %.3f", 1000f * (float) numberSDKFramesReceived / (SystemClock.elapsedRealtime() - lastSDKFPSResetTime)));

    }

    @Override
    public void onDetectorStarted() {

    }

    static Frame createFrameFromData(byte[] frameData, int width, int height, Frame.ROTATE rotation) {
        Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(frameData, width, height, Frame.COLOR_FORMAT.YUV_NV21);
        frame.setTargetRotation(rotation);
        return frame;
    }
}
