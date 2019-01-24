//
// Created by Zhan,Fang on 2018/10/23.
//
#include <sstream>
#include "JEnv.h"

using namespace tns;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JEnv::Init(vm);
    return JNI_VERSION_1_6;
}
