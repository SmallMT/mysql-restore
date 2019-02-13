package szelink.mt.util;

import szelink.mt.constant.CustomizeConstant;

import java.util.UUID;

/**
 * @author mt
 * 通用工具类
 */
public final class CommonUtil {

    /**
     * 判断当前操作系统是否为windows系统
     * @return
     */
    public static boolean isWindowsOs() {
        return System.getProperty("os.name").toLowerCase().contains(CustomizeConstant.WINDOWS_OS);
    }

    /**
     * 生成32位UUID
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }


    public static void main(String[] args) {
        System.out.println(CommonUtil.isWindowsOs());
    }

}
