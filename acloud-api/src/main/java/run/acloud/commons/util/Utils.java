package run.acloud.commons.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import run.acloud.api.auth.vo.UserVO;
import run.acloud.commons.enums.ApiVersionType;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

@Slf4j
public final class Utils {
//    private static final Pattern IPv4 =
//            Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    private static final Pattern IPv4WithPort =
            Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(?::[\\d]+)?$\n");
//    private static final Pattern HostName =
//            Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
    private static final Pattern HostNameWithPort =
            Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])(?::[\\d]+)?$");

	private static final Pattern IPv4Address =
		Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private static final Pattern PASSWORD_POLICY_REGEX_1 = Pattern.compile("^(?=\\S*[a-z])(?=\\S*[A-Z])(?=\\S*[0-9])(?=\\S*[!@#$%^&*\\-_=+])\\S{8,24}$");
    private static final Pattern PASSWORD_POLICY_REGEX_2 = Pattern.compile("[^a-zA-Z0-9!@#$%^&*\\-_=+]");

    private static final Pattern BASE64_ENCODED_STRING = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");

	// IANA_SVC_NAME :
	// matching regex [a-z0-9]([a-z0-9-]*[a-z0-9])*,
	// it must contain at least one letter [a-z],
	// and hyphens cannot be adjacent to other hyphens,
	// at most 15 characters,
	// : e.g. "http"
	private static final Pattern IANA_SVC_NAME = Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])*$");
	// IANA_SVC_NAME 보완 : must contain at least one letter [a-z]
	private static final Pattern MUST_CONTAIN_AT_LEAST_ONE_LETTER_WITH_IANA_CHARACTER_ONLY = Pattern.compile("^[a-z0-9-]*[a-z]+[a-z0-9-]*$");

	public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String DEFAULT_DATE_TIME_ZONE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public static boolean isIanaSvcName(String str) { return IANA_SVC_NAME.matcher(str).matches(); }

	public static boolean isContainLetterWithIanaCharactersOnly(String str) { return MUST_CONTAIN_AT_LEAST_ONE_LETTER_WITH_IANA_CHARACTER_ONLY.matcher(str).matches(); }

	public static HttpClient makeHttpClient(Boolean ssl) throws Exception {
		if (!ssl) {
			return HttpClients.createDefault();
		}

		TrustStrategy trustStrategy = (x509Certificates, s) -> true;
		HttpClientBuilder builder = HttpClientBuilder.create();
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
		builder.setSSLContext(sslContext);

		HostnameVerifier hostnameVerifier = (s, sslSession) -> true;
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory)
				.build();
		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
		builder.setConnectionManager(connMgr);

		return builder.build();
	}

    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        long l = ByteBuffer.wrap(uuid.toString().getBytes()).getLong();
        return Long.toString(l, Character.MAX_RADIX);
    }

	public static Boolean checkIPv4AddressWithPort(String address) {
		return IPv4WithPort.matcher(address).matches();
	}

	public static Boolean checkHostAddressWithPort(String address) {
		return checkIPv4AddressWithPort(address) || HostNameWithPort.matcher(address).matches();
	}

	public static Boolean checkIPv4Address(String address) {
		return IPv4Address.matcher(address).matches();
	}

    public static boolean isApiVersion(ApiVersionType source, ApiVersionType target){
		if(source != null && target != null){
			return source == target;
		}else{
			return false;
		}
	}

	public static boolean isApiVersionV1(ApiVersionType source){
    	if(source != null){
    		return Utils.isApiVersion(source, ApiVersionType.V1);
		}else{
    		return false;
		}
	}

	public static boolean isApiVersionV2(ApiVersionType source){
    	if(source != null){
    		return Utils.isApiVersion(source, ApiVersionType.V2);
		}else{
    		return false;
		}
	}

	public static boolean isApiVersionV3(ApiVersionType source){
    	if(source != null){
    		return Utils.isApiVersion(source, ApiVersionType.V3);
		}else{
    		return false;
		}
	}

	public static boolean isNullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}

	public static boolean isNotNullOrEmpty(String str) {
		return !isNullOrEmpty(str);
	}

	public static boolean isBase64EncodedStr(String str) {
		String encodedStr = null;
		try {
			String decodedStr = new String(Base64Utils.decodeFromString(str), StandardCharsets.UTF_8);
			encodedStr = Base64Utils.encodeToString(decodedStr.getBytes(StandardCharsets.UTF_8));
		} catch (IllegalArgumentException e) {
			log.warn(String.format("%s - %s", "fail base64!!", e.getMessage()));
		} catch (Exception e) {
			log.warn(String.format("%s - %s", "fail base64!!", e.getMessage()));
		}

		return StringUtils.equals(str, encodedStr);
	}

	// 소문자, 숫자, 특수문자
	private static final Pattern ALLOW_NAME = Pattern.compile("[a-z0-9-._]+$");
	public static boolean isValidName(String name) {
		if (StringUtils.isNotBlank(name)) {
			if (ALLOW_NAME.matcher(name).matches()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isValidPassword(String pwd) {

		if (StringUtils.isNotBlank(pwd)) {
			/**
			 * 대문자, 소문자, 숫자, 특수문자(!@#$%^&*-_=+) 모두 무조건 1개 이상
			 */
			if (!Pattern.compile("^(?=.*\\d)(?=.*[!@#$%^&*-_=+])(?=.*[a-z])(?=.*[A-Z])[A-Za-z\\d!@#$%^&*-_=+]{8,24}$").matcher(pwd).matches()) {
				return false;
			}
			/**
			 * 3자리 이상 같은 숫자/문자
			 */
			if (Pattern.compile("(\\p{Alnum})\\1{2,}").matcher(pwd).find()) {
				return false;
			}
			/**
			 * 3자리 이상 연속된 숫자/문자
			 */
			String listThreeChar = "abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz|012|123|234|345|456|567|678|789|890";
			String[] arrThreeChar = listThreeChar.split("\\|");
			for (String s : arrThreeChar) {
				if (pwd.toLowerCase().matches(".*" + s + ".*")) {
					return false;
				}
			}
			/**
			 * 3자리 이상 연속된 키보드 자판배열 숫자/문자
			 */
//            String listKeyboardThreeChar = "qwe|wer|ert|rty|tyu|yui|uio|iop|asd|sdf|dfg|fgh|ghj|hjk|jkl|zxc|xcv|cvb|vbn|bnm|qaz|wsx|edc|rfv|tgb|yhn|ujm|esz|rdx|tfc|ygv|uhb|ijn|okm";
			String listKeyboardThreeChar = "qwe|wer|ert|rty|tyu|yui|uio|iop|asd|sdf|dfg|fgh|ghj|hjk|jkl|zxc|xcv|cvb|vbn|bnm";
			String[] arrKeyboardThreeChar = listKeyboardThreeChar.split("\\|");
			for (String s : arrKeyboardThreeChar) {
				if (pwd.toLowerCase().matches(".*" + s + ".*")) {
					return false;
				}
			}
		} else {
			return false;
		}

		return true;
	}

	public static boolean isValidPasswordWithUserInfo(UserVO user, String pwd) {
		boolean isValid = isValidPassword(pwd);
		if (isValid) {
			if (user != null) {
				/**
				 * userId 포함여부 체크
				 */
				String userId = user.getUserId();
				if (StringUtils.isNotBlank(userId)) {
					// 이메일은 앞부분만
					if (userId.indexOf("@") > -1) {
						userId = StringUtils.split(userId, "@")[0];
					}
					if (StringUtils.containsIgnoreCase(pwd, userId)) {
						isValid = false;
					}
				}
			} else {
				isValid = false;
			}
		}

		return isValid;
	}

	public static boolean isValidPasswordWithEmail(String email, String pwd) {
		boolean isValid = isValidPassword(pwd);
		if (isValid) {
			if (email != null) {
				if (StringUtils.isNotBlank(email)) {
					// 이메일은 앞부분만
					if (email.indexOf("@") > -1) {
						email = StringUtils.split(email, "@")[0];
					}
					if (StringUtils.containsIgnoreCase(pwd, email)) {
						isValid = false;
					}
				}
			} else {
				isValid = false;
			}
		}

		return isValid;
	}

	public static boolean isBase64Encoded(String str) {
		return Utils.isBase64EncodedStr(str);
//		return BASE64_ENCODED_STRING.matcher(str).matches();
	}

	public static boolean isBase64EncodedRegEx(String str) {
		return BASE64_ENCODED_STRING.matcher(str).matches();
	}

	public static HttpServletRequest getCurrentRequest(){
		HttpServletRequest request = null;
		try {
			ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			request = sra.getRequest();
		} catch (IllegalStateException e) {
			log.error("getCurrentRequest : IllegalStateException", e);
		}

		return request;
	}

	public static String getClientIp() {
		return Utils.getClientIp(Utils.getCurrentRequest());
	}

	public static String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("x-real-ip");
		log.debug("============================ : 0 : " + ip);

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Forwarded-For");
			log.debug("============================ : 1 : " + ip);
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
			log.debug("============================ : 2 : " + ip);
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
			log.debug("============================ : 3 : " + ip);
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
			log.debug("============================ : 4 : " + ip);
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
			log.debug("============================ : 5 : " + ip);
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
			log.debug("============================ : 6 : " + ip);
		}

		return ip;
	}

	public static String getRegistryUrlWithoutProtocol(String registryUrl) {
		return StringUtils.replacePattern(registryUrl, "^(?i)(https?|ftp)://", "");
	}

	public static Integer getInteger(String param) throws Exception {
		if(StringUtils.isBlank(param)) {
			return null;
		}

		Integer val = null;
		if (Pattern.matches("^[0-9]+$", param)) {
			val = Integer.valueOf(param);
		}

		return val;
	}

	/**
	 * Decompress GZip from String Data
	 * @param data
	 * @return
	 */
	public static String decompressGZipFromString(String data) throws Exception {
		return Utils.decompressGZipFromString(data, false, "UTF-8");
	}

	public static String decompressGZipFromString(String data, boolean useLinefeed, String characterSet) throws Exception {
		byte[] bytes = null;
		try {
			if (Utils.isBase64EncodedRegEx(data)) {
				log.debug("######## 1. base64 encoded data!");
				bytes = Base64.getDecoder().decode(data);
			}
			else {
				log.debug("######## 1. not base64 encoded data!");
				bytes = data.getBytes();
			}
			return Utils.decompressGZipFromBytes(bytes, useLinefeed, characterSet);
		}
		catch (ZipException ex) {
			try {
				log.error("not a gzip compressed format.1");
				return new String(bytes);
			}
			catch (Exception ex2) {
				return null;
			}
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Decompress GZip from Byte Array
	 * @param bytes
	 * @return
	 */
	public static String decompressGZipFromBytes(byte[] bytes) throws Exception  {
		return Utils.decompressGZipFromBytes(bytes, false, "UTF-8");
	}

	public static String decompressGZipFromBytes(byte[] bytes, boolean useLinefeed, String characterSet) throws Exception {
		try {
			log.debug("######## 2. start decompress!");
			String lineFeed = "";
			if (useLinefeed == true) lineFeed = "\n";
			GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
			BufferedReader bf = new BufferedReader(new InputStreamReader(gis, characterSet));

			String readLine;
			StringBuffer outStr = new StringBuffer();
			while ((readLine = bf.readLine()) != null) {
				outStr.append(readLine);
				if (useLinefeed == true) {
					outStr.append(lineFeed);
				}
			}
			log.debug(String.format("######## 3. end decompress!\n===================================\n%s",outStr));
			return outStr.toString();
		}
		catch (ZipException ex) {
			try {
				log.error("not a gzip compressed format.2");
				return new String(bytes);
			}
			catch (Exception ex2) {
				return null;
			}
		}
		catch (Exception ex) {
			return null;
		}
	}

	public static String getNowDateTime(String dataTimeFormat, String zone) {
		DateTime date = DateTime.now();
		if (StringUtils.isNotBlank(zone)) {
			date = DateTime.now(DateTimeZone.forID(zone));
		}
		return date.toString(dataTimeFormat);
	}

	public static String getNowDateTime(String dataTimeFormat) {
		return getNowDateTime(dataTimeFormat, "UTC");
	}

	public static String getNowDateTime() {
		return getNowDateTime(DEFAULT_DATE_TIME_FORMAT);
	}

	public static OffsetDateTime toOffsetDateTime(String dateTimeStr) {
		if (StringUtils.isNotBlank(dateTimeStr)) {
			return toOffsetDateTime(dateTimeStr, DEFAULT_DATE_TIME_FORMAT);
		}
		return null;
	}

	public static OffsetDateTime toOffsetDateTime(String dateTimeStr, String dateFormat) {
		if (StringUtils.isNotBlank(dateTimeStr)) {
			return OffsetDateTime.of(LocalDateTime.parse(dateTimeStr, java.time.format.DateTimeFormatter.ofPattern(dateFormat)), OffsetDateTime.now().getOffset());
		}
		return null;
	}

	public static boolean isValidUrlHttp(String url) {
		return Pattern.matches("^(?i)(https?)://.*$", url) && isValidUrl(url, new String[]{"http", "https"});
	}

	public static boolean isValidUrl(String url) throws Exception {
		return isValidUrl(url, null);
	}

	public static boolean isValidUrl(String url, String[] schemes) {
		UrlValidator urlValidator;

		if (schemes != null) {
			urlValidator = new UrlValidator(schemes);
		} else {
			urlValidator = UrlValidator.getInstance();
		}

		return urlValidator.isValid(url);
	}

	public static String getUseYn(String useYn) {
		if (StringUtils.isNotBlank(useYn)) {
			if (StringUtils.equalsAnyIgnoreCase(useYn, "Y", "N")) {
				useYn = StringUtils.upperCase(useYn);
			} else {
				useYn = null;
			}
		} else {
			useYn = null;
		}

		return useYn;
	}


	public static boolean isValidEmail(String email) {

		if (StringUtils.isNotBlank(email)) {
			/**
			 * https://www.freeformatter.com/java-regex-tester.html
			 * https://fightingforalostcause.net/content/misc/2006/compare-email-regex.php
			 */
			if (!Pattern.compile("^[-a-z0-9~!$%^&*_=+}{\\'?]+(\\.[-a-z0-9~!$%^&*_=+}{\\'?]+)*@([a-z0-9_][-a-z0-9_]*(\\.[-a-z0-9_]+)*\\.(aero|arpa|biz|com|coop|edu|gov|info|int|mil|museum|name|net|org|pro|travel|mobi|[a-z][a-z])|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,5})?$").matcher(email).matches()) {
				return false;
			}

		} else {
			return false;
		}

		return true;
	}
}
