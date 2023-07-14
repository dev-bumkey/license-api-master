package run.acloud.commons.handler;

import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import run.acloud.commons.util.CompressUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Getter
@Setter
public abstract class AbstractCsvDownloadResultHandler<T> implements ResultHandler<T> {

    private HttpServletResponse response;
    private List<String> headers;
    private CSVWriter csvWriter;
    private Path tempCsvFile;
    private Path tempZipFile;

    /**
     * 생성자
     *
     * @param response
     * @param headers header를 구성할 정보 셋팅
     */
    public AbstractCsvDownloadResultHandler(HttpServletResponse response, List<String> headers) {
        this.response = response;
        this.headers = headers;

        try {
            this.tempCsvFile = Files.createTempFile(null, ".csv");
            this.csvWriter = new CSVWriter(new FileWriter(this.tempCsvFile.toFile()));
            this.csvWriter.writeNext(headers.toArray(new String[headers.size()]));
        } catch (IOException e) {
            this.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw new RuntimeException(e);
        }
    }

    @Override
    public abstract void handleResult(ResultContext<? extends T> resultContext);

    public void writeNext(List<String> nextLine) {
        if (CollectionUtils.isNotEmpty(nextLine)) {
            this.csvWriter.writeNext(nextLine.toArray(new String[nextLine.size()]));
        } else {
            log.warn("cvs result handler current line is empty.");
        }
    }

    public void closeAfterWrite(String fileName, boolean useOnlyZipFile) {
        // csv 생성 및 종료
        try {
            // csv writer flush
            this.csvWriter.flush();

            long fileSize = Files.size(this.tempCsvFile);
            long limitSize = 10 * 1024 * 1024; // 10MB
            if (useOnlyZipFile || limitSize < fileSize) {
                // 1. Set response header
                String zipFileName = String.format("%s.zip", fileName);
                this.response.setContentType("application/zip; UTF-8");
                this.response.setHeader("Accept-Ranges", "bytes");
                this.response.setHeader("Set-Cookie", "fileDownload=true; path=/");
                this.response.setHeader("Content-Disposition", String.format("attachment; filename=%s; filename*=UTF-8''%s", zipFileName, zipFileName));
                this.response.setHeader("Content-Transfer-Encoding", "binary");

                // 2. create temp zip file
                this.setTempZipFile(Files.createTempFile(null, ".zip"));

                // 3. generated temp csv file to compress zip file
                CompressUtils.zipFiles(
                        StringUtils.split(this.getTempCsvFile().toAbsolutePath().toString())
                        , this.getTempZipFile().toAbsolutePath().toString()
                        , true
                );

                // 4. copy response
                try (InputStream in = Files.newInputStream(this.getTempZipFile())) {
                    IOUtils.copyLarge(in, this.response.getOutputStream());
                }
            } else {
                // 1. Set response header
                String csvFileName = String.format("%s.csv", fileName);
                this.response.setContentType("text/csv; charset=UTF-8");
                this.response.setHeader("Content-Encoding", "UTF-8");
                this.response.setHeader("Set-Cookie", "fileDownload=true; path=/");
                this.response.setHeader("Content-Disposition", String.format("attachment; filename=%s; filename*=UTF-8''%s", csvFileName, csvFileName));

                // 2. copy response
                try (InputStream in = Files.newInputStream(this.getTempCsvFile())) {
                    IOUtils.copyLarge(in, this.response.getOutputStream());
                }
            }

            this.response.setStatus(HttpServletResponse.SC_OK);
            this.response.flushBuffer();
            this.getCsvWriter().close();
        } catch (IOException e) {
            log.error("CommonCsvResultHandler error.", e);
            this.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {

            this.getTempCsvFile().toFile().deleteOnExit();
            if (this.getTempZipFile() != null) {
                this.getTempZipFile().toFile().deleteOnExit();
            }
        }
    }
}
