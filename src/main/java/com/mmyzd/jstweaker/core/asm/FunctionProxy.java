package com.mmyzd.jstweaker.core.asm;

import java.util.ArrayList;

import org.objectweb.asm.Type;

import com.google.common.collect.ImmutableMap;
import com.mmyzd.jstweaker.JSTLogger;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class FunctionProxy {
	
	public Object[] args;
	public Object ret;
	
	private int ID;
	private String type;
	private ScriptObjectMirror func;
	
	private static ArrayList<FunctionProxy> proxies = new ArrayList<FunctionProxy>();
	
	public static FunctionProxy getProxy(int ID) {
		return proxies.get(ID);
	}
	
	public static FunctionProxy build(String type, ScriptObjectMirror func) {
		FunctionProxy proxy = new FunctionProxy();
		proxy.ID = proxies.size();
		proxy.type = type;
		proxy.func = func;
		proxies.add(proxy);
		return proxy;
	}
	
	public void invoke() {
		try {
			ret = func.call(func, args);
		} catch (Exception e) {
			JSTLogger.get().error(e);
		}
	}
	
	public void inject(ArrayList<String> list) {
		Type t = Type.getType(ASMHelper.getDescFromType(type));
		Type[] argTypes = t.getArgumentTypes();
		Type retType = t.getReturnType();
		args = new Object[argTypes.length];
		for (int i = argTypes.length - 1; i >= 0; i--) {
			String type = argTypes[i].getClassName();
			upcast(type, list);
			list.add("iconst " + ID);
			list.add("invokestatic com.mmyzd.jstweaker.core.asm.FunctionProxy.getProxy: com.mmyzd.jstweaker.core.asm.FunctionProxy(int)");
			list.add("getfield com.mmyzd.jstweaker.core.asm.FunctionProxy.args: java.lang.Object[]");
			list.add("swap");
			list.add("iconst " + i);
			list.add("swap");
			list.add("aastore");
		}
		String type = retType.getClassName();
		list.add("iconst " + ID);
		list.add("invokestatic com.mmyzd.jstweaker.core.asm.FunctionProxy.getProxy: com.mmyzd.jstweaker.core.asm.FunctionProxy(int)");
		if (!type.equals("void")) list.add("dup");
		list.add("invokevirtual com.mmyzd.jstweaker.core.asm.FunctionProxy.invoke: void()");
		if (!type.equals("void")) {
			list.add("getfield com.mmyzd.jstweaker.core.asm.FunctionProxy.ret: java.lang.Object");
			downcast(type, list);
		}
	}
	
	private static final ImmutableMap<String, String> mapping = ImmutableMap.<String, String>builder()
		.put("byte", "Byte")
		.put("char", "Character")
		.put("double", "Double")
		.put("float", "Float")
		.put("int", "Integer")
		.put("long", "Long")
		.put("short", "Short")
		.put("boolean", "Boolean")
		.build();
	
	private void upcast(String src, ArrayList<String> list) {
		String dst = mapping.get(src);
		if (dst == null) return;
		list.add("invokestatic java.lang." + dst + ".valueOf: java.lang." + dst + "(" + src + ")");
	}
	
	private void downcast(String dst, ArrayList<String> list) {
		String src = mapping.get(dst);
		if (src == null) {
			list.add("checkcast " + dst);
		} else {
			list.add("checkcast java.lang." + src);
			list.add("invokevirtual java.lang." + src + "." + dst + "Value: " + dst + "()");
		}
	}
	
	private FunctionProxy() {}

}
