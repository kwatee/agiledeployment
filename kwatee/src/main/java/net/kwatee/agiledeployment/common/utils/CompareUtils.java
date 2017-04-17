/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.utils;

public class CompareUtils {

	public static boolean equals(Long a, Long b) {
		if (a == null ^ b == null)
			return false;
		if (a == null && b == null)
			return true;
		return a.equals(b);
	}

	public static boolean notEquals(Long a, Long b) {
		return !equals(a, b);
	}

	public static boolean equals(String a, String b) {
		if (a == null ^ b == null)
			return false;
		if (a == null && b == null)
			return true;
		return a.equals(b);
	}

	public static boolean notEquals(String a, String b) {
		return !equals(a, b);
	}

	public static int compareTo(String thisString, String thatString) {
		if (thisString == null ^ thatString == null) {
			return thisString == null ? -1 : 1;
		}
		if (thisString == null && thatString == null) {
			return 0;
		}
		return thisString.compareTo(thatString);
	}

	public static int compareTo(Long thisLong, Long thatLong) {
		if (thisLong == null ^ thatLong == null) {
			return thisLong == null ? -1 : 1;
		}
		if (thisLong == null && thatLong == null) {
			return 0;
		}
		return thisLong.compareTo(thatLong);
	}
}
