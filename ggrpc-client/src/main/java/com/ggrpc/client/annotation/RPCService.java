package com.ggrpc.client.annotation;


import java.lang.annotation.*;

/**
 * 服务者注解
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface RPCService {
    String serviceName() default "";					//服务名
    int weight() default 50;							//负载访问权重
    String responsibilityName() default "system";	//负责人名
    int connCount() default 1;						//单实例连接数，注册中心该参数有效，直连无效
    boolean isVIPService() default false;			//是否是VIP服务
    boolean isSupportDegradeService() default false; //是否支持降级
    String degradeServicePath() default "";			//如果支持降级，降级服务的路径
    String degradeServiceDesc() default "";			//降级服务的描述
    boolean isFlowController() default true;		    //是否单位时间限流
    long maxCallCountInMinute() default 100000;		//单位时间的最大调用量
}
