package run.acloud.framework.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private ByteArrayOutputStream cachedBytes;

    private final Charset encoding;
    private byte[] rawData;
    
    public CustomHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);

        String characterEncoding = request.getCharacterEncoding();
        if (StringUtils.isBlank(characterEncoding)) {
            characterEncoding = StandardCharsets.UTF_8.name();
        }
        this.encoding = Charset.forName(characterEncoding);
//
//        // Convert InputStream data to byte array and store it to this wrapper instance.
//        try {
//            InputStream inputStream = request.getInputStream();
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            byte[] readByte = new byte[1024];
//            int readLen;
//            while ((readLen = inputStream.read(readByte)) != -1)
//           {
//                outputStream.write(readByte, 0, readLen);
//            }
//            this.rawData = outputStream.toByteArray();
//        } catch (IOException e) {
//            log.error("Error reading the request body...");
//        }
    }

    
    @Override
    public ServletInputStream getInputStream() throws IOException {

        if (cachedBytes == null) cacheInputStream();

        return new CachedServletInputStream(cachedBytes.toByteArray());
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream(), this.encoding));
    }

    private void cacheInputStream() throws IOException {

        cachedBytes = new ByteArrayOutputStream();
        IOUtils.copy(super.getInputStream(), cachedBytes);
    }

    public static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        public CachedServletInputStream(byte[] contents) {
            this.buffer = new ByteArrayInputStream(contents);
        }

        @Override
        public int read() {

            return buffer.read();
        }

        @Override
        public boolean isFinished() {

            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {

            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {

            throw new RuntimeException("Not implemented");
        }
    }
}
