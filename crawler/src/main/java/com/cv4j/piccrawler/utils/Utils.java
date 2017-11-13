package com.cv4j.piccrawler.utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by tony on 2017/10/10.
 */
public class Utils {

    public static final String PIC_REGEX = ".*\\.(jpg|jpeg|gif|bmp|png|webp|svg).*";

    public static final String ESCAPE_REGEX = ".*\\.( |\"|#|(|)|\\+|,|:|;|<|>|@|\\||).*";

    public static HashMap<String,String> map = new HashMap<>();

    static {
        map.put(" ","%20");
        map.put("\"","%22");
        map.put("#","%23");
        map.put("(","%28");
        map.put(")","%29");
        map.put("+","%2B");
        map.put(",","%2C");
        map.put(":","%3A");
        map.put(";","%3B");
        map.put("<","%3C");
        map.put(">","%3E");
        map.put("@","%40");
        map.put("\\","%5C");
        map.put("|","%7C");
    }

    /**
     * 生成随机数<br>
     * GUID： 即Globally Unique Identifier（全球唯一标识符） 也称作 UUID(Universally Unique
     * IDentifier) 。
     * <p>
     * 所以GUID就是UUID。
     * <p>
     * GUID是一个128位长的数字，一般用16进制表示。算法的核心思想是结合机器的网卡、当地时间、一个随即数来生成GUID。
     * <p>
     * 从理论上讲，如果一台机器每秒产生10000000个GUID，则可以保证（概率意义上）3240年不重复。
     *
     * @return
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 创建默认的文件夹用于存放图片
     * @param directory
     */
    public static File mkDefaultDir(File directory) {

        directory = new File("images");
        if (!directory.exists()) {
            directory.mkdir();
        }

        return directory;
    }

    public static String tryToGetPicFormat(String url) {

        if (url == null) return null;

        if (!url.matches(PIC_REGEX)) return null;

        if (url.indexOf(".jpg")>-1) {

            return "jpg";
        } else if (url.indexOf(".jpeg")>-1) {

            return "jpeg";
        } else if (url.indexOf(".gif")>-1) {

            return "gif";
        } else if (url.indexOf(".bmp")>-1) {

            return "bmp";
        } else if (url.indexOf(".png")>-1) {

            return "png";
        } else if (url.indexOf(".webp")>-1) {

            return "webp";
        } else if (url.indexOf(".svg")>-1) {

            return "svg";
        }

        return null;
    }

    public static String tryToEscapeUrl(String url) {

        if (url == null) return null;

        if (!url.matches(ESCAPE_REGEX)) return url;

        if (url.indexOf(" ")>-1) {

            url =  url.replace(" ",map.get(" "));
        }

        if (url.indexOf("\"")>-1) {

            url =  url.replace("\"",map.get("\""));
        }

        if (url.indexOf("#")>-1) {

            url =  url.replace("#",map.get("#"));
        }

        if (url.indexOf("(")>-1) {

            url =  url.replace("(",map.get("("));
        }

        if (url.indexOf(")")>-1) {

            url =  url.replace(")",map.get(")"));
        }

        if (url.indexOf("+")>-1) {

            url =  url.replace("+",map.get("+"));
        }

        if (url.indexOf(",")>-1) {

            url =  url.replace(",",map.get(","));
        }

        if (url.indexOf(":")>-1) {

            url =  url.replace(":",map.get(":"));
        }

        if (url.indexOf(";")>-1) {

            url =  url.replace(";",map.get(";"));
        }

        if (url.indexOf("<")>-1) {

            url =  url.replace("<",map.get("<"));
        }

        if (url.indexOf(">")>-1) {

            url =  url.replace(">",map.get(">"));
        }

        if (url.indexOf("@")>-1) {

            url =  url.replace("@",map.get("@"));
        }

        if (url.indexOf("\\")>-1) {

            url =  url.replace("\\",map.get("\\"));
        }

        if (url.indexOf("|")>-1) {

            url =  url.replace("|",map.get("|"));
        }

        return url;
    }

    public static String getReferer(String urlString) {

        try {
            URL url = new URL(urlString);
            System.out.println();
            System.out.println(url.getHost());

            return url.getProtocol() + "://" + url.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
