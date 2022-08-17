package com.jd.platform.hotkey.dashboard.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author liyunfeng31
 */
public class DateUtil {

    public static final String PATTERN_SECONDS="yyMMddHHmmss";

    public static final String PATTERN_MINUS="yyMMddHHmm";

    public static final String PATTERN_HOUR="yyMMddHH";

    public static final String PATTERN_DAY="yyMMdd";

    private static final DateTimeFormatter FORMAT_SECONDS = DateTimeFormatter.ofPattern(PATTERN_SECONDS);

    private static final DateTimeFormatter FORMAT_MINUS = DateTimeFormatter.ofPattern(PATTERN_MINUS);

    private static final DateTimeFormatter FORMAT_HOUR = DateTimeFormatter.ofPattern(PATTERN_HOUR);

    private static final DateTimeFormatter FORMAT_DAY = DateTimeFormatter.ofPattern(PATTERN_DAY);

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static Date strToDate(String str){
        try {
            return simpleDateFormat.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LocalDateTime strToLdt(String str, String pattern){
        return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
    }


    public static int reviseTime(LocalDateTime time, int diff, int type){
        switch (type){
            case 0:
                return Integer.parseInt(FORMAT_SECONDS.format(time.plusSeconds(diff)));
            case 1:
                return Integer.parseInt(FORMAT_MINUS.format(time.plusMinutes(diff)));
            case 2:
                return Integer.parseInt(FORMAT_HOUR.format(time.plusHours(diff)));
            case 3:
                return Integer.parseInt(FORMAT_DAY.format(time.plusDays(diff)));
            default:
        }
        return 0;
    }




    public static Date ldtToDate(LocalDateTime localDateTime){
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }


    public static LocalDateTime dateToLdt(Date date){
        return LocalDateTime.ofInstant( date.toInstant(), ZoneId.systemDefault());
    }


    public static int nowMinus(LocalDateTime now){
        return Integer.parseInt(now.format(FORMAT_MINUS)) ;
    }

    public static int nowHour(LocalDateTime now){
        return Integer.parseInt(now.format(FORMAT_HOUR));
    }

    public static int nowDay(LocalDateTime now){ return Integer.parseInt(now.format(FORMAT_DAY));}


    public static int preHoursInt(int hours){
        return Integer.parseInt(LocalDateTime.now().minusHours(hours).format(FORMAT_HOUR));
    }

    public static Date preHours(int hours){
        return ldtToDate(LocalDateTime.now().minusHours(hours));
    }

    public static Date preTime(int hours){
       return ldtToDate(LocalDateTime.now().minusHours(hours));
    }


    public static Date preMinus(int minus){
        return ldtToDate(LocalDateTime.now().minusMinutes(minus));
    }

    public static Date preDays(int days){
        return ldtToDate(LocalDateTime.now().minusDays(days));
    }

    public static String formatTime(int time,String pattern){
        return strToLdt(time+"", pattern).toString().replace("T", " ");
    }

}
