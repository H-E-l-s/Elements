package com.hels.elements;

import java.nio.ByteBuffer;

public class Utils {

    static int intFromByteArray(byte[] a, int first, int length) {
        ByteBuffer wrapped = ByteBuffer.wrap(a, first, length); // big-endian by default
        return wrapped.getInt();     // 1
    }
//    ByteBuffer dbuf = ByteBuffer.allocate(2);
//    dbuf.putShort(num);
//    byte[] bytes = dbuf.array(); // { 0, 1 }


}
