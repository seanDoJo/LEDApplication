#include "jni.h"
#include "opencv2/contrib/contrib.hpp"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_Recognizers_EigenFaceRecognizer_createEigenFaceRecognizer(JNIEnv* env, jclass);
JNIEXPORT jlong JNICALL Java_Recognizers_EigenFaceRecognizer_createEigenFaceRecognizer(JNIEnv* env, jclass) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createEigenFaceRecognizer();
        pfr.addref(); // this is for the 2.4 branch, 3.0 would need a different treatment here
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_Recognizers_EigenFaceRecognizer_createEigenFaceRecognizer1(JNIEnv* env, jclass, jint num_components);
JNIEXPORT jlong JNICALL Java_Recognizers_EigenFaceRecognizer_createEigenFaceRecognizer1(JNIEnv* env, jclass, jint num_components) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createEigenFaceRecognizer(num_components);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_Recognizers_EigenFaceRecognizer_createEigenFaceRecognizer2(JNIEnv* env, jclass, jint num_components, jdouble threshold);
JNIEXPORT jlong JNICALL Java_Recognizers_EigenFaceRecognizer_createEigenFaceRecognizer2(JNIEnv* env, jclass, jint num_components, jdouble threshold) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createEigenFaceRecognizer(num_components,threshold);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_Recognizers_LbphRecognizer_createLbphRecognizer(JNIEnv* env, jclass);
JNIEXPORT jlong JNICALL Java_Recognizers_LbphRecognizer_createLbphRecognizer(JNIEnv* env, jclass) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createLBPHFaceRecognizer();
        pfr.addref(); // this is for the 2.4 branch, 3.0 would need a different treatment here
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}
JNIEXPORT jlong JNICALL Java_Recognizers_LbphRecognizer_createLbphRecognizer1(JNIEnv* env, jclass, jint radius, jint neighbors);
JNIEXPORT jlong JNICALL Java_Recognizers_LbphRecognizer_createLbphRecognizer1(JNIEnv* env, jclass, jint radius, jint neighbors) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createLBPHFaceRecognizer(radius,neighbors);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}
JNIEXPORT jlong JNICALL Java_Recognizers_LbphRecognizer_createLbphRecognizer2(JNIEnv* env, jclass, jint radius, jint neighbors, jint grid_x, jint grid_y, jdouble threshold);
JNIEXPORT jlong JNICALL Java_Recognizers_LbphRecognizer_createLbphRecognizer2(JNIEnv* env, jclass, jint radius, jint neighbors, jint grid_x, jint grid_y, jdouble threshold) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createLBPHFaceRecognizer(radius,neighbors,grid_x,grid_y,threshold);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_Recognizers_FisherFaceRecognizer_createFisherFaceRecognizer(JNIEnv* env, jclass);
JNIEXPORT jlong JNICALL Java_Recognizers_FisherFaceRecognizer_createFisherFaceRecognizer(JNIEnv* env, jclass) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createFisherFaceRecognizer();
        pfr.addref(); // this is for the 2.4 branch, 3.0 would need a different treatment here
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_Recognizers_FisherFaceRecognizer_createFisherFaceRecognizer1(JNIEnv* env, jclass, jint num_components);
JNIEXPORT jlong JNICALL Java_Recognizers_FisherFaceRecognizer_createFisherFaceRecognizer1(JNIEnv* env, jclass, jint num_components) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createFisherFaceRecognizer(num_components);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_Recognizers_FisherFaceRecognizer_createFisherFaceRecognizer2(JNIEnv* env, jclass, jint num_components, jdouble threshold);
JNIEXPORT jlong JNICALL Java_Recognizers_FisherFaceRecognizer_createFisherFaceRecognizer2(JNIEnv* env, jclass, jint num_components, jdouble threshold) {
    try {
        cv::Ptr<cv::FaceRecognizer> pfr = cv::createFisherFaceRecognizer(num_components,threshold);
        pfr.addref();
        return (jlong) pfr.obj;
    } catch (...) {
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "sorry, dave..");
    }
    return 0;
}
#ifdef __cplusplus
}
#endif