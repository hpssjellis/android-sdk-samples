package com.example.alan.imagedetectordemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.affectiva.android.affdex.sdk.Frame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Alan on 7/29/2015.
 */
public class DrawingView extends SurfaceView implements SurfaceHolder.Callback{

    //Inner Thread class
    class DrawingThread extends Thread{
        private SurfaceHolder mSurfaceHolder;
        private boolean stopFlag = false; //boolean to indicate when thread has been told to stop
        private Frame nextFrameToDraw = null;
        private PointF[] nextPointsToDraw = null;
        private final long drawPeriod = 33; //draw at 30 fps
        Paint circlePaint;
        Frame initialFrame;

        public DrawingThread(SurfaceHolder surfaceHolder, Frame initialFrame) {
            mSurfaceHolder = surfaceHolder;
            circlePaint = new Paint();
            circlePaint.setColor(Color.rgb(255, 0, 0));
            nextFrameToDraw = initialFrame;

        }

        public void stopThread() {
            stopFlag = true;
        }

        public boolean isStopped() {
            return stopFlag;
        }

        //Updates thread with latest points returned by the onImageResults() event.
        public void updateFrameAndPoints(Frame frame, PointF[] points) {
            nextFrameToDraw = frame;
            nextPointsToDraw = points;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            while(!stopFlag) {
                long startTime = SystemClock.elapsedRealtime(); //get time at the start of thread loop

                /**
                 * We use SurfaceHolder.lockCanvas() to get the canvas that draws to the SurfaceView.
                 * After we are done drawing, we let go of the canvas using SurfaceHolder.unlockCanvasAndPost()
                 * **/
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas();

                    if (c!= null) {
                        synchronized (mSurfaceHolder) {
                            c.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR); //clear previous dots
                            if (nextFrameToDraw != null) {
                                draw(c);
                            }


                        }
                    }
                }
                finally {
                    if (c!= null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }

                //send thread to sleep so we don't draw faster than the requested 'drawPeriod'.
                long sleepTime = drawPeriod - (SystemClock.elapsedRealtime() - startTime);
                try {
                    if(sleepTime>0){
                        this.sleep(sleepTime);
                    }
                } catch (InterruptedException ex) {
                    Log.e(LOG_TAG, ex.getMessage());
                }
            }

        }

        void draw(Canvas c) {
            //get reference to frame on this thread
            Frame frame = nextFrameToDraw;
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


            if (nextPointsToDraw != null) {
                //Save our own reference to the list of points, in case the previous reference is overwritten by the main thread.
                PointF[] points = nextPointsToDraw;


                for (int i = 0; i < points.length; i++) {

                    //transform from the camera coordinates to our screen coordinates
                    //The camera preview is displayed as a mirror, so X pts have to be mirrored back.
                    float x = (points[i].x * scaling) + leftOffset;
                    float y = (points[i].y * scaling) + topOffset;

                    c.drawCircle(x, y, radius, circlePaint);
                }
            }

        }
    }

    //Class variables of DrawingView class
    private SurfaceHolder surfaceHolder;
    private DrawingThread drawingThread; //DrawingThread object
    private static String LOG_TAG = "AffdexMe";
    Frame initialFrame = null;

    //three constructors required of any custom view
    public DrawingView(Context context) {
        super(context);
        initView(context, null);
    }
    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }
    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    void initView(Context context, AttributeSet attrs){
        surfaceHolder = getHolder(); //The SurfaceHolder object will be used by the thread to request canvas to draw on SurfaceView
        surfaceHolder.addCallback(this); //become a Listener to the three events below that SurfaceView throws

        drawingThread = new DrawingThread(surfaceHolder,initialFrame);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (drawingThread.isStopped()) {
            drawingThread = new DrawingThread(surfaceHolder,initialFrame);
        }
        drawingThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //command thread to stop, and wait until it stops
        boolean retry = true;
        drawingThread.stopThread();
        while (retry) {
            try {
                drawingThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }
    }

    public void drawFrame(Frame frame, PointF[] points) {
        initialFrame = frame;
        drawingThread.updateFrameAndPoints(frame,points);
    }
}
