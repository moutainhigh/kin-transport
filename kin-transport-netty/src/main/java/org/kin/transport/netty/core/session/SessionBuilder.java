package org.kin.transport.netty.core.session;

import io.netty.channel.Channel;

/**
 *
 * @author huangjianqin
 * @date 2019/5/30
 */
@FunctionalInterface
public interface SessionBuilder<S extends AbstractSession> {
    /**
     * 在channel线程调用
     * @param channel 连接channel
     * @return 绑定该channel的seesion实现
     */
    S create(Channel channel);
}