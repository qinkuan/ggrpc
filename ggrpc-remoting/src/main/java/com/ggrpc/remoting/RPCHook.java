/**
 * FileName: RPCHook
 * Author:   yangqinkuan
 * Date:     2019-12-5 9:15
 * Description:
 */

package com.ggrpc.remoting;

import com.ggrpc.remoting.model.RemotingTransporter;

public interface RPCHook {
    void doBeforeRequest(final String remoteAddr, final RemotingTransporter request);

    void doAfterResponse(final String remoteAddr, final RemotingTransporter request,final RemotingTransporter response);
}
