package szelink.mt.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * @author mt 2018-11-26
 * 文件压缩工具类
 * 将文件压缩为zip 或者 tar.gz格式
 * 会排除所有以 .deleted 结尾的文件
 */
public final class FileCompressUtil {

    private static String TAR_FILE_SUFFIX = ".tar";

    private static String ZIP_FILE_SUFFIX = ".zip";

    private static String TAR_GZ_FILE_SUFFIX = ".gz";

    private static String NEED_EXCLUDE_FILE = ".deleted";

    /**
     * 将文件压缩为zip格式
     * @param src 源文件
     * @param destPath 压缩后文件输出路径
     * @throws IOException
     */
    public static void fileToZip(File src, String destPath) throws IOException{
        String suffix = fileSuffix(destPath);
        if (!ZIP_FILE_SUFFIX.equals(suffix)) {
            throw new IOException("写出文件的文件名应以 .zip 结尾");
        }
        Project project = new Project();
        FileSet fileSet = new FileSet();
        fileSet.setProject(project);
        if (src.isDirectory()) {
            fileSet.setDir(src);
        } else {
            fileSet.setFile(src);
        }
        Zip zip = new Zip();
        zip.setProject(project);
        zip.setDestFile(new File(destPath));
        fileSet.setExcludes("**\\*.deleted");
        zip.addFileset(fileSet);
        zip.execute();
    }

    /**
     * 将文件归档
     * @param dir 源文件路径
     * @param destPath 归档后文件输出路径
     * @throws IOException
     */
    public static void fileToTar(File dir, String destPath) throws IOException {
        String suffix = fileSuffix(destPath);
        if (!TAR_FILE_SUFFIX.equals(suffix)) {
            throw new IOException("写出文件的文件名应以 .tar 结尾");
        }
        File[] files = fileList(dir).toArray(new File[0]);
        OutputStream outputStream = new FileOutputStream(destPath);
        TarArchiveOutputStream tarOut = new TarArchiveOutputStream(outputStream);
        for (File file : files) {
            tarOut.putArchiveEntry(new TarArchiveEntry(file));
            IOUtils.copy(new FileInputStream(file), tarOut);
            tarOut.closeArchiveEntry();
        }
        tarOut.flush();
        tarOut.close();
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 将已归档的文件压缩成tar.gz格式
     * @param dir 源文件
     * @param destPath 压缩后文件输出路径
     * @throws IOException
     */
    public static void fileToTarGz(File dir, String destPath) throws IOException {
        String suffix = fileSuffix(destPath);
        if (!TAR_GZ_FILE_SUFFIX.equals(suffix)) {
            throw new IOException("写出文件的文件名应以 .gz 结尾");
        }
        Long current = System.currentTimeMillis();
        String tempPath = destPath + current + ".temp.tar";
        fileToTar(dir, tempPath);
        InputStream in = new FileInputStream(tempPath);
        GZIPOutputStream gout = new GZIPOutputStream(new FileOutputStream(destPath));
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            gout.write(buf, 0, len);
        }
        gout.finish();
        gout.close();
        in.close();
        FileUtils.deleteQuietly(new File(tempPath));
    }

    /**
     * 解压缩zip文件
     * @param src 源zip文件
     * @param destPath 解压缩后的文件输出路径
     */
    public static void unZip(File src, String destPath){
        Expand expand = new Expand();
        expand.setSrc(src);
        expand.setDest(new File(destPath));
        expand.execute();
    }


    private static String fileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }


    private static List<File> fileList(File dir) {
        List<File> list = new ArrayList<>();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    int index = name.lastIndexOf(".");
                    return index <= -1 ? true : !NEED_EXCLUDE_FILE.equalsIgnoreCase(name.substring(index));
                }
            });
            for (File file : files) {
                List<File> childList = fileList(file);
                list.addAll(childList);
            }
        } else {
            list.add(dir);
        }
        return list;
    }

    public static void main(String[] args) throws IOException{

    }


}
