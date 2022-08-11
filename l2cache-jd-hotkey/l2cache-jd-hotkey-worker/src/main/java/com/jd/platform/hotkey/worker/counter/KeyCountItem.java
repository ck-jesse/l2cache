package com.jd.platform.hotkey.worker.counter;

import com.jd.platform.hotkey.common.model.KeyCountModel;

import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 将收到的数量统计都放到延迟队列里，10几秒后再处理
 *
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-28
 */
public class KeyCountItem implements Delayed {
    private String appName;
    private long createTime;
    private List<KeyCountModel> list;

    public KeyCountItem(String appName, long createTime, List<KeyCountModel> list) {
        this.appName = appName;
        this.createTime = createTime;
        this.list = list;
    }

    public KeyCountItem() {
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<KeyCountModel> getList() {
        return list;
    }

    public void setList(List<KeyCountModel> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "KeyCountItem{" +
                "appName='" + appName + '\'' +
                ", createTime=" + createTime +
                ", list=" + list +
                '}';
    }

    @Override
    public long getDelay(TimeUnit unit) {
        //固定延迟15秒后再处理
        return createTime - System.currentTimeMillis() + 15000;
    }

    @Override
    public int compareTo(Delayed o) {
        KeyCountItem item = (KeyCountItem) o;
        long diff = this.createTime - item.getCreateTime();
        if (diff <= 0) {// 改成>=会造成问题
            return -1;
        } else {
            return 1;
        }
    }


    public static void main(String[] args) {
        KeyCountItem keyCountItem = new KeyCountItem();
        keyCountItem.setAppName("a");
        keyCountItem.setCreateTime(System.currentTimeMillis() + 2000);
        KeyCountItem keyCountItem1 = new KeyCountItem();
        keyCountItem1.setAppName("b");
        keyCountItem1.setCreateTime(System.currentTimeMillis() + 1000);
        KeyCountItem keyCountItem2 = new KeyCountItem();
        keyCountItem2.setAppName("c");
        keyCountItem2.setCreateTime(System.currentTimeMillis());


        DelayQueue<KeyCountItem> queue = new DelayQueue<>();

        queue.put(keyCountItem);
        queue.put(keyCountItem1);
        queue.put(keyCountItem2);

        for (int i = 0; i < 3; i++) {
            try {
                System.out.println(queue.take());
                System.out.println(System.currentTimeMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
