package com.aceattorneyonline.master.protocol;

public class AOUtils {

	public static String unescape(String str) {
		//@formatter:off
		return str.replaceAll("<percent>", "%")
				.replaceAll("<num>", "#")
				.replaceAll("<dollar>", "\\$")
				.replaceAll("<and>", "&")
				.replaceAll("\\$n", "\n")
				// Unescape doubly escaped symbols
				.replaceAll("<percent\\>", "<percent>")
				.replaceAll("<num\\>", "<num>")
				.replaceAll("<dollar\\>", "<dollar>")
				.replaceAll("<and\\>", "<and>");
		//@formatter:on
	}

}
