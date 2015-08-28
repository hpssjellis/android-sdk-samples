package com.example.alan.imagedetectordemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.PhotoDetector;
import com.affectiva.android.affdex.sdk.Frame;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * A sample app showing how to use ImageDetector.
 *
 * This app is not a production release and is known to have bugs. Specifically, the UI thread is blocked while the image is being processed,
 * and the app will crash if the user tries loading a very large image.
 *
 * For some images, facial tracking dots will not appear in the correct location.
 *
 * Also, the UI element that displays metrics is not aesthetic.
 *
 */
public class MainActivity extends Activity implements Detector.ImageListener {

    public static final String LOG_TAG = "Affectiva";
    public static final int PICK_IMAGE = 100;

    ImageView imageView;
    TextView[] metricScoreTextViews;

    LinearLayout metricsContainer;


    PhotoDetector detector;
    Bitmap bitmap = null;
    Frame.BitmapFrame frame;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        Log.e(LOG_TAG, "onCreate");

        loadInitialImage();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(LOG_TAG, "onResume");

    }

    void loadInitialImage() {
        if (bitmap == null) {
            bitmap = getBitmapFromAsset(this, "images/default.jpg");
        }
        setAndProcessBitmap(Frame.ROTATE.NO_ROTATION, false);
    }

    void startDetector() {
        if (!detector.isRunning()) {
            try {
                detector.start();
            } catch (Exception e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(LOG_TAG, "onPause");

    }

    void stopDetector() {
        if (detector.isRunning()) {
            try {
                detector.stop();
            } catch (Exception e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }
    }

    void initUI() {
        metricsContainer = (LinearLayout) findViewById(R.id.metrics_container);
        metricScoreTextViews = MetricsPanelCreator.createScoresTextViews();
        MetricsPanelCreator.populateMetricsContainer(metricsContainer,metricScoreTextViews,this);

        imageView = (ImageView) findViewById(R.id.image_view);
    }



    public Bitmap getBitmapFromAsset(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (Exception e) {
            Log.e(LOG_TAG,e.getMessage());
            return null;
        }

        return bitmap;
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        InputStream istr;
        Bitmap bitmap;
        try {
            istr = getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (Exception e) {
            Log.e(LOG_TAG,e.getMessage());
            return null;
        }

        return bitmap;
    }

    public void select_new_image(View view) {
        Intent gallery =
                new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    void setAndProcessBitmap(Frame.ROTATE rotation, boolean isExpectingFaceDetection) {
        if (bitmap == null) {
            return;
        }

        switch (rotation) {
            case BY_90_CCW:
                bitmap = Frame.rotateImage(bitmap,-90);
                break;
            case BY_90_CW:
                bitmap = Frame.rotateImage(bitmap,90);
                break;
            case BY_180:
                bitmap = Frame.rotateImage(bitmap,180);
                break;
            default:
                //keep bitmap as it is
        }

        frame = new Frame.BitmapFrame(bitmap, Frame.COLOR_FORMAT.UNKNOWN_TYPE);

        detector = new PhotoDetector(this);
        detector.setDetectAllEmotions(true); //emotions
        detector.setDetectAllExpressions(true); //expressions
        //detector.setLicensePath("YourLicensePath");
        detector.setImageListener(this);

        startDetector();
        detector.process(frame);
        stopDetector();

    }

    Bitmap drawCanvas(int width, int height, PointF[] points, Frame frame, Paint circlePaint) {
        if (width <= 0 || height <= 0) {
            return null;
        }

        Bitmap blackBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        blackBitmap.eraseColor(Color.BLACK);
        Canvas c = new Canvas(blackBitmap);

        Frame.ROTATE frameRot = frame.getTargetRotation();
        Bitmap bitmap;

        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();
        int canvasWidth = c.getWidth();
        int canvasHeight = c.getHeight();
        int scaledWidth;
        int scaledHeight;
        int topOffset = 0;
        int leftOffset= 0;
        float radius = (float)canvasWidth/100f;

        if (frame instanceof Frame.BitmapFrame) {
            bitmap = ((Frame.BitmapFrame)frame).getBitmap();
        } else { //frame is ByteArrayFrame
            byte[] pixels = ((Frame.ByteArrayFrame)frame).getByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(pixels);
            bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
        }

        if (frameRot == Frame.ROTATE.BY_90_CCW || frameRot == Frame.ROTATE.BY_90_CW) {
            int temp = frameWidth;
            frameWidth = frameHeight;
            frameHeight = temp;
        }

        float frameAspectRatio = (float)frameWidth/(float)frameHeight;
        float canvasAspectRatio = (float) canvasWidth/(float) canvasHeight;
        if (frameAspectRatio > canvasAspectRatio) { //width should be the same
            scaledWidth = canvasWidth;
            scaledHeight = (int)((float)canvasWidth / frameAspectRatio);
            topOffset = (canvasHeight - scaledHeight)/2;
        } else { //height should be the same
            scaledHeight = canvasHeight;
            scaledWidth = (int) ((float)canvasHeight*frameAspectRatio);
            leftOffset = (canvasWidth - scaledWidth)/2;
        }

        float scaling = (float)scaledWidth/(float)frameWidth;

        Matrix matrix = new Matrix();
        matrix.postRotate((float)frameRot.toDouble());
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,frameWidth,frameHeight,matrix,false);
        c.drawBitmap(rotatedBitmap,null,new Rect(leftOffset,topOffset,leftOffset+scaledWidth,topOffset+scaledHeight),null);


        if (points != null) {
            //Save our own reference to the list of points, in case the previous reference is overwritten by the main thread.

            for (int i = 0; i < points.length; i++) {

                //transform from the camera coordinates to our screen coordinates
                //The camera preview is displayed as a mirror, so X pts have to be mirrored back.
                float x = (points[i].x * scaling) + leftOffset;
                float y = (points[i].y * scaling) + topOffset;

                c.drawCircle(x, y, radius, circlePaint);
            }
        }

        return blackBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(LOG_TAG, "onActivityForResult");
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {

            Uri imageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);

            } catch (Exception e) {
                Toast.makeText(this,"Unable to open image.",Toast.LENGTH_LONG).show();
            }


            setAndProcessBitmap(Frame.ROTATE.NO_ROTATION, true);


        } else {
            Toast.makeText(this,"No image selected.",Toast.LENGTH_LONG).show();
        }
    }

    public void rotate_left(View view) {
        setAndProcessBitmap(Frame.ROTATE.BY_90_CCW, true);
    }

    public void rotate_right(View view) {
        setAndProcessBitmap(Frame.ROTATE.BY_90_CW,true);
    }

    @Override
    public void onImageResults(List<Face> faces, Frame image, float timestamp) {

        PointF[] points = null;



        if (faces != null && faces.size() > 0) {
            Face face = faces.get(0);
            for (int n = 0; n < MetricsManager.getTotalNumMetrics(); n++) {
                metricScoreTextViews[n].setText(String.format("%.3f", getScore(n, face)));
            }
            points = face.getFacePoints();
        } else {
            for (int n = 0; n < MetricsManager.getTotalNumMetrics(); n++) {
                metricScoreTextViews[n].setText("---");
            }
        }

        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.RED);
        Bitmap imageBitmap = drawCanvas(imageView.getWidth(),imageView.getHeight(),points,image,circlePaint);
        if (imageBitmap != null)
            imageView.setImageBitmap(imageBitmap);
    }

    float getScore(int metricCode, Face face) {

        float score;

        switch (metricCode) {
            case MetricsManager.ANGER:
                score = face.emotions.getAnger();
                break;
            case MetricsManager.CONTEMPT:
                score = face.emotions.getContempt();
                break;
            case MetricsManager.DISGUST:
                score = face.emotions.getDisgust();
                break;
            case MetricsManager.FEAR:
                score = face.emotions.getFear();
                break;
            case MetricsManager.JOY:
                score = face.emotions.getJoy();
                break;
            case MetricsManager.SADNESS:
                score = face.emotions.getSadness();
                break;
            case MetricsManager.SURPRISE:
                score = face.emotions.getSurprise();
                break;
            case MetricsManager.ATTENTION:
                score = face.expressions.getAttention();
                break;
            case MetricsManager.BROW_FURROW:
                score = face.expressions.getBrowFurrow();
                break;
            case MetricsManager.BROW_RAISE:
                score = face.expressions.getBrowRaise();
                break;
            case MetricsManager.CHIN_RAISER:
                score = face.expressions.getChinRaise();
                break;
            case MetricsManager.ENGAGEMENT:
                score = face.emotions.getEngagement();
                break;
            case MetricsManager.EYE_CLOSURE:
                score = face.expressions.getEyeClosure();
                break;
            case MetricsManager.INNER_BROW_RAISER:
                score = face.expressions.getInnerBrowRaise();
                break;
            case MetricsManager.LIP_DEPRESSOR:
                score = face.expressions.getLipCornerDepressor();
                break;
            case MetricsManager.LIP_PRESS:
                score = face.expressions.getLipPress();
                break;
            case MetricsManager.LIP_PUCKER:
                score = face.expressions.getLipPucker();
                break;
            case MetricsManager.LIP_SUCK:
                score = face.expressions.getLipSuck();
                break;
            case MetricsManager.MOUTH_OPEN:
                score = face.expressions.getMouthOpen();
                break;
            case MetricsManager.NOSE_WRINKLER:
                score = face.expressions.getNoseWrinkle();
                break;
            case MetricsManager.SMILE:
                score = face.expressions.getSmile();
                break;
            case MetricsManager.SMIRK:
                score = face.expressions.getSmirk();
                break;
            case MetricsManager.UPPER_LIP_RAISER:
                score = face.expressions.getUpperLipRaise();
                break;
            case MetricsManager.VALENCE:
                score = face.emotions.getValence();
                break;
            case MetricsManager.YAW:
                score = face.measurements.orientation.getYaw();
                break;
            case MetricsManager.ROLL:
                score = face.measurements.orientation.getRoll();
                break;
            case MetricsManager.PITCH:
                score = face.measurements.orientation.getPitch();
                break;
            case MetricsManager.INTER_OCULAR_DISTANCE:
                score = face.measurements.getInterocularDistance();
                break;
            default:
                score = Float.NaN;
                break;
        }
        return score;
    }
}
