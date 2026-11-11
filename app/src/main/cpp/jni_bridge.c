#include <jni.h>
#include "convert.h"

static jint
call (JNIEnv *env, jstring in, jstring out,
      int (*fn) (const char *, const char *))
{
  const char *i = (*env)->GetStringUTFChars (env, in, 0);
  const char *o = (*env)->GetStringUTFChars (env, out, 0);
  int r = fn (i, o);
  (*env)->ReleaseStringUTFChars (env, in, i);
  (*env)->ReleaseStringUTFChars (env, out, o);
  return r;
}

JNIEXPORT jint JNICALL
Java_com_byakut_dwgtodxf_Converter_dwgToDxf (JNIEnv *env, jclass cls,
                                              jstring in, jstring out)
{
  return call (env, in, out, conv_dwg_to_dxf);
}

JNIEXPORT jint JNICALL
Java_com_byakut_dwgtodxf_Converter_dxfToDwg (JNIEnv *env, jclass cls,
                                              jstring in, jstring out)
{
  return call (env, in, out, conv_dxf_to_dwg);
}
