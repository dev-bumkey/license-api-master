/**
 * Copyright ⓒ 2018 Acornsoft. All rights reserved
 *
 * @project : cocktail-java
 * @category : run.acloud.commons.constants
 * @class : CommonConstants.java
 * @author : Gun Kim (gun@acornsoft.io)
 * @date : 2018. 8. 28 오후 07:30:38
 * @description :
 */

package run.acloud.commons.constants;

public class CommonConstants {

    /**
     * Default Setting
     */
    public static final String DEFAULT_GROUP_NAME = "Default";
    public static final String DEFAULT_USER_DOMAIN = "acornsoft.io";

    /**
     * for Transaction Logging
     */
    public static final String TXID = "TXID";
    public static final String PRINCIPAL = "PRINCIPAL";

    /**
     * for ExecutionContext
     */
    public static final String EXE_CONTEXT = "EXE_CONTEXT";

    /**
     * for Common
     */
    public static final String UPDATE_TARGET_WORKLOAD = "updateTargetWorkloadName";
    /**
     * for Resource Authorization
     */
    public static final String API_CHECK_URI = "/api/check/";
    public static final String REQUEST_URI_CONST = "REQURI";

    /**
     * for Audit Logging..
     */
    // Audit Processing 처리를 위한 데이터 Map : ContextHolder.auditProcessingDatas() 내부에서 사용
    public static final String AUDIT_PROCESSING_DATAS = "AUDIT_PROCESSING_DATAS";
    public static final String AUDIT_EXISTS_AUDIT_ACCESS_LOG_TABLE = "AUDIT_EXISTS_AUDIT_ACCESS_LOG_TABLE";

    // Duration
    public static final String AUDIT_STOPWATCH_OBJECT = "AUDIT_STOPWATCH_OBJECT";
    public static final String AUDIT_DURATION = "AUDIT_DURATION";
    // Audit Type을 구분하기 위한 ID : {ClassName}.{MethodName}
    public static final String AUDIT_CLASS_NAME = "AUDIT_CLASS_NAME";
    public static final String AUDIT_METHOD_NAME = "AUDIT_METHOD_NAME";
    // Request 정보
    public static final String AUDIT_REQUEST_URI = "AUDIT_REQUEST_URI";
    public static final String AUDIT_REQUEST_REFERER = "Referer";
    public static final String AUDIT_REQUEST_CLIENT_IP = "ClientIP";
    public static final String AUDIT_HTTP_METHOD = "AUDIT_HTTP_METHOD";
    public static final String AUDIT_REQUEST_UA = "User-Agent";
    // Audit에 필요한 필수 사용자 정보 : User Seq, User Role, Workspace Seq
    public static final String AUDIT_USER_SEQ = "AUDIT_USER_SEQ";
    public static final String AUDIT_USER_SERVICE_SEQ = "AUDIT_USER_SERVICE_SEQ";
    public static final String AUDIT_USER_ROLE = "AUDIT_USER_ROLE";
    public static final String AUDIT_USER_ID = "AUDIT_USER_ID";
    public static final String AUDIT_USER_NAME = "AUDIT_USER_NAME";

    // Request Data
    public static final String AUDIT_REQUEST_DATAS = "AUDIT_REQUEST_DATAS";         // Request Data 전체 (안에 아래 3종류 값이 Map 형태로 입력)
    public static final String AUDIT_REQUEST_DATA_PATH = "URI_PATH_VARIABLES";      // Rest URI Path의 Variables
    public static final String AUDIT_REQUEST_DATA_QUERY = "QUERY_STRING_VARIABLES"; // Query String으로 전달 받은 Variables
    public static final String AUDIT_REQUEST_DATA_BODY = "REQUEST_BODY";            // Request Body로 입력된 Variables
    // Result Code
    public static final String AUDIT_RESULT_CODE = "AUDIT_RESULT_CODE";
    public static final String AUDIT_RESULT_DATAS = "AUDIT_RESULT_DATAS";           // Response Data 전체
    // Specific하게 처리해야 하는 Audit 로그들의 데이터 저장을 위한 데이터
    public static final String AUDIT_ADDITIONAL_TYPE = "AUDIT_ADDITIONAL_TYPE";     // AuditAdditionalType.getAuditAdditionalCode()
    public static final String AUDIT_ADDITIONAL_DATAS = "AUDIT_ADDITIONAL_DATAS";
    // ASync를 여러개 실행하였을때 Context를 공유하여 문제 문제 발생... Template 배포시 한번에 하나의  Async만 처리할 수 있도록 하기 위함...
    public static final String AUDIT_WORKLOAD_PROCESSING_FINISHED = "AUDIT_WORKLOAD_PROCESSING_ENDED"; // 워크로드 처리가 끝난 후 Setting

    /**
     * for API Key Authentication
     */
    public static final String CLUSTER_ACCESSKEY_DELIMITER = "|";
    public static final int CLUSTER_SECRET_DEFAULT_EXPIRED_YEAR = 10;
    public static final int CLUSTER_SEQ_MASKING_VALUE = 554;
    public static final String CLUSTER_ACCESS_SIGNATURE_DELIMITER = "</>";
}
