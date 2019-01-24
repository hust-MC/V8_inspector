#include "jni.h"
#include "AssetExtractor.h"

#include <sstream>

using namespace tns;
using namespace std;

extern "C" JNIEXPORT void Java_com_example_zhanfang_test_AssetExtractor_extractAssets(JNIEnv* env, jobject obj, jstring apk, jstring inputDir, jstring outputDir, jboolean _forceOverwrite) {
    AssetExtractor::ExtractAssets(env, obj, apk, inputDir, outputDir, _forceOverwrite);
}
