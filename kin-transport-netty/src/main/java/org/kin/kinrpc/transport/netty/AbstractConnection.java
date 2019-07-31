package org.kin.kinrpc.transport.netty;

import java.net.InetSocketAddress;

/**
 * Created by 健勤 on 2017/2/10.
 */
public abstract class AbstractConnection {
    protected final InetSocketAddress address;

    public AbstractConnection(InetSocketAddress address) {
        this.address = address;
    }

    public abstract void connect(TransportOption transportOption);

    public abstract void bind(TransportOption transportOption) throws Exception;

    public abstract void close();

    public String getAddress() {
        return address.getHostName() + ":" + address.getPort();
    }

    public abstract boolean isActive();
}
