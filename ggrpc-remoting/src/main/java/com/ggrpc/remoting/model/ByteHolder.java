/**
 * FileName: ByteHolder
 * Author:   yangqinkuan
 * Date:     2019-12-3 8:21
 * Description:
 */

package com.ggrpc.remoting.model;

public class ByteHolder {
    public transient byte[] bytes;

    public byte[] bytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int size(){
        return bytes == null?0:bytes.length;
    }
}
