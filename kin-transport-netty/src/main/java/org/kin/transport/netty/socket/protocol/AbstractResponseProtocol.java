package org.kin.transport.netty.socket.protocol;

import org.kin.transport.netty.socket.protocol.domain.Request;

/**
 * out抽象
 *
 * @author huangjianqin
 * @date 2019/5/30
 */
public abstract class AbstractResponseProtocol extends AbstractProtocol {
    public AbstractResponseProtocol() {
    }

    public AbstractResponseProtocol(int protocolId) {
        super(protocolId);
    }

    @Override
    public final void read(Request request) {
        //do nothing
    }
}