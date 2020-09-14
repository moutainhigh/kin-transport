package org.kin.transport.netty.http.server;

import com.google.common.util.concurrent.RateLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.kin.framework.log.LoggerOprs;
import org.kin.transport.netty.AbstractTransportProtocolTransfer;
import org.kin.transport.netty.http.client.HttpHeaders;
import org.kin.transport.netty.http.client.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;

/**
 * @author huangjianqin
 * @date 2020/8/31
 */
public class HttpServerbinaryTransfer
        extends AbstractTransportProtocolTransfer<FullHttpRequest, ServletTransportEntity, FullHttpResponse>
        implements LoggerOprs {
    /** 限流 */
    private final RateLimiter globalRateLimiter;
    /** cookie 解码 */
    private final ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;
    /** cookie 编码 */
    private final ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;


    public HttpServerbinaryTransfer(boolean compression, int globalRateLimit) {
        super(compression);
        if (globalRateLimit > 0) {
            globalRateLimiter = RateLimiter.create(globalRateLimit);
        } else {
            globalRateLimiter = null;
        }
    }

    @Override
    public Collection<ServletTransportEntity> decode(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<String, String> entry : request.headers()) {
            headers.add(entry.getKey(), entry.getValue());
        }

        String contentType = headers.header(CONTENT_TYPE.toString());
        String cookieStr = headers.header(COOKIE.toString());

        List<Cookie> cookies = cookieDecoder.decode(cookieStr).stream().map(Cookie::of).collect(Collectors.toList());

        ServletRequest servletRequest = new ServletRequest(
                HttpUrl.of(request.uri(), request.protocolVersion()),
                request.method(),
                headers,
                cookies,
                HttpRequestBody.of(request.content(), MediaTypeWrapper.parse(contentType)),
                HttpUtil.isKeepAlive(request)
        );

        return Collections.singleton(servletRequest);
    }

    @Override
    public Collection<FullHttpResponse> encode(ChannelHandlerContext ctx, ServletTransportEntity servletTransportEntity) throws Exception {
        if (!(servletTransportEntity instanceof ServletResponse)) {
            return Collections.emptyList();
        }
        ServletResponse servletResponse = (ServletResponse) servletTransportEntity;
        HttpUrl httpUrl = servletResponse.getUrl();
        boolean keepAlive = servletResponse.isKeepAlive();

        ByteBuf byteBuf = ctx.alloc().buffer();
        HttpResponseBody responseBody = servletResponse.getResponseBody();
        byteBuf.writeBytes(responseBody.bytes());

        HttpVersion httpVersion = httpUrl.version();
        FullHttpResponse response = new DefaultFullHttpResponse(httpVersion, HttpResponseStatus.valueOf(servletResponse.getStatusCode()),
                byteBuf);

        for (Map.Entry<String, String> entry : response.headers()) {
            response.headers().set(entry.getKey(), entry.getValue());
        }

        response.headers()
                .set(CONTENT_TYPE, responseBody.mediaType().toContentType())
                .setInt(CONTENT_LENGTH, byteBuf.readableBytes())
                .set(COOKIE, cookieEncoder.encode(servletResponse.getCookies().stream().map(Cookie::toNettyCookie).collect(Collectors.toList())));

        if (keepAlive) {
            if (!httpVersion.isKeepAliveDefault()) {
                response.headers().set(CONNECTION, CONNECTION);
            }
        } else {
            // 通知client关闭连接
            response.headers().set(CONNECTION, CLOSE);
        }

        ChannelFuture f = ctx.write(response);
        if (!keepAlive) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
        return Collections.emptyList();
    }

    @Override
    public Class<FullHttpRequest> getInClass() {
        return FullHttpRequest.class;
    }

    @Override
    public Class<ServletTransportEntity> getMsgClass() {
        return ServletTransportEntity.class;
    }
}
