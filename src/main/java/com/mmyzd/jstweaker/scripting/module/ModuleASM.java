package com.mmyzd.jstweaker.scripting.module;

import com.mmyzd.jstweaker.JSTLogger;
import com.mmyzd.jstweaker.core.asm.ClassPatch;
import com.mmyzd.jstweaker.core.asm.ClassTransformer;
import com.mmyzd.jstweaker.core.asm.FunctionProxy;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class ModuleASM {
	
	public void init() {
		JSTLogger.get().info("ModuleASM initialized.");
	}

	public void listClasses() {
		ClassTransformer.listClasses(true);
	}
	
	public ClassPatch patch(String className) {
		return ClassTransformer.patch(className);
	}
	
	public FunctionProxy invoke(String type, ScriptObjectMirror func) {
		return FunctionProxy.build(type, func);
	}
	
}
