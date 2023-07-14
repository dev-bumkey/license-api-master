package run.acloud.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ExecUtils {

    private ExecUtils() {
    }

    public static ResultVO execute(String cmd, boolean isThrow) throws Exception{
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        StringBuffer successOutput = new StringBuffer(); // 성공 스트링 버퍼
        StringBuffer errorOutput = new StringBuffer(); // 오류 스트링 버퍼
        BufferedReader successBufferReader = null; // 성공 버퍼
        BufferedReader errorBufferReader = null; // 오류 버퍼
        String msg = null; // 메시지
        ResultVO result = new ResultVO();

        List<String> cmdList = new ArrayList<>();

        // 운영체제 구분 (window, window 가 아니면 무조건 linux 로 판단)
        if (System.getProperty("os.name").indexOf("Windows") > -1) {
            cmdList.add("cmd");
            cmdList.add("/c");
        } else {
            cmdList.add("sh");
            cmdList.add("-c");
        }
        // 명령어 셋팅
        cmdList.addAll(Arrays.asList(StringUtils.split(cmd, "\n")));
        String[] array = cmdList.toArray(new String[cmdList.size()]);

        if (log.isDebugEnabled()) {
            log.debug("{}", array.length);
            Arrays.asList(array).forEach(c -> log.debug(c));
        }

        try {

            // 명령어 실행
            process = runtime.exec(array);

            // shell 실행이 정상 동작했을 경우
            successBufferReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            while ((msg = successBufferReader.readLine()) != null) {
                successOutput.append(msg + System.getProperty("line.separator"));
            }

            // shell 실행시 에러가 발생했을 경우
            errorBufferReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            while ((msg = errorBufferReader.readLine()) != null) {
                errorOutput.append(msg + System.getProperty("line.separator"));
            }

            // 프로세스의 수행이 끝날때까지 대기
            boolean waitFor = process.waitFor(30, TimeUnit.SECONDS);

            // shell 실행이 정상 종료되었을 경우
            if (waitFor && process.exitValue() == 0) {
                result.setCode("200");
                result.setMessage(successOutput.toString());
            } else {
                // shell 실행이 비정상 종료되었을 경우
                result.setCode("400");
                result.setMessage(successOutput.toString());
            }

            // shell 실행시 에러가 발생
            if (StringUtils.isNotBlank(errorOutput.toString())) {
                // shell 실행이 비정상 종료되었을 경우
                result.setCode("500");
                result.setMessage(errorOutput.toString());
            }

//            log.debug("Exec result!! - code :[{}], result : [{}]", result.getCode(), result.getMessage());
            log.debug("Exec result!! - code :[{}]", result.getCode());

        } catch (Exception e) {
            CocktailException ce = new CocktailException("fail ExecUtils.execute!!", e, ExceptionType.CompressFail_Unzip);
            log.error(ce.getMessage(), ce);
            if (isThrow){
                throw ce;
            }
        } finally {
            try {
                if (process != null) process.destroy();
                if (successBufferReader != null) successBufferReader.close();
                if (errorBufferReader != null) errorBufferReader.close();
            } catch (IOException e) {
                result.setCode("500");
                result.setMessage(null);
                CocktailException ce = new CocktailException("fail ExecUtils.execute by finally!!", e, ExceptionType.CompressFail_Unzip);
                log.error(ce.getMessage(), ce);
                if (isThrow){
                    throw ce;
                }
            }
        }

        return result;
    }

    public static ResultVO execute(String cmd) {
        try {
            return ExecUtils.execute(cmd, false);
        }catch (Exception e){
            log.error(String.format("Failed to execute cmd [%s].", cmd), e);
            return null;
        }
    }
}
