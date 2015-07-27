package Recognizers;

/**
 * Created by seandonohoe on 6/16/15.
 */
import org.opencv.contrib.FaceRecognizer;
import org.opencv.core.Core;


public class EigenFaceRecognizer extends FaceRecognizer {

    static{
        System.loadLibrary("facerec");
    }

    private static native long createEigenFaceRecognizer();
    private static native long createEigenFaceRecognizer1(int NumComponents);
    private static native long createEigenFaceRecognizer2(int NumComponents, double threshold);

    public EigenFaceRecognizer () {
        super(createEigenFaceRecognizer());
    }
    public EigenFaceRecognizer(int NumComponents){
        super(createEigenFaceRecognizer1(NumComponents));
    }
    public EigenFaceRecognizer(int NumComponents, double threshold){
        super(createEigenFaceRecognizer2(NumComponents, threshold));
    }
}
