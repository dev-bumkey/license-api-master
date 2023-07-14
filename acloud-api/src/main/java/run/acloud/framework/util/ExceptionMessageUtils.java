package run.acloud.framework.util;

import com.google.api.client.util.Maps;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import run.acloud.commons.util.JsonUtils;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.filter.LoggerFilter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public final class ExceptionMessageUtils {
    private ExceptionMessageUtils() {
    }

    public static String setCommonResult(HttpServletRequest request, ResultVO result, Exception e, String message, boolean isLogged){

        if(result == null){
            result = new ResultVO();
        }

        String urlPath = request.getServletPath();
        String method = request.getMethod();

        ExceptionType exceptionType = ExceptionMessageUtils.getCommonExceptionType(method);
        result.setCode(exceptionType.getErrorCode());
        result.setBiz(ExceptionBiz.getExceptionBizByUrl(urlPath).getBizCode());
        if (StringUtils.isNotBlank(message)) {
            result.setMessage(message);
        } else {
            result.setMessage(e.getMessage());
        }
        result.setStatus("error");
//        ExceptionMessageUtils.setCommonResultRequestInfo(request, result);

        StringBuilder sb = new StringBuilder();
        sb.append("\n==============================================================================================================================================\n");
//        sb.append("# Error     : ").append(String.format("[%s][%s]", exceptionType.getErrorCode(), exceptionType.toString())).append("\n");
        ExceptionMessageUtils.setError(sb, exceptionType);
        sb.append("# Biz       : ").append(result.getBiz()).append("\n");
        sb.append("# Data      : ").append(result.getData()).append("\n");
        sb.append("# Exception : ").append(ExceptionMessageUtils.getException(e)).append("\n");
        sb.append("# Message   : ").append(e.getMessage()).append("\n");
        sb.append("# Cause     : ").append(ExceptionMessageUtils.getExceptionCause(e)).append("\n");
        sb.append("# Request   : ").append(ExceptionMessageUtils.getRequestInfo(request)).append("");
//        sb.append("# StackTrace: ").append(ExceptionMessageUtils.getStackTrace(e)).append("\n\n");
        sb.append("==============================================================================================================================================\n");

        ExceptionMessageUtils.isLogged(isLogged, exceptionType, sb.toString(), e);

        return sb.toString();
    }

    /**
     * isLogged = true 로 기본으로 에러로그 출력시 사용
     *
     * @param request
     * @param result
     * @param e
     */
    public static void setCommonResult(HttpServletRequest request, ResultVO result, Exception e){
        ExceptionMessageUtils.setCommonResult(request, result, e, null, true);
    }

    public static void setCommonResult(HttpServletRequest request, ResultVO result, Exception e, String message){
        ExceptionMessageUtils.setCommonResult(request, result, e, message, true);
    }

    public static String setCommonResult(HttpServletRequest request, ResultVO result, Exception e, String message, ExceptionType exceptionType, boolean isLogged){

        if(result == null){
            result = new ResultVO();
        }

        result.setCode(exceptionType.getErrorCode());
        if (StringUtils.isNotBlank(message)) {
            result.setMessage(message);
        } else {
            result.setMessage(e.getMessage());
        }
        result.setStatus("error");
//        ExceptionMessageUtils.setCommonResultRequestInfo(request, result);

        StringBuilder sb = new StringBuilder();
        sb.append("\n==============================================================================================================================================\n");
//        sb.append("# Error     : ").append(String.format("[%s][%s]", exceptionType.getErrorCode(), exceptionType.toString())).append("\n");
        ExceptionMessageUtils.setError(sb, exceptionType);
        sb.append("# Data      : ").append(result.getData()).append("\n");
        sb.append("# Exception : ").append(ExceptionMessageUtils.getException(e)).append("\n");
        sb.append("# Message   : ").append(e.getMessage()).append("\n");
        sb.append("# Cause     : ").append(ExceptionMessageUtils.getExceptionCause(e)).append("\n");
        sb.append("# Request   : ").append(ExceptionMessageUtils.getRequestInfo(request)).append("");
//        sb.append("# StackTrace: ").append(ExceptionMessageUtils.getStackTrace(e)).append("\n\n");
        sb.append("==============================================================================================================================================\n");

        ExceptionMessageUtils.isLogged(isLogged, exceptionType, sb.toString(), e);

        return sb.toString();
    }

    /**
     * isLogged = true 로 기본으로 에러로그 출력시 사용
     *
     * @param request
     * @param result
     * @param e
     * @param exceptionType
     */
    public static void setCommonResult(HttpServletRequest request, ResultVO result, Exception e, String message, ExceptionType exceptionType){
        ExceptionMessageUtils.setCommonResult(request, result, e, message, exceptionType, true);
    }

    /**
     * isLogged = true 로 기본으로 에러로그 출력시 사용
     *
     * @param request
     * @param result
     * @param e
     * @param exceptionType
     */
    public static void setCommonResult(HttpServletRequest request, ResultVO result, Exception e, ExceptionType exceptionType){
        ExceptionMessageUtils.setCommonResult(request, result, e, null, exceptionType, true);
    }

    /**
     * SQLException일 경우 오류 분석을 위한 추가 로그 출력 (SQLState, SQLErrorCode)
     * @param request
     * @param result
     * @param e
     * @param exceptionType
     */
    public static String setCommonResult(HttpServletRequest request, ResultVO result, SQLException e, ExceptionType exceptionType, boolean isLogged){

        if(result == null){
            result = new ResultVO();
        }

        result.setCode(exceptionType.getErrorCode());
        result.setMessage(e.getMessage());
        result.setStatus("error");
//        ExceptionMessageUtils.setCommonResultRequestInfo(request, result);

        StringBuilder sb = new StringBuilder();
        sb.append("\n==============================================================================================================================================\n");
//        sb.append("# Error       : ").append(String.format("[%s][%s]", exceptionType.getErrorCode(), exceptionType.toString())).append("\n");
        ExceptionMessageUtils.setError(sb, exceptionType);
        sb.append("# Data        : ").append(result.getData()).append("\n");
        sb.append("# Exception:  : ").append(ExceptionMessageUtils.getException(e)).append("\n");
        sb.append("# SQLState    : ").append(e.getSQLState()).append("\n");
        sb.append("# SQLErrorCode: ").append(e.getErrorCode()).append("\n");
        sb.append("# Message     : ").append(e.getMessage()).append("\n");
        sb.append("# Cause       : ").append(ExceptionMessageUtils.getExceptionCause(e)).append("\n");
        sb.append("# Request     : ").append(ExceptionMessageUtils.getRequestInfo(request)).append("");
//        sb.append("# StackTrace  : ").append(ExceptionMessageUtils.getStackTrace(e)).append("\n\n");
        sb.append("==============================================================================================================================================\n");

        ExceptionMessageUtils.isLogged(isLogged, exceptionType, sb.toString(), e);

        return sb.toString();
    }

    /**
     * isLogged = true 로 기본으로 에러로그 출력시 사용
     *
     * @param request
     * @param result
     * @param e
     * @param exceptionType
     */
    public static void setCommonResult(HttpServletRequest request, ResultVO result, SQLException e, ExceptionType exceptionType){
        ExceptionMessageUtils.setCommonResult(request, result, e, exceptionType, true);
    }

    /**
     * CocktailException 처리
     * @param request
     * @param result
     * @param e
     * @param isLogged
     * @return
     */
    public static String setCommonResult(HttpServletRequest request, ResultVO result, CocktailException e, boolean isLogged){

        if(result == null){
            result = new ResultVO();
        }

        String bizCode = null;
        if(e.getBiz() != null){
            bizCode = e.getBiz().getBizCode();
        }
        result.setCode(e.getType().getErrorCode());
        result.setBiz(bizCode);
        result.setMessage(e.getMessage());
        result.setAdditionalMessage(e.getAdditionalMessage());
        result.setStatus("error");
        if(result.getData() == null){
            result.setData(e.getData());
        }
//        ExceptionMessageUtils.setCommonResultRequestInfo(request, result);
        result.setHttpStatusCode(e.getHttpStatusCode());

        StringBuilder sb = new StringBuilder();

        sb.append("\n==============================================================================================================================================\n");
//        sb.append("# Error       : ").append(String.format("[%s][%s]", e.getType().getErrorCode(), e.getType().toString())).append("\n");
        ExceptionMessageUtils.setError(sb, e.getType());
        sb.append("# Biz      : ").append(StringUtils.defaultString(bizCode)).append("\n");
        sb.append("# Data     : ").append(result.getData()).append("\n");
        sb.append("# Exception: ").append(ExceptionMessageUtils.getException(e)).append("\n");
        sb.append("# Message  : ").append(e.getMessage()).append("\n");
        sb.append("# Additional Message :").append(e.getAdditionalMessage()).append("\n");
        sb.append("# Cause    : ").append(ExceptionMessageUtils.getExceptionCause(e)).append("\n");
        sb.append("# Request  : ").append(ExceptionMessageUtils.getRequestInfo(request)).append("");
//        sb.append("# StackTrace  : ").append(ExceptionMessageUtils.getStackTrace(e)).append("\n\n");
        sb.append("==============================================================================================================================================\n");

        ExceptionMessageUtils.isLogged(isLogged, e.getType(), sb.toString(), e);

        return sb.toString();
    }

    public static String getRequestInfo(HttpServletRequest request){
        try {
            if(request != null){

                String urlPath = request.getServletPath();
                String method = request.getMethod();

                Map<String, String> requestHeaderMap = ExceptionMessageUtils.getHeadersInfo(request);
                Map<String, Object> dataMap = Maps.newHashMap();
                dataMap.put("URL", String.format("%s [%s]", urlPath, method));
                dataMap.put("RemoteHost", request.getRemoteHost());
                dataMap.put("Headers", (MapUtils.isNotEmpty(requestHeaderMap) ? JsonUtils.toGson(requestHeaderMap) : ""));
                dataMap.put("QueryString", request.getQueryString());
                if (!StringUtils.startsWith(request.getContentType(), "multipart/") && !request.getRequestURI().endsWith("/file/upload")) {
                    dataMap.put("RequestBody", LoggerFilter.getBody(request));
                }

                return JsonUtils.toPrettyString(dataMap);
            }
        } catch (IOException e) {
            log.error("getRequestInfo error", e);
        }

        return null;
    }

    /**
     * isLogged = true 로 기본으로 에러로그 출력시 사용
     *
     * @param request
     * @param result
     * @param e
     */
    public static void setCommonResult(HttpServletRequest request, ResultVO result, CocktailException e){
        ExceptionMessageUtils.setCommonResult(request, result, e, true);
    }

    public static void setCommonResultRequestInfo(HttpServletRequest request, ResultVO result){
        if (result != null) {
            result.setRequestInfo(getRequestInfo(request));
        }
    }

    public static String getException(Exception e){
        return e.getClass().getName();
    }

    public static String getExceptionCause(Exception e){
        StringBuilder sb = new StringBuilder();

        Throwable rootCause = ExceptionUtils.getRootCause(e);
        sb.append("\n").append(String.format("### Root Cause : %s", ExceptionMessageUtils.getExceptionName(rootCause))).append("\n");
        for(Throwable throwableRow : ExceptionUtils.getThrowableList(e)){
            sb.append(String.format("- %s", ExceptionMessageUtils.getExceptionName(throwableRow))).append("\n");
        }

        return sb.toString();
    }

    public static String getExceptionName(Throwable e){
        return (e != null ? String.format("%s: %s", e.getClass().getName(), e.getMessage()) : "");
    }

    public static String getStackTrace(Exception e){
        StringBuilder sb = new StringBuilder();

        StackTraceElement elements[] = e.getStackTrace();
        for (int i = 0, n = elements.length; i < n; i++) {
            sb.append("\n").append(String.format("- %s:%d >> %s()", elements[i].getFileName(), elements[i].getLineNumber(), elements[i].getMethodName()));
        }

        return sb.toString();
    }

    public static ExceptionType getCommonExceptionType(String httpMethod){
        switch (httpMethod){
            case "GET":
                return ExceptionType.CommonInquireFail;
            case "POST":
                return ExceptionType.CommonCreateFail;
            case "DELETE":
                return ExceptionType.CommonDeleteFail;
            case "PUT":
                return ExceptionType.CommonUpdateFail;
            default:
                return ExceptionType.CommonFail;
        }
    }

    public static Map<String, Object> setParameterData(String colName, String colValue, int maxLength){
        Map<String, Object> objectMap = Maps.newHashMap();
        objectMap.put("column", colName);
        objectMap.put("value", colValue);
        objectMap.put("length", StringUtils.length(colValue));
        objectMap.put("maxLength", maxLength);

        return objectMap;
    }

    public static Map<String, Object> setParameterData(String colName, Object colValue, String reason){
        Map<String, Object> objectMap = Maps.newHashMap();
        objectMap.put("column", colName);
        objectMap.put("value", colValue);
        objectMap.put("reason", StringUtils.defaultString(reason));

        return objectMap;
    }

    public static Map<String, Object> setParameterDataEmpty(String colName, Object colValue){
        return ExceptionMessageUtils.setParameterData(colName, colValue, "is empty");
    }

    public static Map<String, Object> setParameterDataInvalid(String colName, Object colValue){
        return ExceptionMessageUtils.setParameterData(colName, colValue, "is invalid");
    }

    public static void checkParameter(String colName, String colValue, int maxLength, boolean isRequired) throws CocktailException{
        if(StringUtils.isNotBlank(colValue)){
            if(StringUtils.length(colValue) > maxLength){
                throw new CocktailException(String.format("%s is more than %d characters.", StringUtils.capitalize(colName), maxLength), ExceptionType.InvalidParameter_Overflow
                        , JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterData(colName, colValue, maxLength)));
            }
        }else{
            if(isRequired){
                throw new CocktailException(String.format("%s is empty.", StringUtils.capitalize(colName)), ExceptionType.InvalidParameter_Empty
                        , JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterDataEmpty(colName, colValue)));
            }
        }
    }

    public static void checkParameterRequired(String colName, Object colValue) throws CocktailException{
        ExceptionMessageUtils.checkParameterRequired(colName, colValue, null);
    }

    public static void checkParameterRequired(String colName, Object colValue, String reason) throws CocktailException{
        if(colValue == null){
            String errMsg;
            if (StringUtils.isNotBlank(reason)) {
                errMsg = JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterData(colName, colValue, reason));
            } else {
                errMsg = JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterDataEmpty(colName, colValue));
            }
            throw new CocktailException(String.format("%s is empty.", StringUtils.capitalize(colName)), ExceptionType.InvalidParameter_Empty
                    , errMsg);
        }
    }

    public static void checkParameterRequired(String colName, String colValue) throws CocktailException{
        ExceptionMessageUtils.checkParameterRequired(colName, colValue, null);
    }

    public static void checkParameterRequired(String colName, String colValue, String reason) throws CocktailException{
        if(StringUtils.isBlank(colValue)){
            String errMsg;
            if (StringUtils.isNotBlank(reason)) {
                errMsg = JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterData(colName, colValue, reason));
            } else {
                errMsg = JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterDataEmpty(colName, colValue));
            }
            throw new CocktailException(String.format("%s is empty.", StringUtils.capitalize(colName)), ExceptionType.InvalidParameter_Empty
                    , errMsg);
        }
    }

    public static void checkParameterRequired(String colName, Integer colValue) throws CocktailException{
        ExceptionMessageUtils.checkParameterRequired(colName, colValue, null);
    }

    public static void checkParameterRequired(String colName, Integer colValue, String reason) throws CocktailException{
        if(colValue == null || (colValue != null && colValue.intValue() < 1)){
            String errMsg;
            if (StringUtils.isNotBlank(reason)) {
                errMsg = JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterData(colName, colValue, reason));
            } else {
                errMsg = JsonUtils.toPrettyString(ExceptionMessageUtils.setParameterDataEmpty(colName, colValue));
            }
            throw new CocktailException(String.format("%s is empty.", StringUtils.capitalize(colName)), ExceptionType.InvalidParameter_Empty
                    , errMsg);
        }
    }

    public static Map<String, String> getHeadersInfo(HttpServletRequest request) {

        Map<String, String> map = new HashMap();

        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }

        return map;
    }

    public static void isLogged(boolean isLogged, ExceptionType exceptionType, String exceptionMessage, Exception e){
        if (isLogged){
            if(exceptionType == ExceptionType.NotAuthorized){
                log.error(exceptionMessage);
            }else{
                log.error(exceptionMessage, e);
            }
        }
    }

    private static void setError(StringBuilder sb, ExceptionType exceptionType) {
        sb.append("# Error    : ");
        if(exceptionType == ExceptionType.ExternalApiFail_ClusterApi ||
            exceptionType == ExceptionType.ExternalApiFail_PackageApi ||
            exceptionType == ExceptionType.RegistryApiFail) {
            sb.append("[MAJOR]");
        }
        else if(exceptionType == ExceptionType.DatabaseProcessingFailed ||
            exceptionType == ExceptionType.DatabaseConnectionFailed) {
            sb.append("[CRITICAL]");
        }
        sb.append(String.format("[%s][%s]", exceptionType.getErrorCode(), exceptionType.toString())).append("\n");
    }

}
