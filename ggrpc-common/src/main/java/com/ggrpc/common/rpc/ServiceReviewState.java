/**
 * FileName: ServiceReviewState
 * Author:   yangqinkuan
 * Date:     2019-12-5 15:13
 * Description:
 */

package com.ggrpc.common.rpc;

public enum ServiceReviewState {
    HAS_NOT_REVIEWED, //未审核
    PASS_REVIEW,      //通过审核
    NOT_PASS_REVIEW,  //未通过审核
    FORBIDDEN         //禁用
}
