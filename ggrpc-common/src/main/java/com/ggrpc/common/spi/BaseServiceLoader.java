/**
 * FileName: BaseServiceLoader
 * Author:   yangqinkuan
 * Date:     2019-12-3 17:04
 * Description:
 */

package com.ggrpc.common.spi;

import java.util.ServiceLoader;

public class BaseServiceLoader {
    public static <S> S load (Class<S> serviceClass){
        return ServiceLoader.load(serviceClass).iterator().next();
    }
}
