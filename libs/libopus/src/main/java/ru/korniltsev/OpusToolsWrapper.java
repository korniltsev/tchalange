package ru.korniltsev;

public class OpusToolsWrapper {
    static {
        System.loadLibrary("opustoolswrapper");
    }

    // ogg(opus) -> pcm16
    public static native boolean decode(String srcFilePath, String dstFilePath);

    // pcm16 mono -> ogg(opus)
    public static native boolean encode(String srcFilePath, String dstFilePath);


}
