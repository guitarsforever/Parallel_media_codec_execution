package com.example.encoderdecodertest;
import android.graphics.Bitmap;
/**
 * Created by shreyassudheendrarao on 4/24/18.
 */
public class image {
    Bitmap newImage;
    int Status;
    int frameNumber;

    public image(int status, Bitmap newImage, int frameNumber) {
        Status = status;
        this.newImage = newImage;
        this.frameNumber = frameNumber;
    }

    public Bitmap getNewImage() {
        return newImage;
    }

    public void setNewImage(Bitmap newImage) {
        this.newImage = newImage;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(int frameNumber) {
        this.frameNumber = frameNumber;
    }

    public int getStatus() {
        return Status;
    }

    public void setStatus(int status) {
        Status = status;
    }
}
