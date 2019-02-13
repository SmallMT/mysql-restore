package szelink.mt.util;

import org.springframework.stereotype.Component;
import szelink.mt.constant.CustomizeConstant;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mt
 * mysql 数据备份工具类
 * 在这个类中,进行对mysql数据的备份
 * 默认备份参数参照 DataBackUpConfig
 */
@Component("dataBackUpUtil")
public final class DataBackUpUtil {

    /**
     * 不指定压缩格式,则根据当前操作系统进行压缩
     * windows 平台压缩为 .zip格式
     * linux   平台压缩为 .tar.gz格式
     * @param srcPath 源文件路径
     * @param destPath 压缩后输出文件路径
     * @param fileName 压缩后文件名
     * @throws IOException
     */
    public static void backup(String srcPath, String destPath, String fileName) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (CommonUtil.isWindowsOs()) {
            backup(srcPath, destPath, fileName, CompressType.ZIP);
        } else if (CustomizeConstant.LINUX_OS.equals(os)) {
            backup(srcPath, destPath, fileName, CompressType.TAR_GZ);
        }
    }

    public static void backup(String srcPath, String destPath) throws IOException{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName = sdf.format(new Date());
        backup(srcPath, destPath, fileName);
    }

    public static void backup(String srcPath, String destPath, String fileName, CompressType type) throws IOException {
        if (type.equals(CompressType.ZIP)) {
            // 压缩为zip格式
            File file = new File(destPath + "\\" + fileName + CustomizeConstant.ZIP);
            FileCompressUtil.fileToZip(new File(srcPath), file.getAbsolutePath());
        } else if (type.equals(CompressType.TAR_GZ)) {
            // 压缩为 tar.gz格式
            File file = new File(destPath + "\\" + fileName + CustomizeConstant.TAR_GZ);
            FileCompressUtil.fileToTarGz(new File(srcPath), file.getAbsolutePath());
        }
    }
}
