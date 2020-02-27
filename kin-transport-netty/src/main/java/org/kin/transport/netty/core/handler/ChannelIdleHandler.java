package org.kin.transport.netty.core.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.kin.transport.netty.core.listener.ChannelIdleListener;

/**
 *
 * @author huangjianqin
 * @date 2019/6/3
 */
public class ChannelIdleHandler extends ChannelDuplexHandler {
    private ChannelIdleListener channelIdleListener;

    public ChannelIdleHandler() {
    }

    public ChannelIdleHandler(ChannelIdleListener channelIdleListener) {
        this.channelIdleListener = channelIdleListener;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                if (channelIdleListener != null) {
                    channelIdleListener.readIdle(ctx.channel());
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                if (channelIdleListener != null) {
                    channelIdleListener.writeIdel(ctx.channel());
                }
            } else {
                //All IDLE
                if (channelIdleListener != null) {
                    channelIdleListener.allIdle(ctx.channel());
                }
            }
        }
    }
}
