package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.util.CacheValueHashUtil;

public class CacheValueHashUtilTest {

    public static void main(String[] args) {
        UserDTO userDTO1 = new UserDTO("张三", "账号123123333333333333333333333你到时候来啊合理吧啊啊啊的发的的啊亲亲份额诉讼费切割机哦i哦及登记卡倒垃圾合肥");
        UserDTO userDTO2 = new UserDTO("张三", "账号123123333333333333333333333你到时候来啊合理吧啊啊啊的发的的啊亲亲份额诉讼费切割机哦i哦及登记卡倒垃圾合肥");

        String hash1 = CacheValueHashUtil.calcHash(userDTO1);
        String hash2 = CacheValueHashUtil.calcHash(userDTO2);
        System.out.println(hash1);
        System.out.println(hash1);

        if (hash1.equals(hash2)) {
            System.out.println("hash 一致");
        } else {
            System.out.println("hash 不一致");
        }
    }
}
