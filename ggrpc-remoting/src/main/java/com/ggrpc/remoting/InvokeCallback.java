/**
 * FileName: InvokeCallback
 * Author:   yangqinkuan
 * Date:     2019-12-5 10:07
 * Description:
 */

package com.ggrpc.remoting;

import com.ggrpc.remoting.model.RemotingResponse;

public interface InvokeCallback {
    void operationComplete(final RemotingResponse remotingResponse);
}
