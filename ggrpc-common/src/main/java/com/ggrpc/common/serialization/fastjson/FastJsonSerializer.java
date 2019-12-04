/**
 * FileName: FastJsonSerializer
 * Author:   yangqinkuan
 * Date:     2019-12-4 15:20
 * Description:
 */

package com.ggrpc.common.serialization.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ggrpc.common.serialization.Serializer;

public class FastJsonSerializer implements Serializer {
    @Override
    public <T> byte[] writeObject(T obj) {
        return JSON.toJSONBytes(obj, SerializerFeature.SortField);
    }

    @Override
    public <T> T readObject(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes, clazz, Feature.SortFeidFastMatch);
    }
}
