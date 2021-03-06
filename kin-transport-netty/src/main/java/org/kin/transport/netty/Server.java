package org.kin.transport.netty;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * server
 * 阻塞绑定端口
 *
 * @author huangjianqin
 * @date 2019/5/30
 */
public class Server extends ServerConnection {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    /** selector线程池 */
    private NioEventLoopGroup bossGroup;
    /** worker线程池 */
    private NioEventLoopGroup workerGroup;
    /** selector */
    private volatile Channel selector;

    public Server(AbstractTransportOption transportOption, ChannelHandlerInitializer channelHandlerInitializer) {
        super(transportOption, channelHandlerInitializer);
    }


    @Override
    public void bind(InetSocketAddress address) {
        if (isActive()) {
            return;
        }
        log.info("server({}) connection binding...", address);

        Map<ChannelOption, Object> serverOptions = transportOption.getServerOptions();
        Map<ChannelOption, Object> channelOptions = transportOption.getChannelOptions();

        //校验
        Preconditions.checkArgument(serverOptions != null);
        Preconditions.checkArgument(channelOptions != null);
        Preconditions.checkArgument(channelHandlerInitializer != null);

        //一半的CPU用于selector
        this.bossGroup = new NioEventLoopGroup(SysUtils.CPU_NUM / 2 + 1);
        //默认2倍cpu
        this.workerGroup = new NioEventLoopGroup();

        CountDownLatch latch = new CountDownLatch(1);

        //配置bootstrap
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup).channel(NioServerSocketChannel.class);

        for (Map.Entry<ChannelOption, Object> entry : serverOptions.entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<ChannelOption, Object> entry : channelOptions.entrySet()) {
            bootstrap.childOption(entry.getKey(), entry.getValue());
        }

        final SslContext sslCtx;
        if (transportOption.isSsl()) {
            try {
                sslCtx = SslContextBuilder.forServer(transportOption.getCertFile(), transportOption.getCertKeyFile()).build();
            } catch (SSLException e) {
                throw new IllegalStateException(e);
            }
        } else {
            sslCtx = null;
        }

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline pipeline = socketChannel.pipeline();
                if (Objects.nonNull(sslCtx)) {
                    pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));
                }

                for (ChannelHandler channelHandler : channelHandlerInitializer.getChannelHandlers()) {
                    pipeline.addLast(channelHandler.getClass().getSimpleName(), channelHandler);
                }
            }
        });

        //绑定
        ChannelFuture cf = bootstrap.bind(address);
        cf.addListener((ChannelFuture channelFuture) -> {
            if (channelFuture.isSuccess()) {
                log.info("server connection binded: {}", address);
                selector = channelFuture.channel();
            }
            latch.countDown();
        });

        long connectTimeout = transportOption.getConnectTimeout();
        try {
            if (connectTimeout > 0) {
                boolean success = latch.await(connectTimeout, TimeUnit.MILLISECONDS);
                if (!success) {
                    throw new ServerBindTimeoutException(address.toString());
                }
            } else {
                latch.await();
            }
        } catch (InterruptedException e) {

        }
        if (selector == null) {
            throw new RuntimeException("server connection bind fail: " + address);
        }
    }

    @Override
    public void close() {
        if (this.selector == null || this.workerGroup == null || this.bossGroup == null) {
            return;
        }

        String addressStr = getAddress();

        this.selector.close();
        this.workerGroup.shutdownGracefully();
        this.bossGroup.shutdownGracefully();

        //help gc
        this.selector = null;
        this.workerGroup = null;
        this.bossGroup = null;

        log.info("server connection({}) closed", addressStr);
    }

    @Override
    public String getAddress() {
        InetSocketAddress address = (InetSocketAddress) selector.localAddress();
        return address.getHostName() + ":" + address.getPort();
    }

    @Override
    public boolean isActive() {
        return Objects.nonNull(selector) && selector.isActive();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Server that = (Server) o;

        return Objects.equals(selector, that.selector);
    }

    @Override
    public int hashCode() {
        return selector != null ? selector.hashCode() : 0;
    }
}
