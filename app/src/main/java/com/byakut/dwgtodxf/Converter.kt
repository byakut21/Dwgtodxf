package com.byakut.dwgtodxf

/** Thin wrapper over the native LibreDWG converter (libdwgconv.so). */
object Converter {
    init {
        System.loadLibrary("dwgconv")
    }

    /** Returns 0 on success, otherwise an error code. */
    @JvmStatic external fun dwgToDxf(inPath: String, outPath: String): Int

    /** Returns 0 on success, otherwise an error code. Output is AutoCAD 2000 DWG. */
    @JvmStatic external fun dxfToDwg(inPath: String, outPath: String): Int
}
