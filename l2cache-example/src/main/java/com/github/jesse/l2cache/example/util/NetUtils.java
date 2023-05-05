package com.github.jesse.l2cache.example.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author chenck
 * @date 2023/5/5 15:40
 */
public class NetUtils {
    private static final Log logger = LogFactory.getLog(NetUtils.class);

    private static String local_ip = null;

    /**
     * 获取本机IP
     *
     * @return
     */
    public static String getLocalHostIP() {
        if (local_ip == null) {
            local_ip = getLocalNetIP();
        }
        return local_ip;
    }

    /**
     * 获取本机网口一个IP
     *
     * @return
     */
    public static String getLocalNetIP() {
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            InetAddress ip = null;
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("get local net ip fail", e);
        }
        return "";
    }
}

