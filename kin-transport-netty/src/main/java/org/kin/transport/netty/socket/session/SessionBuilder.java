package org.kin.transport.netty.socket.session;

import io.netty.channel.Channel;

/**
 * session builder
 * 定义构建session实例逻辑
 *
 * @author huangjianqin
 * @date 2019/5/30
 */
@FunctionalInterface
public interface SessionBuilder<S extends AbstractSession> {
    /**
     * session构建逻辑
     * 在channel线程调用
     *
     * @param channel 连接的channel
     * @return 绑定该channel的seesion实现
     */
    S create(Channel channel);
}
