/**
 * FileName: NativeSupport
 * Author:   yangqinkuan
 * Date:     2019-12-6 14:19
 * Description:
 */

package com.ggrpc.common.utils;

public class NativeSupport {
    private static final boolean SUPPORT_NATIVE_ET;
    static {
        boolean epoll;
        try {
            Class.forName("io.netty.channel.epoll.Native");
            epoll = true;
        } catch (Throwable e) {
            epoll = false;
        }
        SUPPORT_NATIVE_ET = epoll;
    }

    /**
     * The native socket transport for Linux using JNI.
     */
    public static boolean isSupportNativeET() {
        return SUPPORT_NATIVE_ET;
    }
}
