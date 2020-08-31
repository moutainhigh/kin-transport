package org.kin.transport.netty.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import org.kin.framework.log.LoggerOprs;
import org.kin.transport.netty.AbstractTransportProtocolTransfer;
import org.kin.transport.netty.http.server.session.HttpSession;
import org.kin.transport.netty.socket.SocketTransportProtocolTransfer;
import org.kin.transport.netty.socket.protocol.AbstractSocketProtocol;
import org.kin.transport.netty.socket.protocol.SocketByteBufResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * 基于{@link SocketTransportProtocolTransfer}
 *
 * @author huangjianqin
 * @date 2020/8/31
 */
public class HttpServerTransportProtocolTransfer
        extends AbstractTransportProtocolTransfer<FullHttpRequest, AbstractSocketProtocol, FullHttpResponse>
        implements LoggerOprs {
    private final SocketTransportProtocolTransfer transfer;

    public HttpServerTransportProtocolTransfer(boolean compression) {
        super(compression);
        this.transfer = new SocketTransportProtocolTransfer(compression, true);
    }

    @Override
    public Collection<AbstractSocketProtocol> decode(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Channel channel = ctx.channel();
        HttpSession.put(channel, request);

        return transfer.decode(ctx, request.content());
    }

    @Override
    public Collection<FullHttpResponse> encode(ChannelHandlerContext ctx, AbstractSocketProtocol protocol) throws Exception {
        SocketByteBufResponse byteBufResponse = protocol.write();
        ByteBuf byteBuf = byteBufResponse.getByteBuf();

        Channel channel = ctx.channel();
        HttpSession httpSession = HttpSession.remove(channel);
        if (Objects.isNull(httpSession)) {
            log().error("no http session >>> {}", channel);
            return Collections.emptyList();
        }

        FullHttpRequest request = httpSession.getRequest();
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), OK,
                byteBuf);
        response.headers()
                .set(CONTENT_TYPE, TEXT_PLAIN)
                .setInt(CONTENT_LENGTH, byteBuf.readableBytes());

        if (keepAlive) {
            if (!request.protocolVersion().isKeepAliveDefault()) {
                response.headers().set(CONNECTION, CONNECTION);
            }
        } else {
            // 通知client关闭连接
            response.headers().set(CONNECTION, CLOSE);
        }

        ChannelFuture f = ctx.write(response);

        if (!keepAlive) {
            //TODO
            f.addListener(ChannelFutureListener.CLOSE);
        }
        return null;
    }

    @Override
    public Class<FullHttpRequest> getInClass() {
        return FullHttpRequest.class;
    }

    @Override
    public Class<AbstractSocketProtocol> getMsgClass() {
        return AbstractSocketProtocol.class;
    }
}