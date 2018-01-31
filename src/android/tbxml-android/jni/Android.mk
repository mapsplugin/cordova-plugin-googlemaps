LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := tbxml
LOCAL_SRC_FILES := tbxml.c
LOCAL_LDLIBS    := -llog 

include $(BUILD_SHARED_LIBRARY)
