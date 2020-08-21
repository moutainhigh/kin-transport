package org.kin.transport.netty.core;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.kin.framework.utils.CollectionUtils;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * client
 * 阻塞连接远程服务器
 *
 * @author huangjianqin
 * @date 2019/5/30
 */
public class Client extends ClientConnection {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private volatile Channel channel;
    private volatile boolean isStopped;

    public Client(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void connect(ClientTransportOption transportOption, ChannelHandlerInitializer channelHandlerInitializer) {
        log.info("client({}) connecting...", address);

        Map<ChannelOption, Object> channelOptions = transportOption.getChannelOptions();

        Preconditions.checkArgument(channelOptions != null);
        Preconditions.checkArgument(channelHandlerInitializer != null);

        CountDownLatch latch = new CountDownLatch(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);

        for (Map.Entry<ChannelOption, Object> entry : channelOptions.entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        final SslContext sslCtx;
        if (transportOption.isSsl()) {
            try {
                sslCtx = SslContextBuilder.forClient().keyManager(transportOption.getCertFile(), transportOption.getCertKeyFile()).build();
            } catch (SSLException e) {
                throw new IllegalStateException(e);
            }
        } else {
            sslCtx = null;
        }

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline pipeline = socketChannel.pipeline();
                if (Objects.nonNull(sslCtx)) {
                    pipeline.addLast(sslCtx.newHandler(socketChannel.alloc(), address.getHostString(), address.getPort()));
                }

                for (ChannelHandler channelHandler : channelHandlerInitializer.getChannelHandlers()) {
                    pipeline.addLast(channelHandler.getClass().getSimpleName(), channelHandler);
                }
            }
        });
        ChannelFuture cf = bootstrap.connect(address);
        cf.addListener((ChannelFuture channelFuture) -> {
            if (channelFuture.isSuccess()) {
                log.info("connect to remote server success: {}", address);
                channel = channelFuture.channel();
                latch.countDown();
            } else {
                log.error("connect to remote server fail: {}", address);
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {

        }
    }

    @Override
    public void close() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        if (channel != null) {
            channel.close();
        }
        eventLoopGroup.shutdownGracefully();
        log.info("client({}) closed", address);
    }

    @Override
    public boolean isActive() {
        return !isStopped && channel != null && channel.isActive();
    }

    public void request(AbstractProtocol protocol) {
        request(protocol);
    }

    public void request(AbstractProtocol protocol, ChannelFutureListener... listeners) {
        if (isActive() && Objects.nonNull(protocol)) {
            ChannelFuture channelFuture = channel.writeAndFlush(protocol.write());
            if (CollectionUtils.isNonEmpty(listeners)) {
                channelFuture.addListeners(listeners);
            }
        }
    }

    public String getLocalAddress() {
        if (channel != null) {
            return channel.localAddress().toString();
        }
        return "";
    }

    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Client that = (Client) o;

        return Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return channel != null ? channel.hashCode() : 0;
    }
}
