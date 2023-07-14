/**
 * Copyright ⓒ 2018 Acornsoft. All rights reserved
 * @project     : cocktail-java
 * @category    : run.acloud.framework.filter
 * @class       : TransactionLoggingFilter.java
 * @author      : Gun Kim (gun@acornsoft.io)
 * @date        : 2018. 8. 28 오후 07:30:38
 * @description :
 */
package run.acloud.framework.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import run.acloud.commons.util.JsonUtils;

import java.io.*;

/**
 * swagger v3/api-decs unescape
 */
@Slf4j
public class OpenApiDocsFormatterFilter implements Filter {
    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {

        ByteResponseWrapper byteResponseWrapper = new ByteResponseWrapper((HttpServletResponse) response);
        ByteRequestWrapper byteRequestWrapper = new ByteRequestWrapper((HttpServletRequest) request);

        chain.doFilter(byteRequestWrapper, byteResponseWrapper);

        String jsonResponse = new String(byteResponseWrapper.getBytes(), response.getCharacterEncoding());
        String result = StringUtils.removeEnd(StringUtils.removeStart(JsonUtils.unescape(jsonResponse), "\""), "\"");
        response.getOutputStream().write(result.getBytes(response.getCharacterEncoding()));

    }

    static class ByteResponseWrapper extends HttpServletResponseWrapper {

        private PrintWriter writer;
        private ByteOutputStream output;

        public byte[] getBytes() {
            writer.flush();
            return output.getBytes();
        }

        public ByteResponseWrapper(HttpServletResponse response) {
            super(response);
            output = new ByteOutputStream();
            writer = new PrintWriter(output);
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return output;
        }
    }

    static class ByteRequestWrapper extends HttpServletRequestWrapper {

        byte[] requestBytes = null;
        private ByteInputStream byteInputStream;


        public ByteRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            InputStream inputStream = request.getInputStream();

            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            replaceRequestPayload(baos.toByteArray());
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public ServletInputStream getInputStream() {
            return byteInputStream;
        }

        public void replaceRequestPayload(byte[] newPayload) {
            requestBytes = newPayload;
            byteInputStream = new ByteInputStream(new ByteArrayInputStream(requestBytes));
        }
    }

    static class ByteOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream bos = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            bos.write(b);
        }

        public byte[] getBytes() {
            return bos.toByteArray();
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
    }

    static class ByteInputStream extends ServletInputStream {

        private InputStream inputStream;

        public ByteInputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }
}
