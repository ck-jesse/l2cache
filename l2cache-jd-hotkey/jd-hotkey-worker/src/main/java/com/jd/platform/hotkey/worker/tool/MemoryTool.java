package com.jd.platform.hotkey.worker.tool;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import sun.misc.JavaNioAccess;
import sun.misc.SharedSecrets;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-08
 */
public class MemoryTool {
    public static BufferPoolMXBean getDirectBufferPoolMBean(){
        List<BufferPoolMXBean> list = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        if (list.size() > 0) {
            for (BufferPoolMXBean bufferPoolMXBean:list) {
                String name = bufferPoolMXBean.getName();
                if (("direct").equals(name)) {
                    return bufferPoolMXBean;
                }
            }
        }
        return null;

    }

    public static double getBufferPool() {
        BufferPoolMXBean bufferPoolMXBean = getDirectBufferPoolMBean();
        if (bufferPoolMXBean != null) {
            return bufferPoolMXBean.getTotalCapacity() / 1024.0 / 1024.0;
        }
        return getNioBufferPool().getTotalCapacity() / 1024.0 / 1024.0;
    }

    public static JavaNioAccess.BufferPool getNioBufferPool(){
        return SharedSecrets.getJavaNioAccess().getDirectBufferPool();
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {

            map.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        }
        double m = ObjectSizeCalculator.getObjectSize(map);
        System.out.println(m);
    }
}
