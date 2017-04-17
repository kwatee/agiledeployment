package net.kwatee.agiledeployment.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Audit {

	private final static Logger LOG = LoggerFactory.getLogger("audit");

	public static void log(String msg) {
		LOG.info(msg);
	}

	public static void log(String format, Object arg) {
		LOG.info(format, arg);
	}

	public static void log(String format, Object arg1, Object arg2) {
		LOG.info(format, arg1, arg2);
	}

	public static void log(String format, Object... arguments) {
		LOG.info(format, arguments);
	}

}
