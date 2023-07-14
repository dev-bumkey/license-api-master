package run.acloud.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class DateTimeUtils {

	public static final String DEFAULT_DB_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss.SSSZ";

	public static final String DEFAULT_DATE_FORMAT1 = "yyyyMMddhhmmsss";

	public static String getUtcTime() {
		DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT1);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date();
		
		return dateFormat.format(date);
	}

	/**
	 * 날짜 더하기.
	 * @param date
	 * @param year
	 * @param month
	 * @param date
	 * @return
	 */
	public static Date addDate(Date date, int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.YEAR, year);
		calendar.add(Calendar.MONTH, month);
		calendar.add(Calendar.DATE, day);

		return calendar.getTime();
	}

	/**
	 * UTC Time의 문자열 포멧으로 변환.
	 * @param date
	 * @param format
	 * @return
	 */
	public static String getUtcTimeString(Date date, String format) {
		return getTimeString(date, TimeZone.getTimeZone("UTC"), format);
	}

	public static String getTimeString(Date date, String zoneId ,String format) {
		if (StringUtils.isNotBlank(zoneId)) {
			return getTimeString(date, TimeZone.getTimeZone(zoneId), format);
		} else {
			return getUtcTimeString(date, format);
		}
	}

	/**
	 * 요청한 타임존 + 포멧의 String Type 날짜 리턴.
	 * @param date
	 * @param timezone
	 * @param format
	 * @return
	 */
	public static String getTimeString(Date date, TimeZone timezone ,String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(timezone);

		return dateFormat.format(date);
	}

	/**
	 * 시, 분, 초를 모두 최소치로 초기화
	 *
	 * @param date
	 * @return
	 */
	public static Date initMinTime(Date date){
		return DateUtils.truncate(date, Calendar.DATE);
	}

	public static Date initMaxTime(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);

		return cal.getTime();
	}
}
