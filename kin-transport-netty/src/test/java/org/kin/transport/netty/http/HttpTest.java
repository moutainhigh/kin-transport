package org.kin.transport.netty.http;

import io.netty.channel.ChannelHandlerContext;
import org.kin.transport.netty.Client;
import org.kin.transport.netty.Server;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.socket.SocketProtocolHandler;
import org.kin.transport.netty.socket.protocol.Protocol1;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;
import org.kin.transport.netty.socket.protocol.SocketProtocol;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/9/1
 */
public class HttpTest {
    public static void main(String[] args) throws InterruptedException {
        ProtocolFactory.init("org.kin.transport");

        Server server = null;
        Client<SocketProtocol> client = null;
        try {
            InetSocketAddress address = new InetSocketAddress("0.0.0.0", 9000);
            server = Transports.http().server(SocketProtocol.class).protocolHandler(new SocketProtocolHandler() {
                @Override
                public void handle(ChannelHandlerContext ctx, SocketProtocol protocol) {
                    System.out.println(protocol);
                    ctx.channel().writeAndFlush(Protocol1.of(2));
                }
            }).build(address);

            client = Transports.http().client(SocketProtocol.class).protocolHandler(new SocketProtocolHandler() {
                @Override
                public void handle(ChannelHandlerContext ctx, SocketProtocol protocol) {
                    System.out.println(protocol);
                }
            }).build(address);
            client.request(Protocol1.of(1));

            Thread.sleep(5000);
        } finally {
            if (Objects.nonNull(client)) {
                client.close();
            }
            if (Objects.nonNull(server)) {
                server.close();
            }
        }
    }
}