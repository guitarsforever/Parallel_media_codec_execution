package com.example.encoderdecodertest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main2Activity extends Activity {
    private static final String FILE_NAME_BASE = "/sdcard/testfiles/";
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    List<image> bitmapArrayRead ;
    List<image> bitmapArrayWrite ;
    FileOutputStream inputStream = null;
    private static final String TAG = "EncodeDecodeTest";

    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;
    private static final boolean VERBOSE = true;
    FileOutputStream outputStream = null;
    FileOutputStream outputStream1 = null;
    // size of a frame, in pixels
    private int mWidth = 320;
    private int mHeight = 240;
    String fileName = "/sdcard/input." + mWidth + "x" + mHeight + ".mp4";
    String fileName1 = "/sdcard/input." + mWidth + "x" + mHeight + "1"+ ".mp4";
    // bit rate, in bits per second
    private int mBitRate = -1;
    MediaCodec encoder1 = null;
    MediaCodec decoder1 = null;
    MediaCodec encoder2 = null;
    MediaCodec decoder2 = null;


    void InitAllCodecs () {
        final int TIMEOUT_USEC = 10000;
        setParameters(320, 240, 2000000);


        try {
            outputStream = new FileOutputStream(fileName);
            Log.d(TAG, "encoded output will be saved as " + fileName);
        } catch (IOException ioe) {
            Log.w(TAG, "Unable to create debug output file " + fileName);
            throw new RuntimeException(ioe);
        }

        try {
            outputStream1 = new FileOutputStream(fileName1);
            Log.d(TAG, "encoded output will be saved as " + fileName1);
        } catch (IOException ioe) {
            Log.w(TAG, "Unable to create debug output file " + fileName1);
            throw new RuntimeException(ioe);
        }

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
            encoder1 = MediaCodec.createByCodecName(codecInfo.getName());

            encoder1.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec mc, int inputBufferId) {
                    int inputBufIndex = mc.dequeueInputBuffer(TIMEOUT_USEC);
                    Log.d(TAG, "shreyas here");
                    if (bitmapArrayRead.size()!=0 && inputBufIndex >= 0) {
                        Log.d(TAG, "shreyas here 1");
                        ByteBuffer inputBuf = mc.getInputBuffer(inputBufferId);
                        Bitmap bm = bitmapArrayRead.get(0).getNewImage();
                        byte[] frame = getNV21(mWidth,mHeight,bm);
                        //todo - need to get the frameIndex into this callback
                        long ptsUsec = computePresentationTime(0); //need to send the framenumber as arguement,hardcoded 0 for testing
                        if (inputStream != null) {
                            byte[] data = new byte[info.size];
                            inputBuf.get(data);
                            inputBuf.position(info.offset);
                            try {
                                inputStream.write(data);
                            } catch (IOException ioe) {
                                Log.w(TAG, "failed writing debug data to file");
                                throw new RuntimeException(ioe);
                            }
                            Log.w(TAG, "successful writing debug data to file");
                        }
                        // the buffer should be sized to hold one full frame
                        //assertTrue(inputBuf.capacity() >= frameData.length);
                        inputBuf.clear();
                        inputBuf.put(frame);
                        encoder1.queueInputBuffer(inputBufIndex, 0, frame.length, ptsUsec, 0);
                    }
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec mc, int outputBufferId, MediaCodec.BufferInfo x) {
                    ByteBuffer encodedData = encoder1.getOutputBuffer(outputBufferId);
                    MediaFormat bufferFormat = encoder1.getOutputFormat(outputBufferId); // option A
                    encodedData.position(x.offset);
                    encodedData.limit(x.offset + x.size);
                    Log.d(TAG, "shreyas here 2");

                    if (outputStream != null) {
                        byte[] data = new byte[x.size];
                        encodedData.get(data);
                        encodedData.position(x.offset);
                        try {
                            outputStream.write(data);
                        } catch (IOException ioe) {
                            Log.w(TAG, "failed writing debug data to file");
                            throw new RuntimeException(ioe);
                        }
                        Log.w(TAG, "successful writing debug data to file");
                    }
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: returned output buffer: " + outputBufferId);
                        Log.d(TAG, "video decoder: returned buffer of size " + x.size);
                    }
                    encoder1.releaseOutputBuffer(outputBufferId,false);
                }

                @Override
                public void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {

                }


                @Override
                public void onError(MediaCodec mc, MediaCodec.CodecException x) {
                    Log.d(TAG, "video encoder2 : Error ");
                }
            });

            encoder1.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder1.start();

            encoder2 = MediaCodec.createByCodecName(codecInfo.getName());
            encoder2.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec mc, int inputBufferId) {
                    int inputBufIndex = encoder2.dequeueInputBuffer(TIMEOUT_USEC);
                    Log.d(TAG, "shreyas here 4");
                    if (bitmapArrayRead.size()!=0 && inputBufIndex >= 0) {
                        Log.d(TAG, "shreyas here 5");
                        ByteBuffer inputBuf = encoder2.getInputBuffer(inputBufferId);
                        Bitmap bm = bitmapArrayRead.get(1).getNewImage();
                        byte[] frame = getNV21(mWidth,mHeight,bm);
                        //todo - need to get the frameIndex into this callback
                        long ptsUsec = computePresentationTime(0); //need to send the framenumber as arguement,hardcoded 0 for testing
                        if (inputStream != null) {
                            byte[] data = new byte[info.size];
                            inputBuf.get(data);
                            inputBuf.position(info.offset);
                            try {
                                inputStream.write(data);
                            } catch (IOException ioe) {
                                Log.w(TAG, "failed writing debug data to file");
                                throw new RuntimeException(ioe);
                            }
                            Log.w(TAG, "successful writing debug data to file");
                        }
                        // the buffer should be sized to hold one full frame
                        //assertTrue(inputBuf.capacity() >= frameData.length);
                        inputBuf.clear();
                        inputBuf.put(frame);
                        encoder2.queueInputBuffer(inputBufIndex, 0, frame.length, ptsUsec, 0);
                    }
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec mc, int outputBufferId, MediaCodec.BufferInfo x) {
                    ByteBuffer encodedData = encoder1.getOutputBuffer(outputBufferId);
                    MediaFormat bufferFormat = encoder1.getOutputFormat(outputBufferId); // option A
                    if (outputStream1 != null) {
                        byte[] data = new byte[x.size];
                        encodedData.get(data);
                        encodedData.position(x.offset);
                        try {
                            outputStream1.write(data);
                        } catch (IOException ioe) {
                            Log.w(TAG, "failed writing debug data to file");
                            throw new RuntimeException(ioe);
                        }
                        Log.w(TAG, "successful writing debug data to file");
                    }
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: returned output buffer: " + outputBufferId);
                        Log.d(TAG, "video decoder: returned buffer of size " + x.size);
                    }
                    encoder1.releaseOutputBuffer(outputBufferId,false);
                }

                @Override
                public void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {
                }

                @Override
                public void onError(MediaCodec mc, MediaCodec.CodecException x) {
                    Log.d(TAG, "video encoder2 : Error ");
                }
            });

            encoder2.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder2.start();

            // Create a MediaCodec for the decoder, just based on the MIME type.  The various
            // format details will be passed through the csd-0 meta-data later on.
            decoder1 = MediaCodec.createDecoderByType(MIME_TYPE);
            decoder2 = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException a) {} finally {
            if (VERBOSE) Log.d(TAG, "releasing codecs");
            if (encoder1 != null) {
                encoder1.stop();
                encoder1.release();
            }
            if (decoder1 != null) {
                decoder1.stop();
                decoder1.release();
            }

            if (encoder2 != null) {
                encoder2.stop();
                encoder2.release();
            }
            if (decoder2 != null) {
                decoder2.stop();
                decoder2.release();
            }
        }
    }

//    void ExitCodecs () {
//        if (VERBOSE) Log.d(TAG, "releasing codecs");
//        if (encoder1 != null) {
//            encoder1.stop();
//            encoder1.release();
//        }
//        if (decoder1 != null) {
//            decoder1.stop();
//            decoder1.release();
//        }
//
//        if (encoder2 != null) {
//            encoder2.stop();
//            encoder2.release();
//        }
//        if (decoder2 != null) {
//            decoder2.stop();
//            decoder2.release();
//        }
//
//    }
    private void setParameters(int width, int height, int bitRate) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        Log.d(TAG, "SetParameter : width = " + width +" Height = " + height);
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
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

    public void ProcessVideo () {
        int fileIndex = 1;
        String filepath = FILE_NAME_BASE+"thumb000"+fileIndex+".jpg";
//        if (fileIndex>99) {
//            filepath = FILE_NAME_BASE+"thumb0"+fileIndex+".jpg";
//        } else if (fileIndex>9) {
//            filepath = FILE_NAME_BASE+"thumb00"+fileIndex+".jpg";
//        }
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
        buildImageArray(bm,0,fileIndex,2);
        byte[] frame = getNV21(mWidth,mHeight,bm);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        List<image> li = new ArrayList<image>();
        List<image> write = new ArrayList<image>();
        bitmapArrayRead = Collections.synchronizedList(li);
        bitmapArrayWrite= Collections.synchronizedList(write);
        ProcessVideo();
        InitAllCodecs ();

        //ExitCodecs();
    }
    //responsible to build the datastructure and populate the global array
    void buildImageArray(Bitmap b, int status, int frameNumber, int splitNumber) {
        ArrayList<Bitmap> tempArray = splitImage(b,splitNumber);
        for (Bitmap B : tempArray) {
            image buildImageObj = new image (0,B,frameNumber);
            bitmapArrayRead.add(buildImageObj);
        }

    }

    // responsible to split the images
    private ArrayList<Bitmap> splitImage(Bitmap b, int smallimage_Numbers) {
        //For the number of rows and columns of the grid to be displayed
        int rows, cols;
        //For height and width of the small image smallimage_s
        int smallimage_Height, smallimage_Width;
        //To store all the small image smallimage_s in bitmap format in this list
        ArrayList<Bitmap> smallimages = new ArrayList<Bitmap>(smallimage_Numbers);
        //Getting the scaled bitmap of the source image
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
    byte [] getNV21(int inputWidth, int inputHeight, Bitmap scaled) {

        int [] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }

    private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
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
}
