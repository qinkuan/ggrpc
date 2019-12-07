package com.ggrpc.client.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface RPCConsumer {
    String serviceName() default "";//服务名
}
