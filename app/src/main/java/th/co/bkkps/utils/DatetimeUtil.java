package th.co.bkkps.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DatetimeUtil {
    public static final String PATTERN_UNIQUEID = "yyyyMMddHHmmssSSS";
    public static final String PATTERN_DATEONLY = "yyyyMMdd";
    public static final String PATTERN_TIMEONLY = "HHmmss";

    public static String getCurrentDateTimeAsString(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        String formattedString = dateFormat.format(new Date());

        return formattedString;
    }
}
