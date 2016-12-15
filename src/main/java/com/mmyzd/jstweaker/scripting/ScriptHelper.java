package com.mmyzd.jstweaker.scripting;

import jdk.internal.dynalink.beans.StaticClass;

public class ScriptHelper {

	public static Class<?> getClass(Object obj) {
		if (obj instanceof StaticClass) return ((StaticClass)obj).getRepresentedClass();
		if (obj instanceof Class) return (Class<?>)obj;
		return obj.getClass();
	}
	
}
