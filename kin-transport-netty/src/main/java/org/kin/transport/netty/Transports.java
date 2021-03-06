package org.kin.transport.netty;

import org.kin.transport.netty.socket.SocketTransports;
import org.kin.transport.netty.udp.UdpTransports;
import org.kin.transport.netty.websocket.WebsocketTransports;

/**
 * net transports
 *
 * @author huangjianqin
 * @date 2020/8/27
 */
public class Transports {
    /**
     * socket transport 配置
     */
    public static SocketTransports socket() {
        return SocketTransports.INSTANCE;
    }

    /**
     * udp transport 配置
     */
    public static UdpTransports datagram() {
        return UdpTransports.INSTANCE;
    }

    /**
     * websocket transport 配置
     */
    public static WebsocketTransports websocket() {
        return WebsocketTransports.INSTANCE;
    }
}
