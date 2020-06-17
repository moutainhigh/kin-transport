package org.kin.transport.netty.core.protocol;

import org.kin.transport.netty.core.protocol.domain.Request;

/**
 * @author huangjianqin
 * @date 2019/7/4
 */
public class DefaultProtocolTransfer implements Bytes2ProtocolTransfer {
    private static final DefaultProtocolTransfer INSTANCE = new DefaultProtocolTransfer();

    private DefaultProtocolTransfer() {
    }

    public static DefaultProtocolTransfer instance() {
        return INSTANCE;
    }

    @Override
    public <T extends AbstractProtocol> T transfer(Request request) {
        AbstractProtocol protocol = ProtocolFactory.createProtocol(request.getProtocolId());
        protocol.read(request);
        return (T) protocol;
    }
}