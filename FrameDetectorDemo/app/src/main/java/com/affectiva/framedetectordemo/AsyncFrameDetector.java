package com.affectiva.framedetectordemo;

import android.content.Context;
import android.os.*;
import android.os.Process;
import android.util.Log;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.FrameDetector;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A class which instantiates and runs a FrameDetector on a background thread.
 * The background thread is defined in an inner class and is re-created for any pair of start() stop() calls the user makes.
 */
public class AsyncFrameDetector {

    public interface OnDetectorEventListener {
        void onImageResults(List<Face> faces, Frame image, float timeStamp);
        void onDetectorStarted();
    }

    FrameDetectorThread backgroundThread;
    Context context;
    boolean isRunning;
    MainThreadHandler mainThreadHandler;
    OnDetectorEventListener listener;

    /*
     Since FrameDetector is run on a background thread based off Android's HandlerThread class, it will receive frames to process
     in a queue. It is possible that this queue could grow in size, causing FrameDetector to incur a 'debt' of frames to process.
     To avoid this, we define a maximum number of frames that this waiting queue is allowed to have before we submit any more frames.
     */
    int framesWaiting = 0;
    final int MAX_FRAMES_WAITING = 1;

    public AsyncFrameDetector(Context context) {
        this.context = context;
        mainThreadHandler = new MainThreadHandler(this);
    }

    public void setOnDetectorEventListener(OnDetectorEventListener listener) {
        this.listener = listener;
    }

    /**
     * Starts running FrameDetector on a background thread.
     * Note that FrameDetector is not guaranteed to have started by the time this call returns, because it is
     * started asynchronously.
     */
    public void start() {
        if (isRunning)
            throw new RuntimeException("Called start() without calling stop() first.");

        isRunning = true;
        backgroundThread = new FrameDetectorThread("FrameDetectorThread",mainThreadHandler,context);
        backgroundThread.start();
        backgroundThread.waitUntilLooperAndHandlerCreated();
        backgroundThread.startDetector();
        framesWaiting = 0;
    }

    /**
     * Notifies the background thread to stop FrameDetector.
     */
    public void stop() {
        if(!isRunning)
            throw new RuntimeException("Called stop() without calling start() first");

        backgroundThread.stopDetector();
        backgroundThread = null;
        isRunning = false;
    }

    boolean isRunning() {
        return isRunning;
    }

    public void process(Frame frame, float timestamp) {
        if(isRunning) {
            if (framesWaiting <= MAX_FRAMES_WAITING) {
                framesWaiting += 1;
                backgroundThread.sendFrameToDetector(frame, timestamp);
            }
        }
    }

    public void reset() {
        if (isRunning) {
            backgroundThread.resetDetector();
            framesWaiting = 0;
        }
    }

    /*
        Notify our listener that FrameDetector start has completed.
     */
    private void sendDetectorStartedEvent() {
        if (listener!= null) {
            listener.onDetectorStarted();
        }
    }

    /*
        Send processed frame data to our listener.
     */
    private void sendOnImageResultsEvent(List<Face> faces, Frame frame, float timestamp) {
        framesWaiting -= 1;
        if (framesWaiting < 0) {
            framesWaiting = 0;
        }
        Log.e("ThreadTesting",String.format("Frames in queue: %d",framesWaiting));
        if (listener != null) {
            listener.onImageResults(faces,frame,timestamp);
        }
    }

    /**
     * Since our handler may be disposed long before our AsyncFrameDetector is disposed, we declare it as a static inner
     * class and hold only a WeakReference to our AsyncFrameDetector object, to avoid memory leaks.
     */
    static class MainThreadHandler extends Handler {
        WeakReference<AsyncFrameDetector> weakReference;

        public static final int FRAME_READY = 0;
        public static final int DETECTOR_STARTED = 1;

        MainThreadHandler(AsyncFrameDetector asyncFrameDetector) {
            super(Looper.getMainLooper());
            weakReference = new WeakReference<>(asyncFrameDetector);
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncFrameDetector asyncDetector = weakReference.get();
            switch (msg.what) {
                case DETECTOR_STARTED:
                        asyncDetector.sendDetectorStartedEvent();
                    break;
                case FRAME_READY:
                        FrameDetectorThread.OutputData frameData = (FrameDetectorThread.OutputData) msg.obj;
                        asyncDetector.sendOnImageResultsEvent(frameData.faces,frameData.frame,frameData.timestamp);
                    break;
            }
        }
    }

    /**
     * Our background thread, which operates by instantiating a FrameDetector in the background and communicating with it
     * via Handler Messages. See Android HandlerThread class documentation for more information on how this class works.
     */
    private class FrameDetectorThread extends HandlerThread {

        Handler threadHandler;
        Handler mainThreadhandler;

        FrameDetector detector;

        //Incoming message codes
        public static final int START_DETECTOR = 0;
        public static final int PROCESS_FRAME = 1;
        public static final int STOP_DETECTOR = 2;
        public static final int RESET_DETECTOR = 3;

        Context context;

        private final static String LOG_TAG = "Affectiva";

        public FrameDetectorThread(String string, Handler mainHandler, Context context) {
            super(string, Process.THREAD_PRIORITY_URGENT_DISPLAY);
            mainThreadhandler = mainHandler;
            this.context = context;
        }

        /**
         * The getLooper() method blocks the calling thread, so it will cause our calling thread to block until this thread is
         * ready to start receiving messages. This should ensure that the threadHandler object is not null in any of the public
         * methods below.
         */
        public void waitUntilLooperAndHandlerCreated() {
            threadHandler = new Handler(getLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case START_DETECTOR:
                            startDetectorAsync();
                            break;
                        case PROCESS_FRAME:
                            detectFrameAsync((InputData) msg.obj);
                            break;
                        case STOP_DETECTOR:
                            stopDetectorAsync();
                            mainThreadhandler = null;
                            context = null;
                            detector = null;
                            Log.e("ThreadTesting","Quitting Thread");
                            FrameDetectorThread.this.quit();
                            break;
                        case RESET_DETECTOR:
                            resetDetectorAsync();
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        /**
         * When resetting or stopping the detector, we don't want our command to have to wait for messages in front of it to
         * finish processing, so we purge any non-critical messages, namely PROCESS_FRAME and RESET_DETECTOR.
         */
        private void emptyQueue() {
            threadHandler.removeMessages(PROCESS_FRAME);
            threadHandler.removeMessages(RESET_DETECTOR);
        }

        public void startDetector() {
            threadHandler.obtainMessage(START_DETECTOR).sendToTarget();
        }

        public void stopDetector() {
            emptyQueue();
            threadHandler.obtainMessage(STOP_DETECTOR).sendToTarget();
        }

        public void sendFrameToDetector(Frame frame, float timestamp) {
            threadHandler.obtainMessage(PROCESS_FRAME,new InputData(frame,timestamp)).sendToTarget();
        }

        public void resetDetector() {
            emptyQueue();
            threadHandler.obtainMessage(RESET_DETECTOR).sendToTarget();
        }

        private void startDetectorAsync() {

            detector = new FrameDetector(context);
            //detector.setLicensePath("YourLicenseFile");
            detector.setDetectAllEmotions(true);
            detector.setDetectAllExpressions(true);
            detector.setImageListener(new Detector.ImageListener() {
                @Override
                public void onImageResults(List<Face> faceList, Frame frame, float timeStamp) {
                    OutputData data = new OutputData(faceList, frame, timeStamp);
                    mainThreadhandler.obtainMessage(MainThreadHandler.FRAME_READY, data).sendToTarget();
                }
            });

            try {
                detector.start();
            } catch (Exception e) {
                Log.e(LOG_TAG,e.getMessage());
            }

            mainThreadhandler.obtainMessage(MainThreadHandler.DETECTOR_STARTED).sendToTarget();
        }

        public void stopDetectorAsync() {
            detector.setImageListener(null);
            try {
                detector.stop();
            } catch (Exception e) {
                Log.e(LOG_TAG,e.getMessage());
            }
        }

        void detectFrameAsync(InputData data) {
            if (detector.isRunning()) {
                detector.process(data.frame, data.timestamp);
            }
        }

        void resetDetectorAsync() {
            if (detector.isRunning()) {
                detector.reset();
            }
            Log.i("Affectiva", "Detector reset");
        }

        class OutputData {
            public final List<Face> faces;
            public final Frame frame;
            public final float timestamp;

            public OutputData(List<Face> faces, Frame frame, float timestamp) {
                this.faces = faces;
                this.frame = frame;
                this.timestamp = timestamp;
            }

        }

        class InputData {
            public Frame frame;
            public float timestamp;

            public InputData(Frame frame, float timestamp) {
                this.frame = frame;
                this.timestamp = timestamp;
            }
        }

    }


}
