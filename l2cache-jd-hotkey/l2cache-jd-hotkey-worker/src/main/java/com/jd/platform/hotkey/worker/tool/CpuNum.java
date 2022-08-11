package com.jd.platform.hotkey.worker.tool;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-12
 */
public class CpuNum {

    /**
     * netty worker线程数量. cpu密集型
     */
    public static int workerCount() {
        //取cpu核数，新版jdk在docker里取的就是真实分配的，老版jdk取的是宿主机的，可能特别大，如32核
        int count = Runtime.getRuntime().availableProcessors();
        if (isNewerVersion()) {
            return count;
        } else {
            count = count / 2;
            if (count == 0) {
                count = 1;
            }
        }
        return count;
    }

    public static void main(String[] args) {
//        System.out.println(isNewerVersion());
        System.out.println(Runtime.getRuntime().availableProcessors());
    }


    private static boolean isNewerVersion() {
        try {
            //如1.8.0_20, 1.8.0_181,1.8.0_191-b12
            String javaVersion = System.getProperty("java.version");
            //1.8.0_191之前的java版本，在docker内获取availableProcessors的数量都不对，会取到宿主机的cpu数量，譬如宿主机32核，
            //该docker只分配了4核，那么老版会取到32，新版会取到4。
            //线上事故警告！！！！！！老版jdk取到数值过大，线程数太多，导致cpu瞬间100%，大量的线程切换等待
            //先取前三位进行比较

            String topThree = javaVersion.substring(0, 5);
            if (topThree.compareTo("1.8.0") > 0) {
                return true;
            } else if (topThree.compareTo("1.8.0") < 0) {
                return false;
            } else {
                //前三位相等，比小版本. 下面代码可能得到20，131，181，191-b12这种
                String smallVersion = javaVersion.replace("1.8.0_", "");
                //继续截取，找"-"这个字符串，把后面的全截掉
                if (smallVersion.contains("-")) {
                    smallVersion = smallVersion.substring(0, smallVersion.indexOf("-"));
                }

                return Integer.valueOf(smallVersion) >= 191;
            }
        } catch (Exception e) {
            return false;
        }


    }

}
