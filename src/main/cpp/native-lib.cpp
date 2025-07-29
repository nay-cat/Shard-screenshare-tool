#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nay_shard_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Shard 2.0.0";
    return env->NewStringUTF(hello.c_str());
}