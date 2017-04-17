package net.kwatee.agiledeployment.common.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

	static public String currentGMT() {
		DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
		return df.format(new Date());
	}
}
