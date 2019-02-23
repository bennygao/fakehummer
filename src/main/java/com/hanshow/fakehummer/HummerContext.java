package com.hanshow.fakehummer;

import lombok.Getter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.net.InetAddress;
import java.util.Date;

@Getter
public class HummerContext {
    static class SingletonHolder {
        static HummerContext singleton = new HummerContext();
    }

    public static HummerContext getInstance() {
        return SingletonHolder.singleton;
    }

    private Date startTime;
    private InetAddress address;

    private HummerContext() {
        this.startTime = new Date();

        try {
            this.address = InetAddress.getLocalHost();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public Jedis getJedis() {
        HostAndPort redisServer = HostAndPort.parseString(System.getenv("REDIS_SERVER"));
        return new Jedis(redisServer.getHost(), redisServer.getPort());
    }
}
