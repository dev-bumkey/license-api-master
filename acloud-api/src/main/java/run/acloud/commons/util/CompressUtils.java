package run.acloud.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
public class CompressUtils {

    private CompressUtils() {
    }


    public static byte[] zipFileToByte(String entryname, byte[] data, boolean isThrow) throws CocktailException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        try {
            zos.putNextEntry(new ZipEntry(entryname));
            zos.write(data);
            zos.closeEntry();
        } catch (IOException e) {
            CocktailException ce = new CocktailException("fail CompressUtils.zipFileToByte!!", e, ExceptionType.CompressFail_Zip);
            log.error(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            }
        } finally {
            try {
                zos.flush();
                zos.finish();
                baos.flush();
                zos.close();
                baos.close();
            } catch (IOException e) {
                log.error("stream close error.", e);
            }
        }

        return baos.toByteArray();
    }

    public static byte[] zipFileToByte(String entryname, byte[] data) {
        try {
            return CompressUtils.zipFileToByte(entryname, data, false);
        }catch (CocktailException e){
            return null;
        }
    }


    /**
     * upload 한 zip 파일을 읽어 zip 파일 내부의 파일을  tmp 파일로 생성후 리턴하는 메서드.</br>
     * zip 파일내에 한개의 파일만 존재하는 zip 파일만 처리 가능. 여러개 파일 unzip은 별도로 만들어야함.</br>
     *
     * @param file
     * @param isThrow
     * @return
     * @throws CocktailException
     */
    public static File unzipFile(MultipartFile file, boolean isThrow) throws CocktailException {
        byte[] buffer = new byte[1024];
        int bufferSize = 1024;
        File tempFile = null;

        if(!file.isEmpty()){

            ZipInputStream zis = null;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;

            try {
                zis = new ZipInputStream(file.getInputStream());

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    tempFile = File.createTempFile(entry.getName(), "tmp");
                    tempFile.deleteOnExit();
                    fos = new FileOutputStream(tempFile);
                    bos = new BufferedOutputStream(fos, bufferSize);

                    int count;
                    while ((count = zis.read(buffer, 0, bufferSize)) != -1) {
                        bos.write(buffer, 0, count);
                    }

                    break;
                }
            } catch (IOException e) {
                CocktailException ce = new CocktailException("fail CompressUtils.unzipFile!!", e, ExceptionType.CompressFail_Unzip);
                log.error(ce.getMessage(), ce);
                if (isThrow){
                    throw ce;
                }
            } finally {
                if (zis != null) {
                    try {
                        zis.close();
                    } catch (IOException e) {
                        log.error("zis stream close error.", e);
                    }
                }

                if (bos != null ){
                    try {
                        bos.flush();
                        bos.close();
                    } catch (IOException e) {
                        log.error("bos stream close error.", e);
                    }
                }

                if (fos != null ){
                    try {
                        fos.close();
                    } catch (IOException e) {
                        log.error("fos stream close error.", e);
                    }
                }
            }
        }

        return tempFile;
    }

    public static File unzipFile(MultipartFile file) {
        try {
            return CompressUtils.unzipFile(file, false);
        }catch (CocktailException e){
            return null;
        }
    }

    public static byte[] zipFilesToByte(String[] srcPaths, boolean isThrow) throws CocktailException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (String srcPath : srcPaths) {

                Path src = Paths.get(srcPath);
                try (FileInputStream fis = new FileInputStream(src.toFile())) {

                    ZipEntry zipEntry = new ZipEntry(src.getFileName().toString());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            CocktailException ce = new CocktailException("fail CompressUtils.zipFiles!!", e, ExceptionType.CompressFail_Zip);
            log.error(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            }
        } finally {
            try {
                baos.flush();
                baos.close();
            } catch (IOException e) {
                log.error("stream close error.", e);
            }
        }

        return baos.toByteArray();
    }

    public static byte[] zipFilesToByte(String[] srcPaths) {
        try {
            return CompressUtils.zipFilesToByte(srcPaths, false);
        }catch (CocktailException e){
            return null;
        }
    }


    public static void zipFiles(String[] srcPaths, String zipFileName, boolean isThrow) throws CocktailException {

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName))) {

            for (String srcPath : srcPaths) {

                Path src = Paths.get(srcPath);
                try (FileInputStream fis = new FileInputStream(src.toFile())) {

                    ZipEntry zipEntry = new ZipEntry(src.getFileName().toString());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            CocktailException ce = new CocktailException("fail CompressUtils.zipFiles!!", e, ExceptionType.CompressFail_Zip);
            log.error(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            }
        }
    }

    public static void zipFiles(String[] srcPaths, String zipFileName) {
        CompressUtils.zipFiles(srcPaths, zipFileName, false);
    }
}
