package org.kin.transport.netty.http.client;

import com.google.common.base.Preconditions;
import com.jakewharton.disklrucache.DiskLruCache;
import io.netty.channel.ChannelOption;
import org.kin.framework.Closeable;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 对外的http client api
 *
 * @author huangjianqin
 * @date 2020/9/4
 */
public final class KinHttpClient implements Closeable {
    /** http client池 */
    private final HttpClientPool pool;
    /** interceptors */
    private List<Interceptor> interceptors = new ArrayList<>();
    /** 请求重试次数, 默认不断重试 */
    private int retryTimes = -1;
    /** 连接超时,毫秒 */
    private long connectTimeout;
    /** http call超时,毫秒 */
    private long callTimeout;
    /** 读超时,毫秒 */
    private long readTimeout;
    /** 写超时,毫秒 */
    private long writeTimeout;
    /** 缓存目录 */
    private DiskLruCache cache;

    private KinHttpClient() {
        HttpClientTransportOption transportOption = HttpClientTransportOption.builder()
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .channelOption(ChannelOption.SO_KEEPALIVE, true)
                .protocolHandler(HttpClient.HttpClientProtocolHandler.INSTANCE)
                .build();
        pool = new HttpClientPool(transportOption);
    }

    //--------------------------------------------------------------------------------------------------------------

    /**
     * 构建http call
     */
    public HttpCall newCall(HttpRequest request) {
        return new HttpCall(this, request);
    }

    //--------------------------------------------------------------------------------------------------------------

    /**
     * 从http client池获取client
     */
    HttpClient client(InetSocketAddress address) {
        return pool.client(address);
    }

    /**
     * 将client归还到http client池
     */
    void clientBack(InetSocketAddress address, HttpClient client) {
        pool.clientBack(address, client);
    }

    @Override
    public void close() {
        if (Objects.nonNull(cache)) {
            try {
                cache.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getCallTimeout() {
        return callTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public DiskLruCache getCache() {
        return cache;
    }

    //------------------------------------------------------builder------------------------------------------------------
    public static KinHttpClientBuilder builder() {
        return new KinHttpClientBuilder();
    }

    public static class KinHttpClientBuilder {
        /** target */
        private KinHttpClient kinHttpClient = new KinHttpClient();
        private volatile boolean exported;

        public KinHttpClient build() {
            exported = true;
            return kinHttpClient;
        }

        public KinHttpClientBuilder retryTimes(int retryTimes) {
            if (!exported) {
                kinHttpClient.retryTimes = retryTimes;
            }
            return this;
        }

        public KinHttpClientBuilder connectTimeout(long connectTimeout, TimeUnit unit) {
            if (!exported) {
                kinHttpClient.connectTimeout = unit.toMillis(connectTimeout);
            }
            return this;
        }

        public KinHttpClientBuilder callTimeout(long callTimeout, TimeUnit unit) {
            if (!exported) {
                kinHttpClient.callTimeout = unit.toMillis(callTimeout);
            }
            return this;
        }

        public KinHttpClientBuilder readTimeout(long readTimeout, TimeUnit unit) {
            if (!exported) {
                kinHttpClient.readTimeout = unit.toMillis(readTimeout);
            }
            return this;
        }

        public KinHttpClientBuilder writeTimeout(long writeTimeout, TimeUnit unit) {
            if (!exported) {
                kinHttpClient.writeTimeout = unit.toMillis(writeTimeout);
            }
            return this;
        }

        public KinHttpClientBuilder addInterceptor(Interceptor interceptor) {
            if (!exported) {
                kinHttpClient.interceptors.add(interceptor);
            }
            return this;
        }

        public KinHttpClientBuilder addInterceptors(Collection<Interceptor> interceptors) {
            if (!exported) {
                kinHttpClient.interceptors.addAll(interceptors);
            }
            return this;
        }

        public KinHttpClientBuilder removeInterceptor(Interceptor interceptor) {
            if (!exported) {
                kinHttpClient.interceptors.remove(interceptor);
            }
            return this;
        }

        public KinHttpClientBuilder removeInterceptors(Collection<Interceptor> interceptors) {
            if (!exported) {
                kinHttpClient.interceptors.removeAll(interceptors);
            }
            return this;
        }

        public KinHttpClientBuilder cacheDir(String dirPath, int maxSize) {
            return cacheDir(new File(dirPath), maxSize);
        }

        public KinHttpClientBuilder cacheDir(File cacheDir, int maxSize) {
            if (!exported) {
                Preconditions.checkArgument(Objects.isNull(kinHttpClient.cache), "cache has been set");
                Preconditions.checkArgument(Objects.nonNull(cacheDir) && cacheDir.isDirectory(),
                        "cache dir path must be a directory");
                //1个app版本
                //1个key对应1个value
                try {
                    kinHttpClient.cache = DiskLruCache.open(cacheDir, 1, 1, maxSize);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return this;
        }
    }
}
