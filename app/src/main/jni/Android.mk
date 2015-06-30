LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
include /Users/seandonohoe/Documents/libraries/OpenCV-android-sdk-old/sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  := facerec.cpp
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := facerec

include $(BUILD_SHARED_LIBRARY)