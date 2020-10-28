package org.kin.transport.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.log.LoggerOprs;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 支持重连的{@link Client}
 * <p>
 * 需重写{@link Client}所有方法
 *
 * @author huangjianqin
 * @date 2020/10/28
 */
public class ReconnectClient<MSG> extends Client<MSG> implements LoggerOprs {
    /** 重连线程池 */
    public static final ExecutionContext RECONNECT_EXECUTORS =
            ExecutionContext.cache("client-reconnect",
                    2, "client-reconnect-scheduler");

    static {
        JvmCloseCleaner.DEFAULT().add(RECONNECT_EXECUTORS::shutdownNow);
    }

    /** 重连的TransportOption */
    private final ReconnectTransportOption<MSG> reconnectTransportOption;
    /** 包装的client */
    private volatile Client<MSG> client;
    /** 上次connect的address */
    private volatile InetSocketAddress address;
    /** 支持重连的ProtocolHandler */
    private volatile ProtocolHandler autoReconnectProtocolHandler;
    /** 重连future */
    private volatile Future<?> reconnectFuture;
    /** 重连成功后补发, 断开链接时发的消息, 最好缓存200个消息, 超过了size但还未重连成功直接报错 */
    private LinkedBlockingQueue<MSG> queue = new LinkedBlockingQueue<>(500);

    public ReconnectClient(AbstractTransportOption<?, ?, ?, ?> transportOption, ReconnectTransportOption<MSG> reconnectTransportOption) {
        super(transportOption, null);
        this.reconnectTransportOption = reconnectTransportOption;
    }

    @Override
    public void connect(InetSocketAddress address) {
        if (Objects.nonNull(client) && isStopped()) {
            return;
        }

        if (Objects.nonNull(client) && isActive()) {
            return;
        }

        //生成实现了自动重连的ProtocolHanler
        wrapReconnectProtocolHanler();
        reconnectTransportOption.wrapProtocolHandler(autoReconnectProtocolHandler);

        this.address = address;
        this.client = reconnectTransportOption.reconnect(address);

        if (!isStopped() && !isActive() && Objects.isNull(reconnectFuture)) {
            //n秒后重连
            reconnectFuture = RECONNECT_EXECUTORS.schedule(this::reconnect, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * 重连
     */
    private void reconnect() {
        if (!isStopped() && isActive()) {
            //连接有效
            return;
        }
        if (Objects.nonNull(reconnectFuture)) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
        //先关闭
        if (Objects.nonNull(client)) {
            client.close();
        }
        client = null;
        log.info("client({}) reconnect....", address);
        //重连
        connect(address);
    }

    /**
     * 包装实现了自动重连的{@link ProtocolHandler}
     */
    private void wrapReconnectProtocolHanler() {
        if (Objects.nonNull(this.autoReconnectProtocolHandler)) {
            return;
        }
        ProtocolHandler protocolHandler = transportOption.getProtocolHandler();
        this.autoReconnectProtocolHandler = new ProtocolHandler() {
            @Override
            public void handle(ChannelHandlerContext ctx, Object protocol) {
                protocolHandler.handle(ctx, protocol);
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                protocolHandler.channelActive(ctx);

                //补发断开链接时发的消息
                List<MSG> pendingMsgs = new ArrayList<>(queue.size());
                queue.drainTo(pendingMsgs);
                for (MSG pendingMsg : pendingMsgs) {
                    request(pendingMsg);
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) {
                if (!isStopped()) {
                    //立即重连
                    RECONNECT_EXECUTORS.execute(() -> reconnect());
                }
                protocolHandler.channelInactive(ctx);
            }

            @Override
            public void handleException(ChannelHandlerContext ctx, Throwable cause) {
                protocolHandler.handleException(ctx, cause);
            }

            @Override
            public void rateLimitReject(ChannelHandlerContext ctx, Object protocol) {
                protocolHandler.rateLimitReject(ctx, protocol);
            }

            @Override
            public void globalRateLimitReject() {
                protocolHandler.globalRateLimitReject();
            }

            @Override
            public void readWriteIdle(ChannelHandlerContext ctx) {
                protocolHandler.readWriteIdle(ctx);
            }

            @Override
            public void readIdle(ChannelHandlerContext ctx) {
                protocolHandler.readIdle(ctx);
            }

            @Override
            public void writeIdel(ChannelHandlerContext ctx) {
                protocolHandler.writeIdel(ctx);
            }
        };
    }

    @Override
    public void close() {
        if (Objects.nonNull(client)) {
            client.close();
            client = null;
        }
    }

    @Override
    public boolean isActive() {
        if (Objects.nonNull(client)) {
            return client.isActive();
        }
        return false;
    }

    @Override
    public String getAddress() {
        if (Objects.nonNull(client)) {
            return client.getAddress();
        }
        return null;
    }

    @Override
    public boolean request(MSG msg) {
        return request(msg, (ChannelFuture channelFuture) -> {
            if (!channelFuture.isSuccess()) {
                //发送失败, 则缓存消息
                queue.add(msg);
            }
        });
    }

    @Override
    public boolean request(MSG msg, ChannelFutureListener... listeners) {
        if (Objects.nonNull(client)) {
            if (!client.request(msg, listeners)) {
                //发送失败, 则缓存消息
                queue.add(msg);
            }
            return true;
        } else {
            //链接还未建立, 则缓存消息
            queue.add(msg);
        }

        return false;
    }

    @Override
    public String getLocalAddress() {
        if (Objects.nonNull(client)) {
            return client.getLocalAddress();
        }
        return null;
    }

    @Override
    public boolean isStopped() {
        if (Objects.nonNull(client)) {
            return client.isStopped();
        }
        return true;
    }
}