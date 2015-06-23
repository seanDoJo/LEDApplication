package com.cisco.prototype.ledsignaldetection;

import org.opencv.contrib.FaceRecognizer;

/**
 * Created by seandonohoe on 6/23/15.
 */
public class FisherFaceRecognizer extends FaceRecognizer {
    static{
        System.loadLibrary("facerec");
    }

    private static native long createFisherFaceRecognizer();
    private static native long createFisherFaceRecognizer1(int NumComponents);
    private static native long createFisherFaceRecognizer2(int NumComponents, double threshold);

    public FisherFaceRecognizer () {
        super(createFisherFaceRecognizer());
    }
    public FisherFaceRecognizer(int NumComponents){
        super(createFisherFaceRecognizer1(NumComponents));
    }
    public FisherFaceRecognizer(int NumComponents, double threshold){
        super(createFisherFaceRecognizer2(NumComponents, threshold));
    }
}
