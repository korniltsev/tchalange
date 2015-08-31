package ru.korniltsev;

public class OpusToolsWrapper {
    static {
        System.loadLibrary("opustoolswrapper");
    }

    // ogg(opus) -> pcm16
    public static native boolean decode(String srcFilePath, String dstFilePath);

    public static native boolean encode(String srcFilePath, String dstFilePath,  int rawBits, int rawRate, int rawChan);


    //    public static native boolean opusenc(String []args);
}
