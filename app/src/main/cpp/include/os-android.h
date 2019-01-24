#ifndef _OS_ANDROID_H_

#define _OS_ANDROID_H_

#include <android/log.h>
#include <jni.h>

#define LOG_TAG "zhanfang"

#define LOG_DEBUG	ANDROID_LOG_DEBUG
#define LOG_INFO	ANDROID_LOG_INFO
#define LOG_ERROR	ANDROID_LOG_ERROR

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define LOG(LOG_LEVEL, ...)  __android_log_print(LOG_LEVEL, LOG_TAG, __VA_ARGS__)

#endif
