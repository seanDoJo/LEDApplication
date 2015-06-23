package com.cisco.prototype.ledsignaldetection;

import org.opencv.contrib.FaceRecognizer;

/**
 * Created by seandonohoe on 6/23/15.
 */
public class LbphRecognizer extends FaceRecognizer {
    static{
        System.loadLibrary("facerec");
    }
/*****THESE METHODS ARE BROKEN -- PLEASE FIX*****/
    private static native long createLbphRecognizer();
    private static native long createLbphRecognizer1(int radius, int neighbors);
    private static native long createLbphRecognizer2(int radius, int neighbors, int grid_x, int grid_y, double threshold);

    public LbphRecognizer () {
        super(createLbphRecognizer());
    }
    public LbphRecognizer(int radius, int neighbors){
        super(createLbphRecognizer1(radius, neighbors));
    }
    public LbphRecognizer(int radius, int neighbors, int grid_x, int grid_y, double threshold){
        super(createLbphRecognizer2(radius,neighbors,grid_x,grid_y,threshold));
    }
}
