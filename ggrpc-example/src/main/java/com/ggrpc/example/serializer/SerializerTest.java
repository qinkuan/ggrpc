/**
 * FileName: SerializerTest
 * Author:   yangqinkuan
 * Date:     2019-12-5 8:11
 * Description:
 */

package com.ggrpc.example.serializer;

import com.ggrpc.example.netty.TestCommonCustomBody;
import com.ggrpc.example.netty.TestCommonCustomBody.ComplexTestObj;
import static com.ggrpc.common.serialization.SerializerHolder.serializerImpl;

public class SerializerTest {

    public static void main(String[] args) {

        long beginTime = System.currentTimeMillis();
        int length = 0;
        for(int i = 0;i < 1000000;i++){
            ComplexTestObj complexTestObj = new ComplexTestObj("attr1", 2);
            TestCommonCustomBody commonCustomHeader = new TestCommonCustomBody(1, "test",complexTestObj);
            byte[] bytes = serializerImpl().writeObject(commonCustomHeader);
            length = bytes.length;
            TestCommonCustomBody body = serializerImpl().readObject(bytes, TestCommonCustomBody.class);
        }
        System.out.println(length);
        long endTime = System.currentTimeMillis();

        System.out.println((endTime - beginTime));

    }
}
