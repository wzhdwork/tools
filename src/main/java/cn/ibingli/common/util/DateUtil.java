/*
 * Copyright (c) 2004-2014 by UCweb INFORMATION TECHNOLOGY CO.
 *             All rights reserved
 */
package cn.ibingli.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * @Description:Date工具类
 * @author <a href="mailto:tongyiwzh@qq.com">wuzh</a>
 * @since 2013-11-27
 * @version 1.0.0
 */
public class DateUtil {
	private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DateUtil.class);

	/** 时间格式 **/
	public static final String DAY = "yyyy-MM-dd";
	public static final String DAY2 = "yyyy/M/dd";
	public static final String DAY3 = "yyyyMMdd";
	public static final String DAY4 = "yyyy/M/d";
	public static final String TIME = "HH:mm:ss";
	public static final String DATETIME = "yyyy-MM-dd HH:mm:ss";
	public static final String DATETIME2 = "yyyy-MM-dd HH:mm:ss ss";
	public static final String DATETIME3 = "yyyyMMddHHmmss";
	public static final String DATETIME4 = "MM月dd日HH:mm";
	public static final String MONTH = "yyyy-MM";
	public static final String MONTH2 = "yyyyMM";
	public static final String MIN = "yyyy-MM-dd HH:mm";
	public static final String DATE = "MM月dd日";
	public static final String CN_YMD = "yyyy年MM月dd日";

	public static final Integer HOUR_MILSECOND = 60 * 60 * 1000;

	public static Date getDate(int year,int month,int day){
		LocalDate localDate = LocalDate.of(year, month, day);
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * 获取前N个月数组
	 *
	 * @param n
	 *            月数
	 * @return List<String> 返回N个月日期队列
	 */
	public static List<String> getPreMonths(int n) {
		if (n <= 0) {
			return null;
		}
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			result.add(getPreMoth(i));
		}
		return result;
	}



	/**
	 * 获取两个日期之前的所有日期集合
	 * 
	 * @param startYear
	 * @return
	 */
	public static List<String> getDateList(Integer startYear, Integer startMonth, Integer startDay, Integer endYear,
			Integer endMonth, Integer endDay) {

		Calendar start = Calendar.getInstance();
		start.set(startYear, startMonth, startDay);
		Long startTime = start.getTimeInMillis();

		Calendar end = Calendar.getInstance();
		end.set(endYear, endMonth, endDay);
		Long endTime = end.getTimeInMillis();

		Long oneDay = 1000 * 60 * 60 * 24l;

		Long time = startTime;
		List<String> list = new ArrayList<String>();
		while (time <= endTime) {
			list.add(DateUtil.format(new Date(time), DateUtil.DAY));
			time += oneDay;
		}
		return list;
	}

	/**
	 * 获取前N月数据
	 *
	 * @param pre
	 *            前N个月
	 * @return String 返回前N个月数据
	 */
	public static String getPreMoth(int pre) {
		if (pre < 0) {
			return null;
		}
		Calendar begin = Calendar.getInstance();
		begin.set(Calendar.MONTH, begin.get(Calendar.MONTH) - pre + 1);
		begin.set(Calendar.DAY_OF_MONTH, 0);
		begin.set(Calendar.HOUR_OF_DAY, 0);
		begin.set(Calendar.MINUTE, 0);
		begin.set(Calendar.SECOND, 0);
		return format(begin.getTime(), MONTH);
	}

	/**
	 * 获取当前天
	 *
	 * @return String
	 */
	public static String getCurDayStr() {
		return new SimpleDateFormat(DAY).format(new Date());
	}

	/**
	 * 获取当前天
	 *
	 * @return String
	 */
	public static String getCurTimeStr() {
		return new SimpleDateFormat(DATETIME).format(new Date());
	}

	public static String getCurMinStr() {
		return new SimpleDateFormat(MIN).format(new Date());
	}

	/**
	 * 获取前一个月
	 *
	 * @param date
	 *            当前时间
	 * @return Date 前一个月时间
	 */
	public static Date getPreviousMonth(Date date) {
		Calendar cd = Calendar.getInstance();
		cd.setTime(date);
		cd.set(Calendar.MONTH, cd.get(Calendar.MONTH) - 1);
		return cd.getTime();
	}

	/**
	 * 获取前一天
	 *
	 * @return Date
	 */
	public static Date getPreviousDate() {
		Calendar begin = Calendar.getInstance();
		begin.set(Calendar.DAY_OF_MONTH, begin.get(Calendar.DAY_OF_MONTH) - 1);
		begin.set(Calendar.HOUR_OF_DAY, 0);
		begin.set(Calendar.MINUTE, 0);
		begin.set(Calendar.SECOND, 0);
		return begin.getTime();
	}

	/**
	 * 获取下N天
	 *
	 * @param date
	 *            当前时间
	 * @param n
	 *            下N天
	 * @return Date 下N天日期
	 */
	public static Date getNextDate(Date date, int n) {
		Calendar begin = Calendar.getInstance();
		begin.setTime(date);
		begin.set(Calendar.DAY_OF_MONTH, begin.get(Calendar.DAY_OF_MONTH) + n);
		begin.set(Calendar.HOUR_OF_DAY, 0);
		begin.set(Calendar.MINUTE, 0);
		begin.set(Calendar.SECOND, 0);
		begin.set(Calendar.MILLISECOND, 0);
		return begin.getTime();
	}

	/**
	 * 获取年度第一个月
	 *
	 * @param date
	 *            当前时间
	 * @return String 当前时间的年度第一个月
	 */
	public static String getFirstMonth(Date date) {
		Calendar begin = Calendar.getInstance();
		begin.setTime(date);
		begin.set(Calendar.MONTH, 0);
		return format(begin.getTime(), DateTimeType.Month);
	}

	/**
	 * 获取时间戳
	 *
	 * @param time
	 *            当前时间
	 * @return long 当前时间的时间戳
	 */
	public static long getTimeStamp(Date time) {
		return null == time ? 0 : time.getTime();
	}

	/**
	 * 检查时间字符串与日期格式是否相符
	 *
	 * @param str
	 *            时间字符串
	 * @param sdf
	 *            日期格式字符串
	 * @return boolean true：时间字符串与日期格式相符；false：不符
	 */
	public static boolean isParse(String str, String sdf) {
		try {
			return null != parse(str, sdf);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 解析字符串(yyyy-MM-dd HH:mm:ss)
	 *
	 * @param str
	 *            时间字符串
	 * @return Date 时间对象
	 * @throws ParseException
	 */
	public static Date parse(String str) throws ParseException {
		if (StringUtils.isEmpty(str))
			return null;
		return new SimpleDateFormat(DATETIME).parse(str);
	}

	/**
	 * 按照日期格式，解析时间字符串
	 *
	 * @param str
	 *            时间字符串
	 * @param sdf
	 *            日期格式
	 * @return Date 时间对象
	 * @throws ParseException
	 */
	public static Date parse(String str, String sdf) throws ParseException {
		if (StringUtils.isEmpty(str) || StringUtils.isEmpty(sdf)) {
			return null;
		}

		return new SimpleDateFormat(sdf).parse(str);
	}

	/**
	 * 按照日期格式枚举，解析时间字符串
	 *
	 * @param str
	 *            时间字符串
	 * @param type
	 *            日期格式枚举类型
	 * @return Date 时间对象
	 * @throws ParseException
	 */
	public static Date parse(String str, DateTimeType type) throws ParseException {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		switch (type) {
		case DateTime: {
			return new SimpleDateFormat(DATETIME).parse(str);
		}
		case Day: {
			return new SimpleDateFormat(DAY).parse(str); // SimpleDateFormat有线程安全问题，需要独享
		}
		case Time: {
			return new SimpleDateFormat(TIME).parse(str);
		}
		case CNDate: {
			return new SimpleDateFormat(DATE).parse(str);
		}
		default: {
			return new SimpleDateFormat(DATETIME).parse(str);
		}
		}
	}

	/**
	 * 格式化日期, 根据用户传进来的格式
	 *
	 * @param date
	 *            时间对象
	 * @param sdf
	 *            日期格式字符串
	 * @return 格式化字符串
	 */
	public static String format(Date date, String sdf) {
		if (date == null) {
			return "";
		}
		return new SimpleDateFormat(sdf).format(date);
	}

	/**
	 * 格式化时间戳
	 *
	 * @param timestamp
	 *            时间戳
	 * @return String 格式化字符串
	 */
	public static String format(long timestamp) {
		if (timestamp < 0) {
			return "";
		}
		return format(new Date(timestamp), DateTimeType.DateTime);
	}

	/**
	 * 格式化时间戳
	 *
	 * @param timestamp
	 *            时间戳
	 * @param sdf
	 *            日期格式
	 * @return String 格式化字符串
	 */
	public static String format(long timestamp, String sdf) {
		if (timestamp < 0) {
			return "";
		}
		return new SimpleDateFormat(sdf).format(new Date(timestamp));
	}

	/**
	 * 格式化时间戳
	 *
	 * @param timestamp
	 *            时间戳
	 * @param type
	 *            日期格式枚举类型
	 * @return String 格式化字符串
	 */
	public static String format(long timestamp, DateTimeType type) {
		if (timestamp < 0) {
			return "";
		}
		return format(new Date(timestamp), type);
	}

	/**
	 * 按照默认日期格式，格式化日期
	 *
	 * @param date
	 *            时间对象
	 * @return String 格式化字符串
	 */
	public static String format(Date date) {
		return format(date, DateTimeType.DateTime);
	}

	/**
	 * 格式化日期
	 *
	 * @param date
	 *            时间对象
	 * @param type
	 *            日期格式枚举
	 * @return String 格式化字符串
	 */
	public static String format(Date date, DateTimeType type) {
		if (null == date) {
			return "";
		}
		switch (type) {
			case DateTime: {
				return new SimpleDateFormat(DATETIME).format(date); // SimpleDateFormat有线程安全问题，需要独享
			}
			case Time: {
				return new SimpleDateFormat(TIME).format(date);
			}
			case Day: {
				return new SimpleDateFormat(DAY).format(date);
			}
			case CNDate: {
				return new SimpleDateFormat(DATE).format(date);
			}
			case Month: {
				return new SimpleDateFormat(MONTH).format(date);
			}
			default: {
				return new SimpleDateFormat(DATETIME).format(date);
			}
		}
	}

	/**
	 * 项目标准返回
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDateProperty(Date date) {
		if (null == date) {
			return "";
		}

		return new SimpleDateFormat(DATETIME).format(date); // SimpleDateFormat有线程安全问题，需要独享
	}

	/**
	 * 获取一天起始时间
	 *
	 * @param date
	 *            当前时间
	 * @return Date 起始时间
	 */
	public static Date getFisrtTime(Date date) {
		if (date == null) {
			return null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取一天最后时间
	 *
	 * @param date
	 *            当前时间
	 * @return Date 起始时间
	 */
	public static Date getLastTime(Date date) {
		if (date == null) {
			return null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	/**
	 * 获取一天结束时间
	 *
	 * @param date
	 *            当前时间
	 * @return Date 结束时间
	 */
	public static Date getEndTime(Date date) {
		if (date == null) {
			return null;
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取天
	 *
	 * @param date
	 *            当前时间
	 * @return Date 天
	 */
	public static Date getDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取年月
	 *
	 * @param date
	 *            当前日期
	 * @return int 月份
	 */
	public static int getYearMonth(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1); // 获取月份
	}

	/**
	 * 间隔天数，当天算1天，可能返回负数
	 * @param startDate
	 * @param endDate
	 * @return
	 */
    public static int daysBetween(Date startDate, Date endDate)
    {
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        try {
            startDate=sdf.parse(sdf.format(startDate));
            endDate=sdf.parse(sdf.format(endDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        long startTime = cal.getTimeInMillis();
        cal.setTime(endDate);
        long endTime = cal.getTimeInMillis();
        long between_days=(endTime-startTime)/(1000*3600*24);

        if(between_days == 0)
            return 1;
        else
            return Integer.parseInt(String.valueOf(between_days));
    }
    
    /**
     * 时间间隔（包含开始和结束当天）,返回可以是负数
     * 例如:
     * 		startDate:2018-04-02 endDate:2018-04-02,返回 1
     * 		startDate:2018-04-02 endDate:2018-04-03,返回 2
     * 		startDate:2018-04-02 endDate:2018-04-01,返回 -1
     * 		startDate:2018-04-03 endDate:2018-04-01,返回 -2
     * 注：返回天数没有  0；
     * @param startDate
     * @param endDate
     * @return
     */
    public static int daysInterval(Date startDate, Date endDate){
    	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        try {
            startDate=sdf.parse(sdf.format(startDate));
            endDate=sdf.parse(sdf.format(endDate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        long startTime = cal.getTimeInMillis();
        cal.setTime(endDate);
        long endTime = cal.getTimeInMillis();
        long between_days=(endTime-startTime)/(1000L*3600L*24L);
        if(between_days>0){
        	return Integer.parseInt(String.valueOf(between_days));
        }else {
        	return Integer.parseInt(String.valueOf(between_days))-1;
        }
    }

    /**
	 * @description 时间枚举
	 */
	public enum DateTimeType {
		DateTime(0, DATETIME), // 日期时间
		Day(1, DAY), // 日期 yyyy-MM-dd
		Time(2, TIME), // 时间 HH:mm:ss
		Month(3, MONTH), // 年月
		CNDate(4, DATE),// 中文时间格式
		CNYMD(5, CN_YMD);

		private int index;
		private String formatStr;

		private DateTimeType(int index, String formatStr) {
			this.index = index;
			this.formatStr = formatStr;
		}

		public int getIndex() {
			return index;
		}

		public String getFormatStr() {
			return formatStr;
		}
	}

	/**
	 * 返回yyyy/M/d格式信息
	 *
	 * @return String
	 */
	public static String getImgDateInfo(Long date) {
		return new SimpleDateFormat(DAY4).format(date);
	}

	/**
	 * 获取距离下一个时间周期的时间距离（单位s）
	 *
	 * @param destTimeStr
	 *            HH:mm:ss
	 * @return long
	 */
	public static long getHourDelay(String destTimeStr) {
		if (StringUtils.isEmpty(destTimeStr)) {
			return -1;
		}
		Date destDate = null;
		try {
			destDate = parse(destTimeStr, "HH:mm:ss");
		} catch (ParseException e) {
			// Logger.error(e, "[DateUtil getDelay] error.");
			return -1;
		}
		Calendar now = Calendar.getInstance();
		now.setTime(new Date());
		Calendar dest = Calendar.getInstance();
		dest.setTime(destDate);
		dest.set(Calendar.YEAR, now.get(Calendar.YEAR));
		dest.set(Calendar.MONTH, now.get(Calendar.MONDAY));
		dest.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
		long delay = dest.getTimeInMillis() - now.getTimeInMillis();
		if (delay < 0) {
			dest.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH) + 1);
			delay = dest.getTimeInMillis() - now.getTimeInMillis();
		}
		return delay / 1000;
	}

	/**
	 * delayHour小时后的时间
	 *
	 * @param now
	 * @param delayHour
	 * @return Long
	 */
	public static Long getHourDelay(Long now, Integer delayHour) {
		// 为了不报空值，设置一个当前时间
		Long result = new Date().getTime();
		if (null != now && null != delayHour) {
			result = now + delayHour * HOUR_MILSECOND;
		}
		return result;
	}

	/**
	 * 获取随机延迟时间，秒
	 * 
	 * @param cycle
	 *            秒
	 * @param min
	 * @return long
	 */
	public static long getRandomDelay(long cycle, long min) {
		long delay = min + Math.round(Math.random() * Math.min(cycle, 300));
		return delay;
	}

	/**
	 * 添加秒数
	 *
	 * @return Date
	 */
	public static Date addSecond(Long date, int second) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(date);
		cal.add(Calendar.SECOND, second);
		return cal.getTime();
	}

	/**
	 * 添加小时
	 * 
	 * @param date
	 * @param hour
	 * @return Date
	 */
	public static Date addHour(Long date, int hour) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(date);
		cal.add(Calendar.HOUR_OF_DAY, hour);
		return cal.getTime();
	}

	public static Date addMinutes(Date date, int minutes) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, minutes);
		return cal.getTime();
	}

	/**
	 * 添加天
	 * 
	 * @param date
	 * @param day
	 * @return Date
	 */
	public static Date addDay(Date date, int day) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DAY_OF_YEAR, day);
		return cal.getTime();
	}

	/**
	 * 添加天数
	 * 
	 * @param date
	 * @param hour
	 *            时间长度，以毫秒为基准
	 */
	public static long TIME_SECOND = 1000L;
	public static long TIME_MINUTE = 60 * TIME_SECOND;
	public static long TIME_HOUR = 60 * TIME_MINUTE;
	public static long TIME_DAY = 24 * TIME_HOUR;
	public static long TIME_WEEK = 7 * TIME_DAY;
	public static long TIME_MONTH = 30 * TIME_DAY;
	public static long TIME_YEAR = 365 * TIME_DAY;

	/**
	 * 时间字符串转换,单位y M w d h m s
	 * 
	 * @param input
	 * @return long
	 */
	public static long timeParse(String input) {
		return timeParse(input, "");
	}

	/**
	 * 时间字符串转换,单位y M w d h m s
	 * 
	 * @param input
	 * @param tu
	 *            返回单位，默认为毫秒
	 * @return long
	 */
	public static long timeParse(String input, String tu) {
		if (StringUtils.isEmpty(input)) {
			return 0L;
		}
		input = input.trim();
		String unit = input.substring(input.length() - 1, input.length());
		long time = NumberUtils.toLong(input.substring(0, input.length() - 1));
		if (time <= 0) {
			return 0L;
		}
		if ("y".equals(unit)) {
			time = time * TIME_YEAR;
		} else if ("M".equals(unit)) {
			time = time * TIME_MONTH;
		} else if ("w".equals(unit)) {
			time = time * TIME_WEEK;
		} else if ("d".equals(unit)) {
			time = time * TIME_DAY;
		} else if ("h".equals(unit)) {
			time = time * TIME_HOUR;
		} else if ("m".equals(unit)) {
			time = time * TIME_MINUTE;
		} else if ("s".equals(unit)) {
			time = time * TIME_SECOND;
		}
		if ("y".equals(tu)) {
			time = time / TIME_YEAR;
		} else if ("M".equals(tu)) {
			time = time / TIME_MONTH;
		} else if ("w".equals(tu)) {
			time = time / TIME_WEEK;
		} else if ("d".equals(tu)) {
			time = time / TIME_DAY;
		} else if ("h".equals(tu)) {
			time = time / TIME_HOUR;
		} else if ("m".equals(tu)) {
			time = time / TIME_MINUTE;
		} else if ("s".equals(tu)) {
			time = time / TIME_SECOND;
		}
		return time;
	}

	/**
	 * 获取所有的时区编号. <br>
	 * 排序规则:按照ASCII字符的正序进行排序. <br>
	 * 排序时候忽略字符大小写.
	 * 
	 * @return 所有的时区编号(时区编号已经按照字符[忽略大小写]排序).
	 */
	public static String[] fecthAllTimeZoneIds() {
		Vector v = new Vector();
		String[] ids = TimeZone.getAvailableIDs();
		for (int i = 0; i < ids.length; i++) {
			v.add(ids[i]);
		}
		java.util.Collections.sort(v, String.CASE_INSENSITIVE_ORDER);
		v.copyInto(ids);
		v = null;
		return ids;
	}

	/**
	 * 将日期时间字符串根据转换为指定时区的日期时间.
	 * 
	 * @param srcFormater
	 *            待转化的日期时间的格式.
	 * @param srcDateTime
	 *            待转化的日期时间.
	 * @param dstFormater
	 *            目标的日期时间的格式.
	 * @param dstTimeZoneId
	 *            目标的时区编号.
	 * 
	 * @return 转化后的日期时间.
	 */
	public static String string2Timezone(String srcFormater, String srcDateTime, DateTimeType dstFormater,
			String dstTimeZoneId) {
		if (srcFormater == null || "".equals(srcFormater))
			return null;
		if (srcDateTime == null || "".equals(srcDateTime))
			return null;
		if (dstFormater == null || "".equals(dstFormater))
			return null;
		if (dstTimeZoneId == null || "".equals(dstTimeZoneId))
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat(srcFormater);
		try {
			int diffTime = getDiffTimeZoneRawOffset(dstTimeZoneId);
			Date d = sdf.parse(srcDateTime);
			long nowTime = d.getTime();
			long newNowTime = nowTime - diffTime;
			d = new Date(newNowTime);
			return format(d, dstFormater);
		} catch (ParseException e) {
			logger.error("[DateUtil string2Timezone] error:", e);
			return null;
		} finally {
			sdf = null;
		}
	}

	/**
	 * 获取系统当前默认时区与UTC的时间差.(单位:毫秒)
	 * 
	 * @return 系统当前默认时区与UTC的时间差.(单位:毫秒)
	 */
	private static int getDefaultTimeZoneRawOffset() {
		return TimeZone.getDefault().getRawOffset();
	}

	/**
	 * 获取指定时区与UTC的时间差.(单位:毫秒)
	 * 
	 * @param timeZoneId
	 *            时区Id
	 * @return 指定时区与UTC的时间差.(单位:毫秒)
	 */
	private static int getTimeZoneRawOffset(String timeZoneId) {
		return TimeZone.getTimeZone(timeZoneId).getRawOffset();
	}

	/**
	 * 获取系统当前默认时区与指定时区的时间差.(单位:毫秒)
	 * 
	 * @param timeZoneId
	 *            时区Id
	 * @return 系统当前默认时区与指定时区的时间差.(单位:毫秒)
	 */
	private static int getDiffTimeZoneRawOffset(String timeZoneId) {
		return TimeZone.getDefault().getRawOffset() - TimeZone.getTimeZone(timeZoneId).getRawOffset();
	}

	/**
	 * 将日期时间字符串根据转换为指定时区的日期时间.
	 * 
	 * @param srcDateTime
	 *            待转化的日期时间.
	 * @param dstTimeZoneId
	 *            目标的时区编号.
	 * 
	 * @return 转化后的日期时间.
	 * @see #string2Timezone(String, String, String, String)
	 */
	public static String string2TimezoneDefault(String srcDateTime, String dstTimeZoneId) {
		return string2Timezone(DATETIME, srcDateTime, DateTimeType.DateTime, dstTimeZoneId);
	}

	/**
	 * 得到UTC时间，类型为字符串，格式为"yyyy-MM-dd HH:mm"<br />
	 * 如果获取失败，返回null
	 * 
	 * @return
	 */
	public static String getUTCTimeStr() {
		StringBuffer UTCTimeBuffer = new StringBuffer();
		// 1、取得本地时间：
		Calendar cal = Calendar.getInstance();
		// 2、取得时间偏移量：
		int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
		// 3、取得夏令时差：
		int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
		// 4、从本地时间里扣除这些差量，即可以取得UTC时间：
		cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		UTCTimeBuffer.append(year).append("-");
		if (month > 9) {
			UTCTimeBuffer.append(month);
		} else {
			UTCTimeBuffer.append("0").append(month);
		}
		UTCTimeBuffer.append("-");

		if (day > 9) {
			UTCTimeBuffer.append(day);
		} else {
			UTCTimeBuffer.append("0").append(day);
		}
		UTCTimeBuffer.append(" ");

		if (hour > 9) {
			UTCTimeBuffer.append(hour);
		} else {
			UTCTimeBuffer.append("0").append(hour);
		}
		UTCTimeBuffer.append(":");

		if (minute > 9) {
			UTCTimeBuffer.append(minute);
		} else {
			UTCTimeBuffer.append("0").append(minute);
		}
		UTCTimeBuffer.append(":");

		if (second > 9) {
			UTCTimeBuffer.append(second);
		} else {
			UTCTimeBuffer.append("0").append(second);
		}

		return UTCTimeBuffer.toString();
	}

	/**
	 * 将UTC时间转换为本地时间???????????
	 * 
	 * @param UTCTime
	 * @return
	 */
	public static String getLocalTimeFromUTC(String utcTime) {
		String localTimeStr = null;
		try {
			SimpleDateFormat utcFormater = new SimpleDateFormat(DATETIME);
			utcFormater.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
			Date UTCDate = utcFormater.parse(utcTime);
			// System.out.println("==========> UTCDate:" + UTCDate); // 值已经变成本地时间
			
			
			SimpleDateFormat localFormater = new SimpleDateFormat(DATETIME);
			localFormater.setTimeZone(TimeZone.getDefault());
			localTimeStr = localFormater.format(UTCDate.getTime());
		} catch (ParseException e) {
			logger.error("[DateUtil getLocalTimeFromUTC] error:", e);
		}

		return localTimeStr;
	}

	/**
	 * 
	 * @description 将输入时间串设置为utc时间，返回的是该时间相对于本地时间的Calendar
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 * @param date
	 * @return
	 */
	public static Calendar getStringToUtcCal(String date) {
		if (date == null || date.trim().length() < 19) {
			return null;
		}
		
		String year = date.substring(0, 4);
		String month = date.substring(5, 7);
		String day = date.substring(8, 10);
		String hour = date.substring(11, 13);
		String minute = date.substring(14, 16);
		
		boolean hasMillion = date.length() >= 23;
		String second = "";
		if (hasMillion) {
			second = date.substring(17, 19);
		} else {
			second = date.substring(17).trim();
		}
		
		String millisecondStr = "0";
		if (hasMillion) {
			millisecondStr = date.substring(20, 23);
		}
		
		int millisecond = Integer.valueOf(millisecondStr);
		Calendar result = new GregorianCalendar(Integer.valueOf(year), Integer.valueOf(month) - 1, Integer.valueOf(day),
				Integer.valueOf(hour), Integer.valueOf(minute), Integer.valueOf(second));
		result.set(Calendar.MILLISECOND, millisecond);
		result.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		return result;
	}

	/**
	 * 获取本月第一天
	 * 
	 * 历史： Java中的月份遵循了罗马历中的规则：当时一年中的月份数量是不固定的；
	 *      Java中Calendar.MONTH返回的数值其实是当前月距离第一个月有多少个月份的数值；
	 *      
	 * 
	 * @return
	 */
    public static Date getFirstDayOfMonth() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.DATE, now.getActualMinimum(Calendar.DATE));
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        return now.getTime();
    }

    /**
     * 获取本月第一周第一天
     * 
     * @return
     */
    public static Date getFirstDayOfFirstWeekOfMonth() {
        Calendar now = Calendar.getInstance();
        // 按照中国人的习惯，将周一设置为每周第一天
        now.setFirstDayOfWeek(Calendar.MONDAY);
        now.setTime(getFirstDayOfMonth()); // 设置基数时间

        int i = 1;
        while (now.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        	// 在基数时间上递增当月的 day，直到为当周第一天
            now.set(Calendar.DAY_OF_MONTH, i++); // 设置这个月的星期1 为几号
        }

        return now.getTime();
    }
    
    
    /**
     * 获取本月第一周第一天
     * 
     * @return
     */
    public static Date[] getLastMonthBeginTime(Date referTime) {
        Calendar time = Calendar.getInstance();
        if (referTime == null) {
        	// 以此时为参考时间
        	referTime = new Date();
        }
        time.setTime(referTime);
        
        time.add(Calendar.MONTH, -1);
        time.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = time.getTime();
        
        time.add(Calendar.MONTH, 1);
        time.set(Calendar.DAY_OF_MONTH, 1);
        Date endDate = time.getTime();

        Date[] beginEndTime = new Date[2];
        beginEndTime[0] = startDate;
        beginEndTime[1] = endDate;
        
        return beginEndTime;
    }

    public static String secToTime(int time) {
		String timeStr = null;
		int hour = 0;
		int minute = 0;
		int second = 0;
		if (time <= 0) {
			return "00:00";
		} else {
			minute = time / 60;
			if (minute < 60) {
				second = time % 60;
				timeStr = unitFormat(minute) + ":" + unitFormat(second);
			} else {
				hour = minute / 60;
				// 2019-06-10 wuzh
//				if (hour > 99) {
//					return "99:59:59";
//				}
				minute = minute % 60;
				second = time - hour * 3600 - minute * 60;
				timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
			}
		}
		
		return timeStr;
	}

	public static String unitFormat(int i) {
		String retStr = null;
		if (i >= 0 && i < 10) {
			retStr = "0" + Integer.toString(i);
		} else {
			retStr = "" + i;
		}
		
		return retStr;
	}
	
	public static String formatTimeDescrBySeconds(long secondLen) {
		String DateTimes = null;
		long days = secondLen / ( 60 * 60 * 24);
		long hours = (secondLen % ( 60 * 60 * 24)) / (60 * 60);
		long minutes = (secondLen % ( 60 * 60)) /60;
	    long seconds = secondLen % 60;
	    if (days > 0) {
	        DateTimes= days + "天" + hours + "小时" + minutes + "分钟" + seconds + "秒"; 
	    } else if (hours > 0) {
	    	DateTimes = hours + "小时" + minutes + "分钟" + seconds + "秒"; 
	    } else if (minutes > 0) {
	    	DateTimes = minutes + "分钟" + seconds + "秒"; 
	    } else {
	    	DateTimes = seconds + "秒";
	    }
	  
	    return DateTimes;
	}
    
    
    /**
     * 获取时间段内的时间(正序)
     * @param dateStart  2019-01-01
     * @param dateEnd	 2019-01-31
     * @return  [2019-01-01 2019-01-02 ....... 2019-01-31]
     */
    public static List<String> getDaysByPeriod(String fromDate, String toDate) {
		SimpleDateFormat sdf = new SimpleDateFormat(DAY);
		List<String> dateList = new ArrayList<String>();
		dateList.add(fromDate);
		try {
			Date dateBegin = sdf.parse(fromDate);
			Date dateEnd = sdf.parse(toDate);

			Calendar calBegin = Calendar.getInstance();
			calBegin.setTime(dateBegin);
			Calendar calEnd = Calendar.getInstance();
			calEnd.setTime(dateEnd);

			while (dateEnd.after(calBegin.getTime())) {
				calBegin.add(Calendar.DAY_OF_MONTH, 1); 
				dateList.add(sdf.format(calBegin.getTime()));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return dateList;
	}
    
    public static String fillFullTime(String time) {
    	if (StringUtils.isBlank(time)) {
    		return null;
    	}
    	
    	time = time.trim();
    	if (time.length() == 19) {
    		return time;
    	} else if (time.length() == 16) {
			return time + ":00";
		} else if (time.length() == 13) {
			return time + ":00:00";
		} else if (time.length() == 10) {
			return time + " 00:00:00";
		} else {
			throw new RuntimeException("日期格式不正确");
		}
    }
    
	public static void main(String[] args) throws Exception {
		// String[] zoneIds = fecthAllTimeZoneIds();
		// for (String zoneId : zoneIds) {
		// System.out.println("============> zoneId:" + zoneId);
		// }
//		System.out.println("============> getUTCTimeStr:" + getUTCTimeStr());
//		String timeStr = getLocalTimeFromUTC("2017-04-18 14:00:00");
//		System.out.println("===========> timezone:" + TimeZone.getDefault().getID());
//		System.out.println("============> localtimeStr:" + timeStr);
//		
//		Calendar cal = getStringToUtcCal("2017-04-18 14:00:00");
//		Date date = new Date(cal.getTimeInMillis());
//		System.out.println("===============> date:" + date);
		
		
//		Date d1 = getFirstDayOfMonth();
//		System.out.println("=========> 本月第一天:" + DateUtil.format(d1));
//		
//		Date d2 = getFirstDayOfFirstWeekOfMonth();
//		System.out.println("=========> 本月第一周第一天:" + DateUtil.format(d2));
//		
//		Date date = DateUtil.parse("2017-09-11 11:00:00");
//		WeekInfoModel dateInfo = getTheDayInfo(date);
//		System.out.println("=========> 当日详情:" + GsonHelper.toJson(dateInfo));
//		
//		WeekInfoModel currentDayInfo = DateUtil.getTheDayInfo(date);
//		Date preWeekOneDay = DateUtil.addDay(currentDayInfo.getStartDateOfWeek(), -1);
//		WeekInfoModel preWeekDayInfo = DateUtil.getTheDayInfo(preWeekOneDay);
//		Date fromTime = preWeekDayInfo.getStartDateOfWeek();
//		Date toTime = preWeekDayInfo.getEndDateOfWeek();
//		
//		System.out.println("==============> preWeekOneDay:" + DateUtil.format(preWeekOneDay) + ", currentDayInfo:" + DateUtil.format(new Date()) + ", weekstart:" + DateUtil.format(currentDayInfo.getStartDateOfWeek()));
//		System.out.println("==============> fromTime:" + DateUtil.format(fromTime));
//		System.out.println("==============> toTime:" + DateUtil.format(toTime));
		
//		Integer i = Integer.parseInt("00023");
//		System.out.println("i:" + i);
//		
//		String strTime = "2019-01-31-16-32-59";
//		Date time = DateUtil.parse(strTime, "yyyy-MM-dd-HH-mm-ss");
//		System.out.println("============> time:" + time);
		
		Date referTime = DateUtil.parse("2019-08-01", DateTimeType.Day);
		// begin:2019-08-01 14:30:47, end:2019-09-01 14:30:47
		Date[] timeArr = getLastMonthBeginTime(referTime);
		System.out.println("=============> begin:" + DateUtil.format(timeArr[0], DateTimeType.DateTime) + ", end:" + DateUtil.format(timeArr[1], DateTimeType.DateTime));
	}
}
