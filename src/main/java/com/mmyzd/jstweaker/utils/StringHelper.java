package com.mmyzd.jstweaker.utils;

import com.google.common.collect.ImmutableMap;

public class StringHelper {
	
	private static final ImmutableMap<Character, String> ESCAPE_TABLE = ImmutableMap.<Character, String>builder()
			.put('\'', "\\'")
			.put('"', "\\\"")
			.put('\\', "\\\\")
			.put('\n', "\\n")
			.put('\r', "\\r")
			.put('\t', "\\t")
			.put('\b', "\\b")
			.put('\f', "\\f")
			.build();
	
	public static String escape(String s) {
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			String output = ESCAPE_TABLE.get(c);
			if (output != null) {
				ret.append(output);
			} else if (c < 0x20 || c > 0x7f) {
				ret.append("\\u");
				if (c < 0x10)   ret.append("0");
				if (c < 0x100)  ret.append("0");
				if (c < 0x1000) ret.append('0');
				ret.append(Integer.toString(c, 16));
			} else {
				ret.append(c);
			}
		}
		return ret.toString();
	}
	
}
