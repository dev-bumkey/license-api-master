package run.acloud.framework.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import run.acloud.api.auth.enums.ServiceMode;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.api.configuration.service.UserService;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.AntPathMatcherUtils;
import run.acloud.commons.vo.ResultVO;
import run.acloud.framework.context.ContextHolder;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;
import run.acloud.framework.properties.CocktailServiceProperties;
import run.acloud.framework.util.AuditAccessLogger;
import run.acloud.framework.util.AuditLogger;
import run.acloud.framework.util.ExceptionMessageUtils;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ControllerAdvice
public class ControllerAdvisor implements ResponseBodyAdvice<Object>{

    @Autowired
    private UserService userService;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private AuditAccessLogger auditAccessLogger;

	@Autowired
	private CocktailServiceProperties cocktailServiceProperties;

	private static Logger aLog = LoggerFactory.getLogger("audit.logger");

	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(Exception.class)
	@ResponseBody
    public ResultVO handleException(HttpServletRequest request, HttpServletResponse response, Exception e) {
    	ResultVO result = new ResultVO();
		if (e instanceof MissingServletRequestParameterException) { // Required Parameter를 빼고 보낸 경우
			ExceptionMessageUtils.setCommonResult(request, result, e, ExceptionType.InvalidParameter);
		} else {
			ExceptionMessageUtils.setCommonResult(request, result, e);
		}
        return result;
    }

	@ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(RuntimeException.class)
	@ResponseBody
    public ResultVO handleRuntimeException(HttpServletRequest request, HttpServletResponse response, RuntimeException e) {
    	ResultVO result = new ResultVO();
		/**
		 * Database 관련 RuntimeException 처리
		 */
		if (e instanceof MyBatisSystemException) { // MyBatis System Exception (DB 처리 관련 Exception)

			boolean isDatabaseConnection = false;
			if (e.getCause() != null && e.getCause().getCause() != null) {
				Exception cce = (Exception) e.getCause().getCause();
				if (cce instanceof CannotGetJdbcConnectionException){
					isDatabaseConnection = true;
				}
			}

			if (isDatabaseConnection) { // Could not connect to SQL Server
				ExceptionMessageUtils.setCommonResult(request, result, e, ExceptionType.DatabaseConnectionFailed);
			}else {
				ExceptionMessageUtils.setCommonResult(request, result, e, ExceptionType.DatabaseProcessingFailed);
			}

		} else if (e instanceof DataAccessException) { // 그외 SQLException 발생

			boolean isSQLException = false;
			if (e.getCause() != null){
				Exception se = (Exception) e.getCause();
				if (se instanceof SQLException) {
					isSQLException = true;
				}
			}

			if (isSQLException) {
				// getCause가 SQLException 일 경우 파라미터로 SQLException을 넘겨 관련 로그를 추가로 남김
				ExceptionMessageUtils.setCommonResult(request, result, (SQLException) e.getCause(), ExceptionType.DatabaseProcessingFailed);
			}
			else {
				// getCause가 SQLException이 아닐 경우 Common RuntimeException 으로 처리 (ExceptionType.DatabaseProcessingFailed)
				ExceptionMessageUtils.setCommonResult(request, result, e, ExceptionType.DatabaseProcessingFailed);
			}

		}
		/**
		 * 그 외 RuntimeException 처리
		 */
		else if (e.getCause() instanceof SocketTimeoutException || e.getCause() instanceof ConnectException) { // SocketTimeoutException
			ExceptionMessageUtils.setCommonResult(request, result, e, "External Api Failed.", ExceptionType.ExternalApiFail);
		}
		else if (e instanceof NullPointerException) {
			ExceptionMessageUtils.setCommonResult(request, result, e, "is null.");
		}
		else {
			ExceptionMessageUtils.setCommonResult(request, result, e, "Unknown.");
		}
        return result;
    }

//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(CocktailException.class)
	@ResponseBody
    public ResultVO handleCocktailException(HttpServletRequest request, CocktailException e) {

        ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e);
        return result;
    }

//	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(NoHandlerFoundException.class)
	@ResponseBody
	public ResultVO handleNoHandlerFoundException(HttpServletRequest request, NoHandlerFoundException e) {
		ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e, HttpStatus.NOT_FOUND.getReasonPhrase(), ExceptionType.K8sApiStatus404);
		return result;
	}

//	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseBody
	public ResultVO handleHttpRequestMethodNotSupportedException(HttpServletRequest request, HttpRequestMethodNotSupportedException e) {
		ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e, HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(), ExceptionType.K8sApiStatus405);
		return result;
	}

//	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	@ResponseBody
	public ResultVO handleHttpMediaTypeNotSupportedException(HttpServletRequest request, HttpMediaTypeNotSupportedException e) {
		ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e, HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(), ExceptionType.K8sApiStatus415);
		return result;
	}

//	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	@ResponseBody
	public ResultVO handleHttpMediaTypeNotAcceptableException(HttpServletRequest request, HttpMediaTypeNotAcceptableException e) {
		ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e, HttpStatus.NOT_ACCEPTABLE.getReasonPhrase(), ExceptionType.K8sApiStatus406);
		return result;
	}

//	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler({
			MissingPathVariableException.class,
			ConversionNotSupportedException.class,
			HttpMessageNotWritableException.class
	})
	@ResponseBody
	public ResultVO handleInternalException(HttpServletRequest request, Exception e) {
		ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ExceptionType.InternalError);
		return result;
	}

//	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			ServletRequestBindingException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestPartException.class
	})
    @ResponseBody
    public ResultVO handleBindException(HttpServletRequest request, Exception e) {
    	ResultVO result = new ResultVO();
		ExceptionMessageUtils.setCommonResult(request, result, e, HttpStatus.BAD_REQUEST.getReasonPhrase(), ExceptionType.K8sApiStatus400);
        return result;
    }


	@ModelAttribute
	public void getRequestHeader(HttpServletRequest request) throws Exception {

        String userId = request.getHeader("user-id");
        if (StringUtils.isNotBlank(userId)) {
            int id;
            try {
                id = Integer.parseInt(userId);
            } catch (NumberFormatException nfe) {
                throw new CocktailException("Invalid user seq", nfe, ExceptionType.UserIdNotFound, userId);
            }

            UserVO user = this.userService.getByUserSeq(id);
            if (user == null) {
                throw new CocktailException("User seq not found", ExceptionType.UserIdNotFound);
            } else if (user.getUseYn().equals("N")) {
                throw new CocktailException("Inactivated user", ExceptionType.InactivatedUser);
            }
        }
	}

	@Override public boolean supports( MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType ) { return true; } 

	// 실행중 임시로 사용할 데이터 저장을 위한 hashmap, 동시성 문제가 없도록 concurrentHashMap 사용.
	private ConcurrentHashMap<String, Object> execTempMap = new ConcurrentHashMap<String, Object>();

	@Override 
	public Object beforeBodyWrite( Object body, 
									MethodParameter returnType, 
									MediaType selectedContentType, 
									Class<? extends HttpMessageConverter<?>> selectedConverterType, 
									ServerHttpRequest request,
								    ServerHttpResponse response ) { 

		ResultVO result;
		if (request.getURI().getPath().contains("swagger")
				|| request.getURI().getPath().startsWith("/v2/api-docs")
				|| request.getURI().getPath().startsWith("/v3/api-docs")
				|| request.getURI().equals("/csrf")
		) {
			return body;
		}

		if (body instanceof ResultVO) {
			result = (ResultVO)body;
		}
		else {
			result = new ResultVO();
			result.setResult(body);
		}

		/**
		 * Audit Logging
		 */
		log.debug("=============================== start audit =======================================");
		try {
			boolean existAuditAccessLogsTable = auditAccessLogger.existTable();
			ServiceMode serviceMode = ServiceMode.valueOf(cocktailServiceProperties.getMode());
			// secret online 모드 이면서 audit_access_logs 테이블 존재하면 audit_access_logs 테이블에 로그 적재, 아니면 기존 테이블에 로그 적재
			if (serviceMode == ServiceMode.SECURITY_ONLINE && existAuditAccessLogsTable){
				writeAuditAccessLog(request, result);
			}else{
				writeAuditLog(request, result);
			}
		}
		catch (IOException ioe) {
			// Audit 로그 처리중 Exception 발생시 에러 로그 남기고 다음 처리를 계속 함..
			if(aLog.isDebugEnabled()) {
				aLog.debug("@CATCH : Can not write audit log! : " + request.getURI());
			}
			else {
				aLog.error("@CATCH : Can not write audit log! : " + request.getURI());
			}
		}
		log.debug("=============================== ended audit =======================================");

		return result;
	}

	/**
	 * 기존 auditlog 처리를 위한 메서드.
	 *
	 * @param request
	 * @param result
	 * @throws IOException
	 */
	private void writeAuditLog(ServerHttpRequest request, ResultVO result) throws IOException {
		boolean isWriteAudit = true;

		/** Audit 이중 적재 방지..
		 * - Client의 Timeout 설정으로 인해 연결이 끊어졌을때... Audit 추가 적재 방지...
		 * 1. 이미 beforeBodyWrite를 실행하여 Audit이 작성되었는데.
		 * 2. 이후 실제 Write 실행시 Connection이 null이라 오류 발생
		 * 3. ExceptionMessageUtils에서 메시지를 구성하여 reWrite가 일어나게 되어 다시한번 beforeBodyWrite가 실행 => 이중으로 Audit Log를 적재하는 문제 발생..
		 * - 아래 해당 Exception 케이스에 대응, ExceptionHandler를 구성하여 1차 처리하였으나 모든 케이스에 대응되도록 ThreadLocal에 Flag 처리하여 재구현 함...
		 * org.eclipse.jetty.io.EofException
		 * A Jetty specialization of EOFException.
		 * This is thrown by Jetty to distinguish between EOF received from the connection, vs and EOF thrown by some application talking to some other file/socket etc. The only difference in handling is that Jetty EOFs are logged less verbosely.
		 **/
		Boolean alreadyWriteAudit = (Boolean) Optional.ofNullable(ContextHolder.auditProcessingDatas().get("alreadyWriteAudit")).orElseGet(() ->Boolean.FALSE);
		if(alreadyWriteAudit.booleanValue()) {
			aLog.debug("### Already Write Audit!!!!!!!!!!!");
			isWriteAudit = false;
		}

		/** Audit Logging 에외 케이스.. **/
		if ("GET".equalsIgnoreCase(request.getMethod().name())) {
			aLog.debug("### Skip Audit Logging. Because Request with GET Method : " + request.getURI().getPath());
			isWriteAudit = false;
		}
		if (isWriteAudit && Optional.ofNullable(ContextHolder.exeContext().getUserSeq()).orElseGet(() ->0) == 0) { // = userSeq is null or 0
			// 사용자 정보 없이 audit logging까지 진입 => Audit Logging이 필요 없는 예외 요청일 가능성 높으므로 제외
			// (사용자 정보가 없는 경우 리소스 인가 Filter에 의해 진입이 불가능하기 때문..)
			if(!StringUtils.equalsIgnoreCase("/api/auth/login", request.getURI().getPath()) // 로그인은 예외
					&& !StringUtils.startsWithIgnoreCase(request.getURI().getPath(), "/internal/otp/user/") // otp 정보 수정 예외
					&& !StringUtils.startsWithIgnoreCase(request.getURI().getPath(), "/api/auth/admin/login") // 칵테일 관리자 로그인(CSAP) 예외
					&& !StringUtils.startsWithIgnoreCase(request.getURI().getPath(), "/api/auth/platform/admin/login") // 플랫폼 관리자 로그인(CSAP) 예외
					&& !StringUtils.startsWithIgnoreCase(request.getURI().getPath(), "/api/auth/platform/user/login") // 플랫폼 사용자 로그인(CSAP) 예외
			) {
				aLog.debug("### Skip Audit Logging. Because Request without User Information : " + request.getURI().getPath());
				isWriteAudit = false;
			}
		}

		/** Audit Logging **/
		if(isWriteAudit) {
			Map<String, Object> auditProcessingDatas = auditLogger.makeRequestDump(request);
			// Filter나 Interceptor에서 예외처리되지 않고 정상적으로 Controller까지 호출되었을 경우만 Audit Logging 처리
			if (auditProcessingDatas.get(CommonConstants.AUDIT_CLASS_NAME) != null) {
				// @Async Call : write audit log....
				auditLogger.write(result, auditProcessingDatas);
			}
		}

		/** Audit 이중 적재 방지를 위해 Audit Logging이 한번 실행되면 ThreadLocal에 Flag 처리하여 확인이 가능하도록 함 **/
		ContextHolder.auditProcessingDatas().put("alreadyWriteAudit", Boolean.TRUE);
	}

	/**
	 * 보안온라인 모드일 경우 사용하는 로그 write.</br>
	 * 보안인증으로 인해 사용됨.
	 *
	 * @param request
	 * @param result
	 * @throws IOException
	 */
	private void writeAuditAccessLog(ServerHttpRequest request, ResultVO result) throws IOException {
		boolean isWriteAudit = true;

		Boolean alreadyWriteAudit = (Boolean) Optional.ofNullable(ContextHolder.auditProcessingDatas().get("alreadyWriteAudit")).orElseGet(() ->Boolean.FALSE);
		if(alreadyWriteAudit.booleanValue()) {
			aLog.debug("### Already Write Audit!!!!!!!!!!!");
			isWriteAudit = false;
		}

		/** Audit Logging 예외 케이스.. **/
		if ("GET".equalsIgnoreCase(request.getMethod().name())
				&& !(AntPathMatcherUtils.getPathMatcher().match("/api/account/*/users", request.getURI().getPath())
						|| AntPathMatcherUtils.getPathMatcher().match("/api/account/*/user/*", request.getURI().getPath())
						|| AntPathMatcherUtils.getPathMatcher().match("/api/user/list", request.getURI().getPath())
						|| AntPathMatcherUtils.getPathMatcher().match("/api/user/*", request.getURI().getPath())
						|| AntPathMatcherUtils.getPathMatcher().match("/api/account/applications/*", request.getURI().getPath())
					)
		) {
			aLog.debug("### Skip Audit Logging. Because Request with GET Method : " + request.getURI().getPath());
			isWriteAudit = false;
		}

		if (isWriteAudit && Optional.ofNullable(ContextHolder.exeContext().getUserSeq()).orElseGet(() ->0) == 0) { // = userSeq is null or 0
			// 사용자 정보 없이 audit logging까지 진입 => Audit Logging이 필요 없는 예외 요청일 가능성 높으므로 제외
			// (사용자 정보가 없는 경우 리소스 인가 Filter에 의해 진입이 불가능하기 때문..)
			if(!StringUtils.equalsIgnoreCase("/api/auth/login", request.getURI().getPath()) // 로그인은 예외
					&& !StringUtils.startsWithIgnoreCase(request.getURI().getPath(), "/internal/otp/user/") // otp 정보 수정 예외
					&& !StringUtils.equalsIgnoreCase(request.getURI().getPath(), "/api/auth/admin/login") // 칵테일 관리자 로그인(CSAP) 예외
					&& !StringUtils.equalsIgnoreCase(request.getURI().getPath(), "/api/auth/platform/admin/login") // 플랫폼 관리자 로그인(CSAP) 예외
					&& !StringUtils.equalsIgnoreCase(request.getURI().getPath(), "/api/auth/platform/user/login") // 플랫폼 사용자 로그인(CSAP) 예외
			) {
				aLog.debug("### Skip Audit Logging. Because Request without User Information : " + request.getURI().getPath());
				isWriteAudit = false;
			}
		}

		/** Audit Logging **/
		if(isWriteAudit) {
			Map<String, Object> auditProcessingDatas = auditAccessLogger.makeRequestDump(request);
			// Filter나 Interceptor에서 예외처리되지 않고 정상적으로 Controller까지 호출되었을 경우만 Audit Logging 처리
			if (auditProcessingDatas.get(CommonConstants.AUDIT_CLASS_NAME) != null) {
				// @Async Call : write audit log....
				auditAccessLogger.write(result, auditProcessingDatas);
			}
		}

		/** Audit 이중 적재 방지를 위해 Audit Logging이 한번 실행되면 ThreadLocal에 Flag 처리하여 확인이 가능하도록 함 **/
		ContextHolder.auditProcessingDatas().put("alreadyWriteAudit", Boolean.TRUE);
	}


}
