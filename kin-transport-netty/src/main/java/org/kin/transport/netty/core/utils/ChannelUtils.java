package org.kin.transport.netty.core.utils;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 *
 * @author huangjianqin
 * @date 2019/6/3
 */
public class ChannelUtils {
    private ChannelUtils() {

    }

    public static String getIP(Channel channel) {
        return ((InetSocketAddress) channel.remoteAddress()).getAddress().toString().substring(1);
    }

    public static long ipHashCode(String ip) {
        String[] splits = ip.split("/");
        if (splits.length > 1) {
            ip = splits[splits.length - 1];
        }
        splits = ip.split("\\.");
        long hashcode = 0L;
        int offset = 24;
        for (String item : splits) {
            hashcode += Long.parseLong(item) << offset;
            offset -= 8;
        }
        return hashcode;
    }

    public static long ipHashCode(String ip, int port) {
        return ipHashCode(ip) + port;
    }
}
