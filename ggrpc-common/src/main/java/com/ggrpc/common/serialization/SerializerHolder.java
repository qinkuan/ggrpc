/**
 * FileName: SerializerHolder
 * Author:   yangqinkuan
 * Date:     2019-12-3 17:06
 * Description:
 */

package com.ggrpc.common.serialization;

import com.ggrpc.common.spi.BaseServiceLoader;

public class SerializerHolder {
    private static final Serializer serializer = BaseServiceLoader.load(Serializer.class);

    public static Serializer serializerImpl() {
        return serializer;
    }
}
