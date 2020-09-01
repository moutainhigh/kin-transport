package org.kin.transport.netty.udp.server;

import io.netty.channel.socket.DatagramPacket;
import org.kin.transport.netty.ChannelHandlerInitializer;
import org.kin.transport.netty.Server;
import org.kin.transport.netty.TransportProtocolTransfer;
import org.kin.transport.netty.udp.AbstractUdpTransportOption;
import org.kin.transport.netty.udp.UdpChannelHandlerInitializer;
import org.kin.transport.netty.udp.UdpProtocolWrapper;
import org.kin.transport.netty.udp.UdpTransportProtocolTransfer;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/9/1
 */
public class UdpServerTransportOption extends AbstractUdpTransportOption<UdpServerTransportOption> {
    public Server udp(InetSocketAddress address) {
        ChannelHandlerInitializer<DatagramPacket, UdpProtocolWrapper, DatagramPacket>
                channelHandlerInitializer = new UdpChannelHandlerInitializer<>(this);
        Server server = new Server(address);
        server.bind(this, channelHandlerInitializer);
        return server;
    }

    //------------------------------------------------------------------------------------------------------------------
    @Override
    public TransportProtocolTransfer<DatagramPacket, UdpProtocolWrapper, DatagramPacket> getTransportProtocolTransfer() {
        return Objects.nonNull(super.getTransportProtocolTransfer()) ?
                super.getTransportProtocolTransfer() : new UdpTransportProtocolTransfer(isCompression(), true);
    }
}