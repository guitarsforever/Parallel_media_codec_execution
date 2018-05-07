package com.example.encoderdecodertest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.GridView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class ParallelOperations extends Activity {
    private static final String TAG = "ParallelOperations";
    private static final boolean VERBOSE = true;           // lots of logging
    private static final boolean DEBUG_SAVE_FILE = true;   // save copy of encoded movie
    private static final String DEBUG_FILE_NAME_BASE = "/sdcard/test.";
    private static final String DEBUG_OUT_FILE_NAME_BASE = "/sdcard/decode.";
    private static final String DEBUG_IN_FILE_NAME_BASE = "/sdcard/input.";
    private static final String FILE_NAME_BASE = "/sdcard/testfiles/";
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    List<image> bitmapArrayRead ;
    List<image> bitmapArrayWrite ;
    List <Boolean> flag;
    long startTime = 0 ,endTime = 0;
    List<Long> perFrameTime = new ArrayList<Long>();

    int smallimage_Height, smallimage_Width;
    int fileIndex = 1;
    private boolean decoderRequired = true;

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames

    // movie length, in frames
    private static final int NUM_FRAMES = 30;               // two seconds of video

    private static final int TEST_Y = 120;                  // YUV values for colored rect
    private static final int TEST_U = 160;
    private static final int TEST_V = 200;
    private static final int TEST_R0 = 0;                   // RGB equivalent of {0,0,0}
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 236;                 // RGB equivalent of {120,160,200}
    private static final int TEST_G1 = 50;
    private static final int TEST_B1 = 186;

    // size of a frame, in pixels
    private int mWidth = -1;
    private int mHeight = -1;
    // bit rate, in bits per second
    private int mBitRate = -1;

    // largest color component delta seen (i.e. actual vs. expected)
    private int mLargestColorDelta;

    private Surface mSurface = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parallel_operations);

        List<image> li = new ArrayList<image>();
        List<image> write = new ArrayList<image>();
        List <Boolean> writer = new ArrayList<Boolean>();
        bitmapArrayRead = Collections.synchronizedList(li);
        bitmapArrayWrite= Collections.synchronizedList(write);
        flag = Collections.synchronizedList(writer);
        flag.add(false);
        flag.add(false);
        flag.add(false);
        flag.add(false);
        ProcessVideo ();

        try {
            testEncodeDecodeVideoFromBufferToSurfaceQVGA();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        while (true) {
            if (flag.get(0) == true && flag.get(1) == true && flag.get(2) == true && flag.get(3) == true) {
                endTime = System.currentTimeMillis();
                if (startTime !=0) {
                    long diff = endTime - startTime;
                    perFrameTime.add(diff);}
                bitmapArrayRead.clear();
                //bitmapArrayWrite.clear();
                ProcessVideo ();
                flag.set(0,false);
                flag.set(1,false);
                flag.set(2,false);
                flag.set(3,false);
                Log.d ("Rao", "flag 1 and flag 2 false");
                fileIndex++;
                startTime = System.currentTimeMillis();

            }
            if (fileIndex > NUM_FRAMES) {
                flag.set(0,false);
                flag.set(1,false);
                flag.set(2,false);
                flag.set(3,false);
                Log.d ("Rao", "fileIndex > NUM_FRAMES");
                break;}
        }
        try {
            FileOutputStream Out = new FileOutputStream("/sdcard/"+"perFrame_Log.txt");
            if (Out != null) {
                for (long x : perFrameTime) {
                    String temp = Long.toString(x) + System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                Out.close();
            }else {
                    Log.d("shreyas1", "else" );
                }
            } catch (IOException ioe) {
                Log.w(TAG, "failed writing debug data to file");
                throw new RuntimeException(ioe);
            }
    }

    public void testEncodeDecodeVideoFromBufferToSurfaceQVGA() throws Throwable {
        setParameters(smallimage_Width, smallimage_Height, 2000000);
        BufferToSurfaceWrapper.runTest(this);
        BufferToSurfaceWrapper1.runTest(this);
        BufferToSurfaceWrapper2.runTest(this);
        BufferToSurfaceWrapper3.runTest(this);
    }

    private static class BufferToSurfaceWrapper implements Runnable {
        private Throwable mThrowable;
        private ParallelOperations mTest;

        private BufferToSurfaceWrapper(ParallelOperations test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.encodeDecodeVideoFromBuffer(false);
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /**
         * Entry point.
         */
        public static void runTest(ParallelOperations obj) throws Throwable {
            BufferToSurfaceWrapper wrapper = new BufferToSurfaceWrapper(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            //th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    private static class BufferToSurfaceWrapper1 implements Runnable {
        private Throwable mThrowable;
        private ParallelOperations mTest;

        private BufferToSurfaceWrapper1(ParallelOperations test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.encodeDecodeVideoFromBuffer1(false);
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /**
         * Entry point.
         */
        public static void runTest(ParallelOperations obj) throws Throwable {
            BufferToSurfaceWrapper1 wrapper = new BufferToSurfaceWrapper1(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            //th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    private static class BufferToSurfaceWrapper2 implements Runnable {
        private Throwable mThrowable;
        private ParallelOperations mTest;

        private BufferToSurfaceWrapper2(ParallelOperations test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.encodeDecodeVideoFromBuffer2(false);
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /**
         * Entry point.
         */
        public static void runTest(ParallelOperations obj) throws Throwable {
            BufferToSurfaceWrapper2 wrapper = new BufferToSurfaceWrapper2(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            //th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    private static class BufferToSurfaceWrapper3 implements Runnable {
        private Throwable mThrowable;
        private ParallelOperations mTest;

        private BufferToSurfaceWrapper3(ParallelOperations test) {
            mTest = test;
        }

        @Override
        public void run() {
            try {
                mTest.encodeDecodeVideoFromBuffer3(false);
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /**
         * Entry point.
         */
        public static void runTest(ParallelOperations obj) throws Throwable {
            BufferToSurfaceWrapper3 wrapper = new BufferToSurfaceWrapper3(obj);
            Thread th = new Thread(wrapper, "codec test");
            th.start();
            //th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    private void encodeDecodeVideoFromBuffer(boolean toSurface) throws Exception {
        MediaCodec encoder = null;
        MediaCodec decoder = null;

        mLargestColorDelta = -1;

        try {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            if (VERBOSE) Log.d(TAG, "found codec: " + codecInfo.getName());

            int colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
            if (VERBOSE) Log.d(TAG, "found colorFormat: " + colorFormat);

            // We avoid the device-specific limitations on width and height by using values that
            // are multiples of 16, which all tested devices seem to be able to handle.
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            if (VERBOSE) Log.d(TAG, "format: " + format);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            // Create a MediaCodec for the decoder, just based on the MIME type.  The various
            // format details will be passed through the csd-0 meta-data later on.
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);

            doEncodeDecodeVideoFromBuffer(encoder, colorFormat, decoder, toSurface);
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing codecs");
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            Log.i(TAG, "Largest color delta: " + mLargestColorDelta);
        }
    }

    private void encodeDecodeVideoFromBuffer1(boolean toSurface) throws Exception {
        MediaCodec encoder = null;
        MediaCodec decoder = null;

        mLargestColorDelta = -1;

        try {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            if (VERBOSE) Log.d(TAG, "found codec: " + codecInfo.getName());

            int colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
            if (VERBOSE) Log.d(TAG, "found colorFormat: " + colorFormat);

            // We avoid the device-specific limitations on width and height by using values that
            // are multiples of 16, which all tested devices seem to be able to handle.
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            if (VERBOSE) Log.d(TAG, "format: " + format);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            // Create a MediaCodec for the decoder, just based on the MIME type.  The various
            // format details will be passed through the csd-0 meta-data later on.
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);

            doEncodeDecodeVideoFromBuffer1(encoder, colorFormat, decoder, toSurface);
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing codecs2");
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            Log.i(TAG, "Largest color delta: " + mLargestColorDelta);
        }
    }

    private void encodeDecodeVideoFromBuffer2(boolean toSurface) throws Exception {
        MediaCodec encoder = null;
        MediaCodec decoder = null;

        mLargestColorDelta = -1;

        try {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            if (VERBOSE) Log.d(TAG, "found codec: " + codecInfo.getName());

            int colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
            if (VERBOSE) Log.d(TAG, "found colorFormat: " + colorFormat);

            // We avoid the device-specific limitations on width and height by using values that
            // are multiples of 16, which all tested devices seem to be able to handle.
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            if (VERBOSE) Log.d(TAG, "format: " + format);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            // Create a MediaCodec for the decoder, just based on the MIME type.  The various
            // format details will be passed through the csd-0 meta-data later on.
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);

            doEncodeDecodeVideoFromBuffer2(encoder, colorFormat, decoder, toSurface);
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing codecs3");
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            Log.i(TAG, "Largest color delta: " + mLargestColorDelta);
        }
    }

    private void encodeDecodeVideoFromBuffer3(boolean toSurface) throws Exception {
        MediaCodec encoder = null;
        MediaCodec decoder = null;

        mLargestColorDelta = -1;

        try {
            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            if (VERBOSE) Log.d(TAG, "found codec: " + codecInfo.getName());

            int colorFormat = selectColorFormat(codecInfo, MIME_TYPE);
            if (VERBOSE) Log.d(TAG, "found colorFormat: " + colorFormat);

            // We avoid the device-specific limitations on width and height by using values that
            // are multiples of 16, which all tested devices seem to be able to handle.
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            if (VERBOSE) Log.d(TAG, "format: " + format);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            // Create a MediaCodec for the decoder, just based on the MIME type.  The various
            // format details will be passed through the csd-0 meta-data later on.
            decoder = MediaCodec.createDecoderByType(MIME_TYPE);

            doEncodeDecodeVideoFromBuffer3(encoder, colorFormat, decoder, toSurface);
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing codecs4");
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            Log.i(TAG, "Largest color delta: " + mLargestColorDelta);
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        //fail("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

    private void setParameters(int width, int height, int bitRate) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        Log.d(TAG, "SetParameter : width = " + width +" Height = " + height);
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
    }

    public void ProcessVideo () {

        String filepath = FILE_NAME_BASE + "thumb000" + fileIndex + ".jpg";
        if (fileIndex>99) {
            filepath = FILE_NAME_BASE+"thumb0"+fileIndex+".jpg";
        } else if (fileIndex>9) {
            filepath = FILE_NAME_BASE+"thumb00"+fileIndex+".jpg";
        }
        Log.v(TAG, filepath);
        File imagefile = new File(filepath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(imagefile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Bitmap bm = BitmapFactory.decodeStream(fis);
        buildImageArray(bm, 0, fileIndex, 4);
    }
    void buildImageArray(Bitmap b, int status, int frameNumber, int splitNumber) {
        ArrayList<Bitmap> tempArray = splitImage(b,splitNumber);
//        GridView image_grid = (GridView) findViewById(R.id.vincentgridview);
//
//        image_grid.setAdapter(new SmallImageAdapter(this, tempArray));
//
//        image_grid.setNumColumns((int) Math.sqrt(tempArray.size()));

        for (Bitmap B : tempArray) {
            image buildImageObj = new image (0,B,frameNumber);
            bitmapArrayRead.add(buildImageObj);
        }

    }

    private ArrayList<Bitmap> splitImage(Bitmap b, int smallimage_Numbers) {
        //For the number of rows and columns of the grid to be displayed
        int rows, cols;
        //For height and width of the small image smallimage_s
        //To store all the small image smallimage_s in bitmap format in this list
        ArrayList<Bitmap> smallimages = new ArrayList<Bitmap>(smallimage_Numbers);
        //Getting the scaled bitmap of the source image
        Log.d ("Rao", "Number of Split = " + smallimage_Numbers);
        Bitmap bitmap = b;
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        rows = cols = (int) Math.sqrt(smallimage_Numbers);
        smallimage_Height = bitmap.getHeight() / rows;
        smallimage_Width = bitmap.getWidth() / cols;
        //xCo and yCo are the pixel positions of the image smallimage_s
        int yCo = 0;
        for (int x = 0; x < rows; x++) {
            int xCo = 0;
            for (int y = 0; y < cols; y++) {
                smallimages.add(Bitmap.createBitmap(scaledBitmap, xCo, yCo, smallimage_Width, smallimage_Height));
                xCo += smallimage_Width;
            }
            yCo += smallimage_Height;
        }
        return smallimages;
    }

    private void doEncodeDecodeVideoFromBuffer(MediaCodec encoder, int encoderColorFormat,
                                               MediaCodec decoder, boolean toSurface) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        ByteBuffer[] decoderInputBuffers = null;
        ByteBuffer[] decoderOutputBuffers = null;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long tsLong = 0, tslongMid = 0, tsLongEnd = 0;
        List<Long> encodedTime = new ArrayList<Long>();
        List<Long> decodedTime = new ArrayList<Long>();
        List<Integer> packetSize = new ArrayList<>();
        MediaFormat decoderOutputFormat = null;
        int generateIndex = 0;
        int checkIndex = 0;
        int badFrames = 0;
        boolean decoderConfigured = false;
        Surface outputSurface = null;

        // The size of a frame of video data, in the formats we handle, is stride*sliceHeight
        // for Y, and (stride/2)*(sliceHeight/2) for each of the Cb and Cr channels.  Application
        // of algebra and assuming that stride==width and sliceHeight==height yields:
        byte[] frameData = new byte[mWidth * mHeight * 3 / 2];

        // Just out of curiosity.
        long rawSize = 0;
        long encodedSize = 0;

        // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
        // stream, not a .mp4 file, so not all players will know what to do with it.
        FileOutputStream outputStream = null;
        FileOutputStream outputStream_out = null;
        FileOutputStream inputStream = null;
        if (DEBUG_SAVE_FILE) {
            String fileName_in = DEBUG_IN_FILE_NAME_BASE + mWidth + "x" + mHeight + ".yuv";
            String fileName = DEBUG_FILE_NAME_BASE + mWidth + "x" + mHeight + ".mp4";
            String fileName_out = DEBUG_OUT_FILE_NAME_BASE + mWidth + "x" + mHeight + ".yuv";
            try {
                inputStream = new FileOutputStream(fileName_in);
                Log.d(TAG, "encoded input will be saved as " + fileName_in);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }

            try {
                outputStream = new FileOutputStream(fileName);
                Log.d(TAG, "encoded output will be saved as " + fileName);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }
            try {
                outputStream_out = new FileOutputStream(fileName_out);
                Log.d(TAG, "decoded output will be saved as " + fileName_out);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug decode output file " + fileName_out);
                throw new RuntimeException(ioe);
            }
        }

        if (toSurface) {
            outputSurface = mSurface; //new OutputSurface(mWidth, mHeight);
        }

        // Loop until the output side is done.
        boolean inputDone = false;
        boolean encoderDone = false;
        boolean outputDone = false;
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop");

            // If we're not done submitting frames, generate a new one and submit it.  By
            // doing this on every loop we're working to ensure that the encoder always has
            // work to do.
            //
            // We don't really want a timeout here, but sometimes there's a delay opening
            // the encoder device, so a short timeout can keep us from spinning hard.
            if (!inputDone && flag.get(0) == false) {
                int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (VERBOSE) Log.d(TAG, "inputBufIndex=" + inputBufIndex);
                if (inputBufIndex >= 0) {
                    long ptsUsec = computePresentationTime(generateIndex);
                    if (generateIndex == NUM_FRAMES) {
                        // Send an empty frame with the end-of-stream flag set.  If we set EOS
                        // on a frame with data, that frame data will be ignored, and the
                        // output will be short one frame.
                        encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        outputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS (with zero-length frame)");
                        generateIndex++;
                    } else  {
                       // generateFrame(generateIndex, encoderColorFormat, frameData);
                        Bitmap bm = bitmapArrayRead.get(0).getNewImage();
                        flag.set(0,true);
                        Log.d ("Rao", "flag 1 true");
                        byte[] frame = getNV21(mWidth,mHeight,bm);
                        ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];

//                        if (inputStream != null) {
//                            byte[] data = new byte[info.size];
//                            inputBuf.get(data);
//                            inputBuf.position(info.offset);
//                            try {
//                                inputStream.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file");
//                        }
                        // the buffer should be sized to hold one full frame
                        //assertTrue(inputBuf.capacity() >= frameData.length);
                        inputBuf.clear();
                        inputBuf.put(frame);
                        tsLong = System.currentTimeMillis();
                        encoder.queueInputBuffer(inputBufIndex, 0, frame.length, ptsUsec, 0);
                        if (VERBOSE) Log.d(TAG, "submitted frame " + generateIndex + " to enc");
                        generateIndex++;
                    }

                } else {
                    // either all in use, or we timed out during initial setup
                    if (VERBOSE) Log.d(TAG, "input buffer not available");
                }
            }

            // Check for output from the encoder.  If there's no output yet, we either need to
            // provide more input, or we need to wait for the encoder to work its magic.  We
            // can't actually tell which is the case, so if we can't get an output buffer right
            // away we loop around and see if it wants more input.
            //
            // Once we get EOS from the encoder, we don't need to do this anymore.
            if (!encoderDone) {
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // not expected for an encoder
                    MediaFormat newFormat = encoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    //fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        //fail("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);
                    tslongMid = System.currentTimeMillis();
                    long encodingTime = tslongMid - tsLong;
                    encodedTime.add(encodingTime);
                    encodedSize += info.size;
//                    if (outputStream != null) {
//                        byte[] data = new byte[info.size];
//                        encodedData.get(data);
//                        encodedData.position(info.offset);
//                        try {
//                            outputStream.write(data);
//                        } catch (IOException ioe) {
//                            Log.w(TAG, "failed writing debug data to file");
//                            throw new RuntimeException(ioe);
//                        }
//                        Log.w(TAG, "successful writing debug data to file");
//                    }
                    if (decoderRequired) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // Codec config info.  Only expected on first packet.  One way to
                            // handle this is to manually stuff the data into the MediaFormat
                            // and pass that to configure().  We do that here to exercise the API.
                            //assertFalse(decoderConfigured);
                            MediaFormat format =
                                    MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                            format.setByteBuffer("csd-0", encodedData);
                            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                            //format.setInteger("color-format", 19);
                            decoder.configure(format, toSurface ? outputSurface : null,
                                    null, 0);
                            decoder.start();
                            decoderInputBuffers = decoder.getInputBuffers();
                            decoderOutputBuffers = decoder.getOutputBuffers();
                            decoderConfigured = true;
                            if (VERBOSE) Log.d(TAG, "decoder configured (" + info.size + " bytes)");
                        } else {
                            // Get a decoder input buffer, blocking until it's available.
                            //assertTrue(decoderConfigured);
                            int inputBufIndex = decoder.dequeueInputBuffer(-1);
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            inputBuf.clear();
                            inputBuf.put(encodedData);
                            tslongMid = System.currentTimeMillis();
                            decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                                    info.presentationTimeUs, info.flags);

                            encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                            packetSize.add (info.size);
                            if (VERBOSE) Log.d(TAG, "passed " + info.size + " bytes to decoder"
                                    + (encoderDone ? " (EOS)" : ""));
                        }
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
            }

            // Check for output from the decoder.  We want to do this on every loop to avoid
            // the possibility of stalling the pipeline.  We use a short timeout to avoid
            // burning CPU if the decoder is hard at work but the next frame isn't quite ready.
            //
            // If we're decoding to a Surface, we'll get notified here as usual but the
            // ByteBuffer references will be null.  The data is sent to Surface instead.
            if (decoderConfigured && decoderRequired) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // The storage associated with the direct ByteBuffer may already be unmapped,
                    // so attempting to access data through the old output buffer array could
                    // lead to a native crash.
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    decoderOutputFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else if (decoderStatus < 0) {
                    //fail("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
                } else {  // decoderStatus >= 0
                    if (!toSurface) {
                        ByteBuffer outputFrame = decoderOutputBuffers[decoderStatus];

                        outputFrame.position(info.offset);
                        outputFrame.limit(info.offset + info.size);

                        rawSize += info.size;

//                        if (outputStream_out != null) {
//                            byte[] data = new byte[info.size];
//                            outputFrame.get(data);
//                            outputFrame.position(info.offset);
//                            try {
//                                outputStream_out.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file");
//                        }

                        if (info.size == 0) {
                            if (VERBOSE) Log.d(TAG, "got empty frame");
                        } else {
                            if (VERBOSE) Log.d(TAG, "decoded, checking frame " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            if (!checkFrame(checkIndex++, decoderOutputFormat, outputFrame)) {
                                badFrames++;
                            }
                        }

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            outputDone = true;
                        }
                        tsLongEnd =System.currentTimeMillis();
                        long diff = tsLongEnd- tslongMid;
                        decodedTime.add(diff);
                        decoder.releaseOutputBuffer(decoderStatus, false /*render*/);
                    } else {
                        if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                                " (size=" + info.size + ")");
                        rawSize += info.size;
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            outputDone = true;
                        }

                        boolean doRender = (info.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                        // that the texture will be available before the call returns, so we
                        // need to wait for the onFrameAvailable callback to fire.
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        if (doRender) {
                            if (VERBOSE) Log.d(TAG, "awaiting frame " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            //outputSurface.awaitNewImage();
                            //outputSurface.drawImage();
                            if (!checkSurfaceFrame(checkIndex++)) {
                                badFrames++;
                            }
                        }
                    }
                }
            }
        }

        if (VERBOSE) Log.d(TAG, "decoded " + checkIndex + " frames at "
                + mWidth + "x" + mHeight + ": raw=" + rawSize + ", enc=" + encodedSize);

        try {
            FileOutputStream Out = new FileOutputStream("/sdcard/"+"Logs_T1.txt");
            if (Out != null) {
                for (long x : encodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                String temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : decodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : packetSize) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }

                Out.close();
            } else {
                Log.d("shreyas1", "else" );
            }
        } catch (IOException ioe) {
            Log.w(TAG, "failed writing debug data to file");
            throw new RuntimeException(ioe);
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug file");
                throw new RuntimeException(ioe);
            }
        }
        if (outputStream_out != null) {
            try {
                outputStream_out.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug decode output file");
                throw new RuntimeException(ioe);
            }
        }

        if (outputSurface != null) {
            outputSurface.release();
        }

        if (checkIndex != NUM_FRAMES) {
            //fail("expected " + NUM_FRAMES + " frames, only decoded " + checkIndex);
        }
        if (badFrames != 0) {
            //fail("Found " + badFrames + " bad frames");
        }


    }

    private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }
    private boolean checkFrame(int frameIndex, MediaFormat format, ByteBuffer frameData) {
        // Check for color formats we don't understand.  There is no requirement for video
        // decoders to use a "mundane" format, so we just give a pass on proprietary formats.
        // e.g. Nexus 4 0x7FA30C03 OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka
        int colorFormat = format.getInteger(MediaFormat.KEY_COLOR_FORMAT);
        if (!isRecognizedFormat(colorFormat)) {
            Log.d(TAG, "unable to check frame contents for colorFormat=" +
                    Integer.toHexString(colorFormat));
            return true;
        }

        boolean frameFailed = false;
        boolean semiPlanar = isSemiPlanarYUV(colorFormat);
        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        int halfWidth = width / 2;
        int cropLeft = format.getInteger("crop-left");
        int cropRight = format.getInteger("crop-right");
        int cropTop = format.getInteger("crop-top");
        int cropBottom = format.getInteger("crop-bottom");
        int cropWidth = cropRight - cropLeft + 1;
        int cropHeight = cropBottom - cropTop + 1;

        //assertEquals(mWidth, cropWidth);
        //assertEquals(mHeight, cropHeight);

        for (int i = 0; i < 8; i++) {
            int x, y;
            if (i < 4) {
                x = i * (mWidth / 4) + (mWidth / 8);
                y = mHeight / 4;
            } else {
                x = (7 - i) * (mWidth / 4) + (mWidth / 8);
                y = (mHeight * 3) / 4;
            }

            y += cropTop;
            x += cropLeft;

            int testY, testU, testV;
            if (semiPlanar) {
                // Galaxy Nexus uses OMX_TI_COLOR_FormatYUV420PackedSemiPlanar
                testY = frameData.get(y * width + x) & 0xff;
                testU = frameData.get(width*height + 2*(y/2) * halfWidth + 2*(x/2)) & 0xff;
                testV = frameData.get(width*height + 2*(y/2) * halfWidth + 2*(x/2) + 1) & 0xff;
            } else {
                // Nexus 10, Nexus 7 use COLOR_FormatYUV420Planar
                testY = frameData.get(y * width + x) & 0xff;
                testU = frameData.get(width*height + (y/2) * halfWidth + (x/2)) & 0xff;
                testV = frameData.get(width*height + halfWidth * (height / 2) +
                        (y/2) * halfWidth + (x/2)) & 0xff;
            }

            int expY, expU, expV;
            if (i == frameIndex % 8) {
                // colored rect
                expY = TEST_Y;
                expU = TEST_U;
                expV = TEST_V;
            } else {
                // should be our zeroed-out buffer
                expY = expU = expV = 0;
            }
            if (!isColorClose(testY, expY) ||
                    !isColorClose(testU, expU) ||
                    !isColorClose(testV, expV)) {
                Log.w(TAG, "Bad frame " + frameIndex + " (rect=" + i + ": yuv=" + testY +
                        "," + testU + "," + testV + " vs. expected " + expY + "," + expU +
                        "," + expV + ")");
                frameFailed = true;
            }
        }

        return !frameFailed;
    }

    /**
     * Checks the frame for correctness.  Similar to {@link #checkFrame}, but uses GL to
     * read pixels from the current surface.
     *
     * @return true if the frame looks good
     */
    private boolean checkSurfaceFrame(int frameIndex) {
        ByteBuffer pixelBuf = ByteBuffer.allocateDirect(4); // TODO - reuse this
        boolean frameFailed = false;

        for (int i = 0; i < 8; i++) {
            // Note the coordinates are inverted on the Y-axis in GL.
            int x, y;
            if (i < 4) {
                x = i * (mWidth / 4) + (mWidth / 8);
                y = (mHeight * 3) / 4;
            } else {
                x = (7 - i) * (mWidth / 4) + (mWidth / 8);
                y = mHeight / 4;
            }

            GLES20.glReadPixels(x, y, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuf);
            int r = pixelBuf.get(0) & 0xff;
            int g = pixelBuf.get(1) & 0xff;
            int b = pixelBuf.get(2) & 0xff;
            //Log.d(TAG, "GOT(" + frameIndex + "/" + i + "): r=" + r + " g=" + g + " b=" + b);

            int expR, expG, expB;
            if (i == frameIndex % 8) {
                // colored rect
                expR = TEST_R1;
                expG = TEST_G1;
                expB = TEST_B1;
            } else {
                // zero background color
                expR = TEST_R0;
                expG = TEST_G0;
                expB = TEST_B0;
            }
            if (!isColorClose(r, expR) ||
                    !isColorClose(g, expG) ||
                    !isColorClose(b, expB)) {
                Log.w(TAG, "Bad frame " + frameIndex + " (rect=" + i + ": rgb=" + r +
                        "," + g + "," + b + " vs. expected " + expR + "," + expG +
                        "," + expB + ")");
                frameFailed = true;
            }
        }

        return !frameFailed;
    }
    boolean isColorClose(int actual, int expected) {
        final int MAX_DELTA = 8;
        int delta = Math.abs(actual - expected);
        if (delta > mLargestColorDelta) {
            mLargestColorDelta = delta;
        }
        return (delta <= MAX_DELTA);
    }

    private static boolean isSemiPlanarYUV(int colorFormat) {
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                return false;
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                throw new RuntimeException("unknown format " + colorFormat);
        }
    }

    byte [] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {

        int [] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }

    void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                B = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                R = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }

    private void doEncodeDecodeVideoFromBuffer1(MediaCodec encoder, int encoderColorFormat,
                                                     MediaCodec decoder, boolean toSurface) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        ByteBuffer[] decoderInputBuffers = null;
        ByteBuffer[] decoderOutputBuffers = null;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long tsLong = 0, tslongMid = 0, tsLongEnd = 0;
        List<Long> encodedTime = new ArrayList<Long>();
        List<Long> decodedTime = new ArrayList<Long>();
        List<Integer> packetSize = new ArrayList<>();
        MediaFormat decoderOutputFormat = null;
        int generateIndex = 0;
        int checkIndex = 0;
        int badFrames = 0;
        boolean decoderConfigured = false;
        Surface outputSurface = null;

        // The size of a frame of video data, in the formats we handle, is stride*sliceHeight
        // for Y, and (stride/2)*(sliceHeight/2) for each of the Cb and Cr channels.  Application
        // of algebra and assuming that stride==width and sliceHeight==height yields:
        byte[] frameData = new byte[mWidth * mHeight * 3 / 2];

        // Just out of curiosity.
        long rawSize = 0;
        long encodedSize = 0;

        // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
        // stream, not a .mp4 file, so not all players will know what to do with it.
        FileOutputStream outputStream = null;
        FileOutputStream outputStream_out = null;
        FileOutputStream inputStream = null;
        if (DEBUG_SAVE_FILE) {
            String fileName_in = DEBUG_IN_FILE_NAME_BASE + "1" + mWidth + "x" + mHeight + ".yuv";
            String fileName = DEBUG_FILE_NAME_BASE +"1" + mWidth + "x" + mHeight + ".mp4";
            String fileName_out = DEBUG_OUT_FILE_NAME_BASE +"1" + mWidth + "x" + mHeight + ".yuv";
            try {
                inputStream = new FileOutputStream(fileName_in);
                Log.d(TAG, "encoded input will be saved as " + fileName_in);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }

            try {
                outputStream = new FileOutputStream(fileName);
                Log.d(TAG, "encoded output will be saved as " + fileName);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }
            try {
                outputStream_out = new FileOutputStream(fileName_out);
                Log.d(TAG, "decoded output will be saved as " + fileName_out);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug decode output file " + fileName_out);
                throw new RuntimeException(ioe);
            }
        }

        if (toSurface) {
            outputSurface = mSurface; //new OutputSurface(mWidth, mHeight);
        }

        // Loop until the output side is done.
        boolean inputDone = false;
        boolean encoderDone = false;
        boolean outputDone = false;
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop2");

            // If we're not done submitting frames, generate a new one and submit it.  By
            // doing this on every loop we're working to ensure that the encoder always has
            // work to do.
            //
            // We don't really want a timeout here, but sometimes there's a delay opening
            // the encoder device, so a short timeout can keep us from spinning hard.
            if (!inputDone && flag.get(1) == false) {
                int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (VERBOSE) Log.d(TAG, "inputBufIndex2=" + inputBufIndex);
                if (inputBufIndex >= 0) {
                    long ptsUsec = computePresentationTime(generateIndex);
                    if (generateIndex == NUM_FRAMES) {
                        // Send an empty frame with the end-of-stream flag set.  If we set EOS
                        // on a frame with data, that frame data will be ignored, and the
                        // output will be short one frame.
                        encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        outputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS (with zero-length frame)2");
                    } else {
                        // generateFrame(generateIndex, encoderColorFormat, frameData);
                        Bitmap bm = bitmapArrayRead.get(1).getNewImage();
                        flag.set(1,true);
                        Log.d ("Rao", "flag 2  true");
                        if (bm == null) {Log.d ("Rao", "Bm is null");}
                        byte[] frame = getNV21(smallimage_Width, smallimage_Height, bm);
                        ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];

//                        if (inputStream != null) {
//                            byte[] data = new byte[info.size];
//                            inputBuf.get(data);
//                            inputBuf.position(info.offset);
//                            try {
//                                inputStream.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file2");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file2");
//                        }
                        // the buffer should be sized to hold one full frame
                        //assertTrue(inputBuf.capacity() >= frameData.length);
                        inputBuf.clear();
                        inputBuf.put(frame);

                        encoder.queueInputBuffer(inputBufIndex, 0, frame.length, ptsUsec, 0);
                        tsLong = System.currentTimeMillis();
                        if (VERBOSE) Log.d(TAG, "submitted frame " + generateIndex + " to enc2");
                    }
                    generateIndex++;
                } else {
                    // either all in use, or we timed out during initial setup
                    if (VERBOSE) Log.d(TAG, "input buffer not available");

                }
            }

            // Check for output from the encoder.  If there's no output yet, we either need to
            // provide more input, or we need to wait for the encoder to work its magic.  We
            // can't actually tell which is the case, so if we can't get an output buffer right
            // away we loop around and see if it wants more input.
            //
            // Once we get EOS from the encoder, we don't need to do this anymore.
            if (!encoderDone) {
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "encoder2 output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // not expected for an encoder
                    MediaFormat newFormat = encoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "encoder2 output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    //fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        //fail("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);
                    tslongMid = System.currentTimeMillis();
                    long encodingTime = tslongMid - tsLong;
                    encodedTime.add(encodingTime);
                    encodedSize += info.size;
//                    if (outputStream != null) {
//                        byte[] data = new byte[info.size];
//                        encodedData.get(data);
//                        encodedData.position(info.offset);
//                        try {
//                            outputStream.write(data);
//                        } catch (IOException ioe) {
//                            Log.w(TAG, "failed writing debug data to file");
//                            throw new RuntimeException(ioe);
//                        }
//                        Log.w(TAG, "successful writing debug data to file2");
//                    }

                    if (decoderRequired) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // Codec config info.  Only expected on first packet.  One way to
                            // handle this is to manually stuff the data into the MediaFormat
                            // and pass that to configure().  We do that here to exercise the API.
                            //assertFalse(decoderConfigured);
                            MediaFormat format =
                                    MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                            format.setByteBuffer("csd-0", encodedData);
                            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                            //format.setInteger("color-format", 19);
                            decoder.configure(format, toSurface ? outputSurface : null,
                                    null, 0);
                            decoder.start();
                            decoderInputBuffers = decoder.getInputBuffers();
                            decoderOutputBuffers = decoder.getOutputBuffers();
                            decoderConfigured = true;
                            if (VERBOSE)
                                Log.d(TAG, "decoder2 configured (" + info.size + " bytes)");
                        } else {
                            // Get a decoder input buffer, blocking until it's available.
                            //assertTrue(decoderConfigured);
                            int inputBufIndex = decoder.dequeueInputBuffer(-1);
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            inputBuf.clear();
                            inputBuf.put(encodedData);
                            decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                                    info.presentationTimeUs, info.flags);
                            tslongMid = System.currentTimeMillis();
                            packetSize.add (info.size);
                            encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                            if (VERBOSE) Log.d(TAG, "passed " + info.size + " bytes to decoder2"
                                    + (encoderDone ? " (EOS)" : ""));
                        }
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
            }

            // Check for output from the decoder.  We want to do this on every loop to avoid
            // the possibility of stalling the pipeline.  We use a short timeout to avoid
            // burning CPU if the decoder is hard at work but the next frame isn't quite ready.
            //
            // If we're decoding to a Surface, we'll get notified here as usual but the
            // ByteBuffer references will be null.  The data is sent to Surface instead.
            if (decoderConfigured && decoderRequired) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // The storage associated with the direct ByteBuffer may already be unmapped,
                    // so attempting to access data through the old output buffer array could
                    // lead to a native crash.
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    decoderOutputFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else if (decoderStatus < 0) {
                    //fail("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
                } else {  // decoderStatus >= 0
                    if (!toSurface) {
                        ByteBuffer outputFrame = decoderOutputBuffers[decoderStatus];

                        outputFrame.position(info.offset);
                        outputFrame.limit(info.offset + info.size);

                        rawSize += info.size;

//                        if (outputStream_out != null) {
//                            byte[] data = new byte[info.size];
//                            outputFrame.get(data);
//                            outputFrame.position(info.offset);
//                            try {
//                                outputStream_out.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file");
//                        }

                        if (info.size == 0) {
                            if (VERBOSE) Log.d(TAG, "got empty frame");
                        } else {
                            if (VERBOSE) Log.d(TAG, "decoded, checking frame " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            if (!checkFrame(checkIndex++, decoderOutputFormat, outputFrame)) {
                                badFrames++;
                            }
                        }

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            outputDone = true;
                        }
                        tsLongEnd =System.currentTimeMillis();
                        long diff = tsLongEnd- tslongMid;
                        decodedTime.add(diff);
                        decoder.releaseOutputBuffer(decoderStatus, false /*render*/);
                    } else {
                        if (VERBOSE) Log.d(TAG, "surface decoder2 given buffer " + decoderStatus +
                                " (size=" + info.size + ")");
                        rawSize += info.size;
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output2 EOS");
                            outputDone = true;
                        }

                        boolean doRender = (info.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                        // that the texture will be available before the call returns, so we
                        // need to wait for the onFrameAvailable callback to fire.
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        if (doRender) {
                            if (VERBOSE) Log.d(TAG, "awaiting frame2 " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            //outputSurface.awaitNewImage();
                            //outputSurface.drawImage();
                            if (!checkSurfaceFrame(checkIndex++)) {
                                badFrames++;
                            }
                        }
                    }
                }
            }
        }
        if (VERBOSE) Log.d(TAG, "decoded " + checkIndex + " frames at "
                + mWidth + "x" + mHeight + ": raw=" + rawSize + ", enc=" + encodedSize);

        try {
            FileOutputStream Out = new FileOutputStream("/sdcard/"+"Logs_T2.txt");
            if (Out != null) {
                for (long x : encodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                String temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : decodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : packetSize) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }

                Out.close();
            } else {
                Log.d("shreyas1", "else" );
            }
        } catch (IOException ioe) {
            Log.w(TAG, "failed writing debug data to file");
            throw new RuntimeException(ioe);
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug file");
                throw new RuntimeException(ioe);
            }
        }
        if (outputStream_out != null) {
            try {
                outputStream_out.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug decode output file");
                throw new RuntimeException(ioe);
            }
        }

        if (outputSurface != null) {
            outputSurface.release();
        }

        if (checkIndex != NUM_FRAMES) {
            //fail("expected " + NUM_FRAMES + " frames, only decoded " + checkIndex);
        }
        if (badFrames != 0) {
            //fail("Found " + badFrames + " bad frames");
        }
    }

    private void doEncodeDecodeVideoFromBuffer2(MediaCodec encoder, int encoderColorFormat,
                                                MediaCodec decoder, boolean toSurface) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        ByteBuffer[] decoderInputBuffers = null;
        ByteBuffer[] decoderOutputBuffers = null;
        long tsLong = 0, tslongMid = 0, tsLongEnd = 0;
        List<Long> encodedTime = new ArrayList<Long>();
        List<Long> decodedTime = new ArrayList<Long>();
        List<Integer> packetSize = new ArrayList<>();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MediaFormat decoderOutputFormat = null;
        int generateIndex = 0;
        int checkIndex = 0;
        int badFrames = 0;
        boolean decoderConfigured = false;
        Surface outputSurface = null;

        // The size of a frame of video data, in the formats we handle, is stride*sliceHeight
        // for Y, and (stride/2)*(sliceHeight/2) for each of the Cb and Cr channels.  Application
        // of algebra and assuming that stride==width and sliceHeight==height yields:
        byte[] frameData = new byte[mWidth * mHeight * 3 / 2];

        // Just out of curiosity.
        long rawSize = 0;
        long encodedSize = 0;

        // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
        // stream, not a .mp4 file, so not all players will know what to do with it.
        FileOutputStream outputStream = null;
        FileOutputStream outputStream_out = null;
        FileOutputStream inputStream = null;
        if (DEBUG_SAVE_FILE) {
            String fileName_in = DEBUG_IN_FILE_NAME_BASE + "2" + mWidth + "x" + mHeight + ".yuv";
            String fileName = DEBUG_FILE_NAME_BASE +"2" + mWidth + "x" + mHeight + ".mp4";
            String fileName_out = DEBUG_OUT_FILE_NAME_BASE +"2" + mWidth + "x" + mHeight + ".yuv";
            try {
                inputStream = new FileOutputStream(fileName_in);
                Log.d(TAG, "encoded input will be saved as " + fileName_in);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }

            try {
                outputStream = new FileOutputStream(fileName);
                Log.d(TAG, "encoded output  3 will be saved as " + fileName);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }
            try {
                outputStream_out = new FileOutputStream(fileName_out);
                Log.d(TAG, "decoded output will be saved as " + fileName_out);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug decode output file " + fileName_out);
                throw new RuntimeException(ioe);
            }
        }

        if (toSurface) {
            outputSurface = mSurface; //new OutputSurface(mWidth, mHeight);
        }

        // Loop until the output side is done.
        boolean inputDone = false;
        boolean encoderDone = false;
        boolean outputDone = false;
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop3");

            // If we're not done submitting frames, generate a new one and submit it.  By
            // doing this on every loop we're working to ensure that the encoder always has
            // work to do.
            //
            // We don't really want a timeout here, but sometimes there's a delay opening
            // the encoder device, so a short timeout can keep us from spinning hard.
            if (!inputDone && flag.get(2) == false) {
                int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (VERBOSE) Log.d(TAG, "inputBufIndex3=" + inputBufIndex);
                if (inputBufIndex >= 0) {
                    long ptsUsec = computePresentationTime(generateIndex);
                    if (generateIndex == NUM_FRAMES) {
                        // Send an empty frame with the end-of-stream flag set.  If we set EOS
                        // on a frame with data, that frame data will be ignored, and the
                        // output will be short one frame.
                        encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        outputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS (with zero-length frame)3");
                    } else {
                        // generateFrame(generateIndex, encoderColorFormat, frameData);
                        Bitmap bm = bitmapArrayRead.get(2).getNewImage();
                        flag.set(2,true);
                        Log.d ("Rao", "flag 3  true");
                        if (bm == null) {Log.d ("Rao", "Bm is null");}
                        byte[] frame = getNV21(smallimage_Width, smallimage_Height, bm);
                        ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];

//                        if (inputStream != null) {
//                            byte[] data = new byte[info.size];
//                            inputBuf.get(data);
//                            inputBuf.position(info.offset);
//                            try {
//                                inputStream.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file3");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file3");
//                        }
                        // the buffer should be sized to hold one full frame
                        //assertTrue(inputBuf.capacity() >= frameData.length);
                        inputBuf.clear();
                        inputBuf.put(frame);

                        encoder.queueInputBuffer(inputBufIndex, 0, frame.length, ptsUsec, 0);
                        tsLong = System.currentTimeMillis();
                        if (VERBOSE) Log.d(TAG, "submitted frame " + generateIndex + " to enc3");
                    }
                    generateIndex++;
                } else {
                    // either all in use, or we timed out during initial setup
                    if (VERBOSE) Log.d(TAG, "input buffer not available");

                }
            }

            // Check for output from the encoder.  If there's no output yet, we either need to
            // provide more input, or we need to wait for the encoder to work its magic.  We
            // can't actually tell which is the case, so if we can't get an output buffer right
            // away we loop around and see if it wants more input.
            //
            // Once we get EOS from the encoder, we don't need to do this anymore.
            if (!encoderDone) {
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "encoder3 output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // not expected for an encoder
                    MediaFormat newFormat = encoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "encoder3 output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    //fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        //fail("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);
                    tslongMid = System.currentTimeMillis();
                    long encodingTime = tslongMid - tsLong;
                    encodedTime.add(encodingTime);
                    encodedSize += info.size;
//                    if (outputStream != null) {
//                        byte[] data = new byte[info.size];
//                        encodedData.get(data);
//                        encodedData.position(info.offset);
//                        try {
//                            outputStream.write(data);
//                        } catch (IOException ioe) {
//                            Log.w(TAG, "failed writing debug data to file");
//                            throw new RuntimeException(ioe);
//                        }
//                        Log.w(TAG, "successful writing debug data to file3");
//                    }

                    if (decoderRequired) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // Codec config info.  Only expected on first packet.  One way to
                            // handle this is to manually stuff the data into the MediaFormat
                            // and pass that to configure().  We do that here to exercise the API.
                            //assertFalse(decoderConfigured);
                            MediaFormat format =
                                    MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                            format.setByteBuffer("csd-0", encodedData);
                            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                            //format.setInteger("color-format", 19);
                            decoder.configure(format, toSurface ? outputSurface : null,
                                    null, 0);
                            decoder.start();
                            decoderInputBuffers = decoder.getInputBuffers();
                            decoderOutputBuffers = decoder.getOutputBuffers();
                            decoderConfigured = true;
                            if (VERBOSE)
                                Log.d(TAG, "decoder3 configured (" + info.size + " bytes)");
                        } else {
                            // Get a decoder input buffer, blocking until it's available.
                            //assertTrue(decoderConfigured);
                            int inputBufIndex = decoder.dequeueInputBuffer(-1);
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            inputBuf.clear();
                            inputBuf.put(encodedData);
                            decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                                    info.presentationTimeUs, info.flags);
                            tslongMid = System.currentTimeMillis();
                            packetSize.add (info.size);
                            encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                            if (VERBOSE) Log.d(TAG, "passed " + info.size + " bytes to decoder3"
                                    + (encoderDone ? " (EOS)" : ""));
                        }
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
            }

            // Check for output from the decoder.  We want to do this on every loop to avoid
            // the possibility of stalling the pipeline.  We use a short timeout to avoid
            // burning CPU if the decoder is hard at work but the next frame isn't quite ready.
            //
            // If we're decoding to a Surface, we'll get notified here as usual but the
            // ByteBuffer references will be null.  The data is sent to Surface instead.
            if (decoderConfigured && decoderRequired) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // The storage associated with the direct ByteBuffer may already be unmapped,
                    // so attempting to access data through the old output buffer array could
                    // lead to a native crash.
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    decoderOutputFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else if (decoderStatus < 0) {
                    //fail("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
                } else {  // decoderStatus >= 0
                    if (!toSurface) {
                        ByteBuffer outputFrame = decoderOutputBuffers[decoderStatus];

                        outputFrame.position(info.offset);
                        outputFrame.limit(info.offset + info.size);

                        rawSize += info.size;

//                        if (outputStream_out != null) {
//                            byte[] data = new byte[info.size];
//                            outputFrame.get(data);
//                            outputFrame.position(info.offset);
//                            try {
//                                outputStream_out.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file3");
//                        }

                        if (info.size == 0) {
                            if (VERBOSE) Log.d(TAG, "got empty frame");
                        } else {
                            if (VERBOSE) Log.d(TAG, "decoded, checking frame " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            if (!checkFrame(checkIndex++, decoderOutputFormat, outputFrame)) {
                                badFrames++;
                            }
                        }

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            outputDone = true;
                        }
                        tsLongEnd =System.currentTimeMillis();
                        long diff = tsLongEnd- tslongMid;
                        decodedTime.add(diff);
                        decoder.releaseOutputBuffer(decoderStatus, false /*render*/);
                    } else {
                        if (VERBOSE) Log.d(TAG, "surface decoder3 given buffer " + decoderStatus +
                                " (size=" + info.size + ")");
                        rawSize += info.size;
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output3 EOS");
                            outputDone = true;
                        }

                        boolean doRender = (info.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                        // that the texture will be available before the call returns, so we
                        // need to wait for the onFrameAvailable callback to fire.
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        if (doRender) {
                            if (VERBOSE) Log.d(TAG, "awaiting frame3 " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            //outputSurface.awaitNewImage();
                            //outputSurface.drawImage();
                            if (!checkSurfaceFrame(checkIndex++)) {
                                badFrames++;
                            }
                        }
                    }
                }
            }
        }
        if (VERBOSE) Log.d(TAG, "decoded " + checkIndex + " frames at "
                + mWidth + "x" + mHeight + ": raw=" + rawSize + ", enc=" + encodedSize);

        try {
            FileOutputStream Out = new FileOutputStream("/sdcard/"+"Logs_T3.txt");
            if (Out != null) {
                for (long x : encodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                String temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : decodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : packetSize) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }

                Out.close();
            } else {
                Log.d("shreyas1", "else" );
            }
        } catch (IOException ioe) {
            Log.w(TAG, "failed writing debug data to file");
            throw new RuntimeException(ioe);
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug file");
                throw new RuntimeException(ioe);
            }
        }
        if (outputStream_out != null) {
            try {
                outputStream_out.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug decode output file");
                throw new RuntimeException(ioe);
            }
        }

        if (outputSurface != null) {
            outputSurface.release();
        }

        if (checkIndex != NUM_FRAMES) {
            //fail("expected " + NUM_FRAMES + " frames, only decoded " + checkIndex);
        }
        if (badFrames != 0) {
            //fail("Found " + badFrames + " bad frames");
        }
    }

    private void doEncodeDecodeVideoFromBuffer3(MediaCodec encoder, int encoderColorFormat,
                                                MediaCodec decoder, boolean toSurface) {
        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        ByteBuffer[] decoderInputBuffers = null;
        ByteBuffer[] decoderOutputBuffers = null;
        long tsLong = 0, tslongMid = 0, tsLongEnd = 0;
        List<Long> encodedTime = new ArrayList<Long>();
        List<Long> decodedTime = new ArrayList<Long>();
        List<Integer> packetSize = new ArrayList<>();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        MediaFormat decoderOutputFormat = null;
        int generateIndex = 0;
        int checkIndex = 0;
        int badFrames = 0;
        boolean decoderConfigured = false;
        Surface outputSurface = null;

        // The size of a frame of video data, in the formats we handle, is stride*sliceHeight
        // for Y, and (stride/2)*(sliceHeight/2) for each of the Cb and Cr channels.  Application
        // of algebra and assuming that stride==width and sliceHeight==height yields:
        byte[] frameData = new byte[mWidth * mHeight * 3 / 2];

        // Just out of curiosity.
        long rawSize = 0;
        long encodedSize = 0;

        // Save a copy to disk.  Useful for debugging the test.  Note this is a raw elementary
        // stream, not a .mp4 file, so not all players will know what to do with it.
        FileOutputStream outputStream = null;
        FileOutputStream outputStream_out = null;
        FileOutputStream inputStream = null;
        if (DEBUG_SAVE_FILE) {
            String fileName_in = DEBUG_IN_FILE_NAME_BASE + "3" + mWidth + "x" + mHeight + ".yuv";
            String fileName = DEBUG_FILE_NAME_BASE +"3" + mWidth + "x" + mHeight + ".mp4";
            String fileName_out = DEBUG_OUT_FILE_NAME_BASE +"3" + mWidth + "x" + mHeight + ".yuv";
            try {
                inputStream = new FileOutputStream(fileName_in);
                Log.d(TAG, "encoded input will be saved as " + fileName_in);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }

            try {
                outputStream = new FileOutputStream(fileName);
                Log.d(TAG, "encoded output 4 will be saved as " + fileName);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug output file " + fileName);
                throw new RuntimeException(ioe);
            }
            try {
                outputStream_out = new FileOutputStream(fileName_out);
                Log.d(TAG, "decoded output will be saved as " + fileName_out);
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to create debug decode output file " + fileName_out);
                throw new RuntimeException(ioe);
            }
        }

        if (toSurface) {
            outputSurface = mSurface; //new OutputSurface(mWidth, mHeight);
        }

        // Loop until the output side is done.
        boolean inputDone = false;
        boolean encoderDone = false;
        boolean outputDone = false;
        while (!outputDone) {
            if (VERBOSE) Log.d(TAG, "loop4");

            // If we're not done submitting frames, generate a new one and submit it.  By
            // doing this on every loop we're working to ensure that the encoder always has
            // work to do.
            //
            // We don't really want a timeout here, but sometimes there's a delay opening
            // the encoder device, so a short timeout can keep us from spinning hard.
            if (!inputDone && flag.get(3) == false) {
                int inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (VERBOSE) Log.d(TAG, "inputBufIndex4=" + inputBufIndex);
                if (inputBufIndex >= 0) {
                    long ptsUsec = computePresentationTime(generateIndex);
                    if (generateIndex == NUM_FRAMES) {
                        // Send an empty frame with the end-of-stream flag set.  If we set EOS
                        // on a frame with data, that frame data will be ignored, and the
                        // output will be short one frame.
                        encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        outputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS (with zero-length frame)4");
                    } else {
                        // generateFrame(generateIndex, encoderColorFormat, frameData);
                        Bitmap bm = bitmapArrayRead.get(3).getNewImage();
                        flag.set(3,true);
                        Log.d ("Rao", "flag 4  true");
                        if (bm == null) {Log.d ("Rao", "Bm is null");}
                        byte[] frame = getNV21(smallimage_Width, smallimage_Height, bm);
                        ByteBuffer inputBuf = encoderInputBuffers[inputBufIndex];

//                        if (inputStream != null) {
//                            byte[] data = new byte[info.size];
//                            inputBuf.get(data);
//                            inputBuf.position(info.offset);
//                            try {
//                                inputStream.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file4");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file4");
//                        }
                        // the buffer should be sized to hold one full frame
                        //assertTrue(inputBuf.capacity() >= frameData.length);
                        inputBuf.clear();
                        inputBuf.put(frame);

                        encoder.queueInputBuffer(inputBufIndex, 0, frame.length, ptsUsec, 0);
                        tsLong = System.currentTimeMillis();
                        if (VERBOSE) Log.d(TAG, "submitted frame " + generateIndex + " to enc4");
                    }
                    generateIndex++;
                } else {
                    // either all in use, or we timed out during initial setup
                    if (VERBOSE) Log.d(TAG, "input buffer not available");

                }
            }

            // Check for output from the encoder.  If there's no output yet, we either need to
            // provide more input, or we need to wait for the encoder to work its magic.  We
            // can't actually tell which is the case, so if we can't get an output buffer right
            // away we loop around and see if it wants more input.
            //
            // Once we get EOS from the encoder, we don't need to do this anymore.
            if (!encoderDone) {
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    if (VERBOSE) Log.d(TAG, "encoder4 output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // not expected for an encoder
                    MediaFormat newFormat = encoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "encoder4 output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    //fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        //fail("encoderOutputBuffer " + encoderStatus + " was null");
                    }

                    // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);
                    tslongMid = System.currentTimeMillis();
                    long encodingTime = tslongMid - tsLong;
                    encodedTime.add(encodingTime);
                    encodedSize += info.size;
//                    if (outputStream != null) {
//                        byte[] data = new byte[info.size];
//                        encodedData.get(data);
//                        encodedData.position(info.offset);
//                        try {
//                            outputStream.write(data);
//                        } catch (IOException ioe) {
//                            Log.w(TAG, "failed writing debug data to file");
//                            throw new RuntimeException(ioe);
//                        }
//                        Log.w(TAG, "successful writing debug data to file4");
//                    }

                    if (decoderRequired) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // Codec config info.  Only expected on first packet.  One way to
                            // handle this is to manually stuff the data into the MediaFormat
                            // and pass that to configure().  We do that here to exercise the API.
                            //assertFalse(decoderConfigured);
                            MediaFormat format =
                                    MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                            format.setByteBuffer("csd-0", encodedData);
                            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                            //format.setInteger("color-format", 19);
                            decoder.configure(format, toSurface ? outputSurface : null,
                                    null, 0);
                            decoder.start();
                            decoderInputBuffers = decoder.getInputBuffers();
                            decoderOutputBuffers = decoder.getOutputBuffers();
                            decoderConfigured = true;
                            if (VERBOSE)
                                Log.d(TAG, "decoder4 configured (" + info.size + " bytes)");
                        } else {
                            // Get a decoder input buffer, blocking until it's available.
                            //assertTrue(decoderConfigured);
                            int inputBufIndex = decoder.dequeueInputBuffer(-1);
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            inputBuf.clear();
                            inputBuf.put(encodedData);
                            decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                                    info.presentationTimeUs, info.flags);
                            tslongMid = System.currentTimeMillis();
                            packetSize.add (info.size);
                            encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                            if (VERBOSE) Log.d(TAG, "passed " + info.size + " bytes to decoder4"
                                    + (encoderDone ? " (EOS)" : ""));
                        }
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
            }

            // Check for output from the decoder.  We want to do this on every loop to avoid
            // the possibility of stalling the pipeline.  We use a short timeout to avoid
            // burning CPU if the decoder is hard at work but the next frame isn't quite ready.
            //
            // If we're decoding to a Surface, we'll get notified here as usual but the
            // ByteBuffer references will be null.  The data is sent to Surface instead.
            if (decoderConfigured && decoderRequired) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // The storage associated with the direct ByteBuffer may already be unmapped,
                    // so attempting to access data through the old output buffer array could
                    // lead to a native crash.
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    decoderOutputFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else if (decoderStatus < 0) {
                    //fail("unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
                } else {  // decoderStatus >= 0
                    if (!toSurface) {
                        ByteBuffer outputFrame = decoderOutputBuffers[decoderStatus];

                        outputFrame.position(info.offset);
                        outputFrame.limit(info.offset + info.size);

                        rawSize += info.size;

//                        if (outputStream_out != null) {
//                            byte[] data = new byte[info.size];
//                            outputFrame.get(data);
//                            outputFrame.position(info.offset);
//                            try {
//                                outputStream_out.write(data);
//                            } catch (IOException ioe) {
//                                Log.w(TAG, "failed writing debug data to file");
//                                throw new RuntimeException(ioe);
//                            }
//                            Log.w(TAG, "successful writing debug data to file4");
//                        }

                        if (info.size == 0) {
                            if (VERBOSE) Log.d(TAG, "got empty 4 frame");
                        } else {
                            if (VERBOSE) Log.d(TAG, "decoded, checking frame " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            if (!checkFrame(checkIndex++, decoderOutputFormat, outputFrame)) {
                                badFrames++;
                            }
                        }

                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            outputDone = true;
                        }
                        tsLongEnd =System.currentTimeMillis();
                        long diff = tsLongEnd- tslongMid;
                        decodedTime.add(diff);
                        decoder.releaseOutputBuffer(decoderStatus, false /*render*/);
                    } else {
                        if (VERBOSE) Log.d(TAG, "surface decoder4 given buffer " + decoderStatus +
                                " (size=" + info.size + ")");
                        rawSize += info.size;
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output4 EOS");
                            outputDone = true;
                        }

                        boolean doRender = (info.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                        // that the texture will be available before the call returns, so we
                        // need to wait for the onFrameAvailable callback to fire.
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        if (doRender) {
                            if (VERBOSE) Log.d(TAG, "awaiting frame4 " + checkIndex);
                            //assertEquals("Wrong time stamp", computePresentationTime(checkIndex),
                            //        info.presentationTimeUs);
                            //outputSurface.awaitNewImage();
                            //outputSurface.drawImage();
                            if (!checkSurfaceFrame(checkIndex++)) {
                                badFrames++;
                            }
                        }
                    }
                }
            }
        }
        if (VERBOSE) Log.d(TAG, "decoded " + checkIndex + " frames at "
                + mWidth + "x" + mHeight + ": raw=" + rawSize + ", enc=" + encodedSize);

        try {
            FileOutputStream Out = new FileOutputStream("/sdcard/"+"Logs_T4.txt");
            if (Out != null) {
                for (long x : encodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                String temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : decodedTime) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }
                temp1 = System.lineSeparator()+System.lineSeparator();
                Out.write(temp1.getBytes());
                for (long x : packetSize) {
                    String temp = Long.toString(x)+System.lineSeparator();
                    Out.write(temp.getBytes());
                }

                Out.close();
            } else {
                Log.d("shreyas1", "else" );
            }
        } catch (IOException ioe) {
            Log.w(TAG, "failed writing debug data to file");
            throw new RuntimeException(ioe);
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug file");
                throw new RuntimeException(ioe);
            }
        }
        if (outputStream_out != null) {
            try {
                outputStream_out.close();
            } catch (IOException ioe) {
                Log.w(TAG, "failed closing debug decode output file");
                throw new RuntimeException(ioe);
            }
        }

        if (outputSurface != null) {
            outputSurface.release();
        }

        if (checkIndex != NUM_FRAMES) {
            //fail("expected " + NUM_FRAMES + " frames, only decoded " + checkIndex);
        }
        if (badFrames != 0) {
            //fail("Found " + badFrames + " bad frames");
        }
    }
}
