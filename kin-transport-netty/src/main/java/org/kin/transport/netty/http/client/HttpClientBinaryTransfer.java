package org.kin.transport.netty.http.client;

import com.google.common.util.concurrent.RateLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.kin.framework.log.LoggerOprs;
import org.kin.transport.netty.AbstractTransportProtocolTransfer;
import org.kin.transport.netty.socket.SocketTransfer;
import org.kin.transport.netty.utils.ChannelUtils;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 基于{@link SocketTransfer}
 *
 * @author huangjianqin
 * @date 2020/8/31
 */
public class HttpClientBinaryTransfer extends AbstractTransportProtocolTransfer<FullHttpResponse, HttpEntity, FullHttpRequest>
        implements LoggerOprs {
    /** 限流 */
    private final RateLimiter globalRateLimiter;

    public HttpClientBinaryTransfer(boolean compression, int globalRateLimit) {
        super(compression);
        if (globalRateLimit > 0) {
            globalRateLimiter = RateLimiter.create(globalRateLimit);
        } else {
            globalRateLimiter = null;
        }
    }

    @Override
    public Collection<HttpEntity> decode(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        if (ChannelUtils.globalRateLimit(ctx, globalRateLimiter)) {
            return Collections.emptyList();
        }

        /**
         * 将 {@link FullHttpResponse} 转换成 {@link HttpResponse}
         */
        HttpResponseStatus responseStatus = response.status();
        int code = responseStatus.code();
        String message = responseStatus.reasonPhrase();
        String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
        HttpResponseBody responseBody = HttpResponseBody.of(response.content(), MediaTypeWrapper.parse(contentType));

        HttpResponse httpResponse = HttpResponse.of(responseBody, message, code);
        for (Map.Entry<String, String> entry : response.headers().entries()) {
            httpResponse.headers().put(entry.getKey(), entry.getValue());
        }

        return Collections.singleton(httpResponse);
    }

    @Override
    public Collection<FullHttpRequest> encode(ChannelHandlerContext ctx, HttpEntity httpEntity) throws Exception {
        if (!(httpEntity instanceof HttpRequest)) {
            return Collections.emptyList();
        }

        HttpRequest httpRequest = (HttpRequest) httpEntity;
        HttpRequestBody requestBody = httpRequest.requestBody();
        ByteBuffer byteBuffer = requestBody.sink();
        ByteBuf content = ctx.alloc().buffer(byteBuffer.capacity());
        content.writeBytes(byteBuffer);

        //配置HttpRequest的请求数据和一些配置信息
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_0,
                httpRequest.method(),
                httpRequest.url().uri().toASCIIString(),
                content);

        for (Map.Entry<String, String> entry : httpRequest.headers().entrySet()) {
            request.headers().set(entry.getKey(), entry.getValue());
        }

        //设置content type
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, requestBody.mediaType().toContentType());

        return Collections.singletonList(request);
    }

    @Override
    public Class<FullHttpResponse> getInClass() {
        return FullHttpResponse.class;
    }

    @Override
    public Class<HttpEntity> getMsgClass() {
        return HttpEntity.class;
    }

}
