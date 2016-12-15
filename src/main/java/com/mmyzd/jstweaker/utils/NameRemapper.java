package com.mmyzd.jstweaker.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import jdk.internal.dynalink.beans.StaticClass;

public class NameRemapper {
	
	private static final HashMap<String, HashSet<String>> rawMappingsField  = getRawMappings("/mappings/fields.csv");
	private static final HashMap<String, HashSet<String>> rawMappingsMethod = getRawMappings("/mappings/methods.csv");
	private static final HashMap<FieldSignature, Field> mappingsField = new HashMap<FieldSignature, Field>();
	private static final HashMap<MethodSignature, Method> mappingsMethod = new HashMap<MethodSignature, Method>();
	
	private static FieldSignature fsign = new FieldSignature();
	private static MethodSignature msign = new MethodSignature();
	private static final Class<?>[][] margs = new Class<?>[256][];
	
	static {
		for (int i = 0; i < margs.length; i++) margs[i] = new Class<?>[i];
	}
	
	public static class FieldSignature {
		public Class<?> owner;
		public String name;
	}
	
	public static class MethodSignature {
		public Class<?> owner;
		public String name;
		public Class<?>[] args;
	}
	
	public static HashMap<String, HashSet<String>> getRawMappings(String path) {
		HashMap<String, HashSet<String>> mcpToSrg = new HashMap<String, HashSet<String>>();
		try {
			InputStream stream = NameRemapper.class.getResource(path).openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = reader.readLine();
			line = reader.readLine();
			while (line != null) {
				StringTokenizer tokens = new StringTokenizer(line, ",");
				String srg = tokens.nextToken();
				String mcp = tokens.nextToken();
				HashSet<String> list = mcpToSrg.get(mcp);
				if (list == null) mcpToSrg.put(mcp, list = new HashSet<String>());
				list.add(srg);
				line = reader.readLine();
			}
		} catch (Exception e) {
		}
		return mcpToSrg;
	}

	public static Object get(Object owner, String name) throws Exception {
		if (owner instanceof StaticClass) {
			fsign.owner = ((StaticClass)owner).getRepresentedClass();
		} else {
			fsign.owner = owner.getClass();
		}
		fsign.name = name;
		Field field = mappingsField.get(fsign);
		if (field == null) {
			HashSet<String> list = rawMappingsField.get(name);
			if (list == null) throw new NoSuchFieldException(name);
			for (String srgName: list) {
				try {
					field = fsign.owner.getField(srgName);
					break;
				} catch (Exception e) {
				}
			}
			if (field == null) field = fsign.owner.getField(name);
			mappingsField.put(fsign, field);
			fsign = new FieldSignature();
		}
		return field.get(owner);
	}
	
	public static void set(Object owner, String name, Object value) throws Exception {
		if (owner instanceof StaticClass) {
			fsign.owner = ((StaticClass)owner).getRepresentedClass();
		} else {
			fsign.owner = owner.getClass();
		}
		fsign.name = name;
		Field field = mappingsField.get(fsign);
		if (field == null) {
			HashSet<String> list = rawMappingsField.get(name);
			if (list == null) throw new NoSuchFieldException(name);
			for (String srgName: list) {
				try {
					field = fsign.owner.getField(srgName);
					break;
				} catch (Exception e) {
				}
			}
			if (field == null) field = fsign.owner.getField(name);
			mappingsField.put(fsign, field);
			fsign = new FieldSignature();
		}
		field.set(owner, value);
	}
	
	public static Object invoke(Object owner, String name, Object... args) throws Exception {
		if (owner instanceof StaticClass) {
			msign.owner = ((StaticClass)owner).getRepresentedClass();
		} else {
			msign.owner = owner.getClass();
		}
		msign.name = name;
		msign.args = margs[args.length];
		for (int i = args.length - 1; i >= 0; i--) msign.args[i] = args[i].getClass();
		Method method = mappingsMethod.get(msign);
		if (method == null) {
			HashSet<String> list = rawMappingsMethod.get(name);
			if (list == null) throw new NoSuchFieldException(name);
			Method[] methods = msign.owner.getMethods();
			int maxDif = 999999999;
			for (Method m: methods) {
				Class<?>[] required = m.getParameterTypes();
				if (required.length != args.length) continue;
				if (!list.contains(m.getName()) && !name.equals(m.getName())) continue;
				int i = 0, dif = 0;
				while (i < args.length) {
					if (!required[i].isAssignableFrom(msign.args[i])) break;
					if (!required[i].equals(msign.args[i])) dif++;
					i++;
				}
				if (i == args.length && dif < maxDif) {
					method = m;
					maxDif = dif;
				}
			}
			msign.args = msign.args.clone();
			mappingsMethod.put(msign, method);
			msign = new MethodSignature();
		}
		return method.invoke(owner, args);
	}
	
}
