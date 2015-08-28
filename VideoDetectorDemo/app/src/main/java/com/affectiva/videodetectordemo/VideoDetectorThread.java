package com.affectiva.videodetectordemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.decoder.FrameDecoder;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.VideoFileDetector;

import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.List;

/**
 * Created by Alan on 7/24/2015.
 */
public class VideoDetectorThread extends Thread implements Detector.ImageListener {

    String filename;
    VideoFileDetector detector;
    Activity activity;
    public static String LOG_TAG = "Affectiva";
    DrawingView drawingView;

    FrameDecoder decoder;
    FrameDecoder.FrameDecoderFactory factory;

    MetricsPanel metricsPanel;

    public VideoDetectorThread(String file, Activity context, MetricsPanel metricsPanel, DrawingView drawingView ) {
        filename = file;
        activity = context;
        this.drawingView = drawingView;
        this.metricsPanel = metricsPanel;
    }

    @Override
    public void run() {

        detector = new VideoFileDetector(activity,filename);

        detector.setLicensePath("YourLicense");
        detector.setDetectAllEmotions(true);
        detector.setDetectAllExpressions(true);
        detector.setImageListener(this);
        try {
            detector.start();
        } catch ( Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }




    @Override
    public void onImageResults(List<Face> list, Frame image, final float timestamp) {

        final Frame frame = image;
        final List<Face> faces = list;
        Log.e("integration_testing",String.valueOf(timestamp));



        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //update metrics
                if (faces != null && faces.size() > 0) {
                    Face face = faces.get(0);
                    for (Metrics metric : Metrics.values()) {
                        metricsPanel.setMetricValue(metric,getScore(metric,face));

                        if (metric == Metrics.YAW) {
                            Log.e("integration_testing", String.format("%s score %.3f",metric.getUpperCaseName(),getScore(metric,face)));

                       }
                    }
                    PointF[] facePoints = face.getFacePoints();
                    int frameWidth = frame.getWidth();
                    int frameHeight = frame.getHeight();
                    Frame.ROTATE rotate = frame.getTargetRotation();

                    if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
                        int temp = frameWidth;
                        frameWidth = frameHeight;
                        frameHeight = temp;
                    }
                    Frame.revertPointRotation(facePoints,frameWidth,frameHeight,frame.getTargetRotation());
                    drawingView.drawFrame(frame,facePoints);
                } else {
                    Log.e("integration_testing", String.format("At time %.3f face not found!",timestamp));
                    for (Metrics metric : Metrics.values()) {
                        metricsPanel.setMetricNA(metric);
                    }
                    drawingView.drawFrame(frame, null);
                }

            }
        });
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

}
