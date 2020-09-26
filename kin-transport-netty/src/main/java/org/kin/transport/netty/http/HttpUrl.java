package org.kin.transport.netty.http;

import io.netty.handler.codec.http.HttpVersion;

import java.net.*;

/**
 * url封装
 *
 * @author huangjianqin
 * @date 2020/9/3
 */
public final class HttpUrl {
    /** url */
    private String url;
    /** http协议版本 */
    private HttpVersion version;

    private HttpUrl() {
    }

    //-------------------------------------------------------------------------------------------------------------
    public static HttpUrl of(String url) {
        HttpUrl httpUrl = new HttpUrl();
        httpUrl.url = url;
        return httpUrl;
    }

    public static HttpUrl of(String url, HttpVersion version) {
        HttpUrl httpUrl = new HttpUrl();
        httpUrl.url = url;
        httpUrl.version = version;
        return httpUrl;
    }

    //-------------------------------------------------------------------------------------------------------------

    public String rawUrl() {
        return url;
    }

    /**
     * 转换成{@link URL}
     */
    public URL url() {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 转换成{@link URI}
     */
    public URI uri() {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 转换成{@link InetSocketAddress}
     */
    public InetSocketAddress address() {
        URI uri = uri();
        return new InetSocketAddress(uri.getHost(), uri.getPort());
    }

    public HttpVersion getVersion() {
        return version;
    }
}