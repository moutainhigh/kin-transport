package org.kin.transport.netty.udp;

import io.netty.channel.ChannelHandlerContext;
import org.kin.transport.netty.ProtocolHandler;
import org.kin.transport.netty.Transports;
import org.kin.transport.netty.UdpClient;
import org.kin.transport.netty.UdpServer;
import org.kin.transport.netty.socket.protocol.Protocol1;
import org.kin.transport.netty.socket.protocol.ProtocolFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/9/1
 */
public class UdpTest {
    public static void main(String[] args) throws InterruptedException {
        ProtocolFactory.init("org.kin.transport");

        UdpServer server = null;
        UdpClient client = null;
        try {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9000);
            server = Transports.datagram().server().protocolHandler(new ProtocolHandler<UdpProtocolWrapper>() {
                @Override
                public void handle(ChannelHandlerContext ctx, UdpProtocolWrapper protocol) {
                    System.out.println(protocol.getProtocol());
                    ctx.channel().writeAndFlush(UdpProtocolWrapper.senderWrapper(Protocol1.of(2), protocol.getSenderAddress()));
                }
            }).build(address);

            client = Transports.datagram().client().protocolHandler(new ProtocolHandler<UdpProtocolWrapper>() {
                @Override
                public void handle(ChannelHandlerContext ctx, UdpProtocolWrapper protocol) {
                    System.out.println(protocol.getProtocol());
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
