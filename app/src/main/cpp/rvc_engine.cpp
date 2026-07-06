#include <jni.h>
#include <android/log.h>

#define LOG_TAG "RVC_NDK_CORE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rvc_patch_ipc_IPCManager_initializeNativeEngine(
    JNIEnv *env,
    jobject /* this */,
    jobject fileDescriptor,
    jint bufferSize) {
    LOGI("Dummy engine initialized (source code was missing dependencies)");
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_rvc_patch_ipc_IPCManager_processAudioNative(
    JNIEnv *env,
    jobject /* this */,
    jint bytesRead) {
    return JNI_TRUE;
}
