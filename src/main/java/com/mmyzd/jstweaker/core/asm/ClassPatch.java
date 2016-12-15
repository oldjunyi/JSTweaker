package com.mmyzd.jstweaker.core.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.mmyzd.jstweaker.JSTLogger;
import com.mmyzd.jstweaker.utils.StringHelper;

public class ClassPatch {
	
	private String superClass = null;
	private boolean isLogging = false;
	private HashMap<String, ArrayList<MethodPatch>> methods = new HashMap<String, ArrayList<MethodPatch>>();
	private HashMap<String, ArrayList<FieldPatch>> fields = new HashMap<String, ArrayList<FieldPatch>>();
	
	public MethodPatch method(String typedName) {
		String name = ASMHelper.getNameFromRaw(typedName);
		String type = ASMHelper.getTypeFromRaw(typedName);
		typedName = name + ": " + type;
		ArrayList<MethodPatch> patches = methods.get(typedName);
		if (patches == null) methods.put(typedName, patches = new ArrayList<MethodPatch>());
		MethodPatch patch = new MethodPatch(this);
		patches.add(patch);
		return patch;
	}
	
	public FieldPatch field(String typedName) {
		String name = ASMHelper.getNameFromRaw(typedName);
		String type = ASMHelper.getTypeFromRaw(typedName);
		typedName = name + ": " + type;
		ArrayList<FieldPatch> patches = fields.get(typedName);
		if (patches == null) fields.put(typedName, patches = new ArrayList<FieldPatch>());
		FieldPatch patch = new FieldPatch(this);
		patches.add(patch);
		return patch;
	}
	
	public ClassPatch setSuperClass(String name) {
		superClass = name;
		return this;
	}
	
	public ClassPatch logging() {
		isLogging = true;
		return this;
	}
	
	public void apply(String name, ClassNode cn) {
		JSTLogger.get().info("Patching " + name);
		if (isLogging) {
			JSTLogger.get("JSTweakerASM").info("Patching " + name);
			StringBuilder info = new StringBuilder();
			info.append("Super Class: ");
			info.append(ASMHelper.getTypeFromIntl(cn.superName));
			if (superClass != null) {
				info.append(" -> ");
				info.append(superClass);
			}
			JSTLogger.get("JSTweakerASM").info(info.toString());
		}
		patchFields(cn);
		patchMethods(cn);
	}
	
	private void patchFields(ClassNode cn) {
		if (isLogging) JSTLogger.get("JSTweakerASM").info("Fields:");
		ArrayList<FieldNode> removal = new ArrayList<FieldNode>();
		HashSet<String> typedNameTable = new HashSet<String>();
		HashSet<String> nameTable = new HashSet<String>();
		for (FieldNode fn: cn.fields) {
			String typedName = fn.name + ": " + ASMHelper.getTypeFromDesc(fn.desc);
			typedNameTable.add(typedName);
			nameTable.add(fn.name);
			ArrayList<FieldPatch> patches = fields.get(typedName);
			int oldAccess = fn.access;
			boolean toRemove = false;
			if (patches != null) {
				for (FieldPatch fp: patches) {
					toRemove |= fp.toRemove();
					fp.apply(fn);
				}
			}
			if (toRemove) removal.add(fn);
			loggingFields(fn, toRemove ? '-' : ' ', oldAccess, patches);
		}
		for (FieldNode fn: removal) cn.fields.remove(fn);
		for (Entry<String, ArrayList<FieldPatch>> e: fields.entrySet()) {
			String typedName = e.getKey();
			ArrayList<FieldPatch> patches = e.getValue();
			boolean notWarned = true;
			for (FieldPatch fp: patches) {
				if (fp.toCreate()) {
					LinkedList<String> tokens = ASMHelper.getTokens(typedName);
					String name = tokens.poll();
					String desc = ASMHelper.getDescFromType(tokens.poll());
					if (nameTable.contains(name)) {
						JSTLogger.get().error("Trying to create \"" + typedName + "\" with a duplicated name!");
					} else {
						FieldNode fn = fp.doCreate(name, desc);
						nameTable.add(name);
						cn.fields.add(fn);
						loggingFields(fn, '+', fn.access, patches);
					}
				} else if (!typedNameTable.contains(typedName)) {
					if (notWarned) {
						notWarned = false;
						JSTLogger.get().warning("\"" + typedName + "\" not exist in raw class!");
					}
				}
			}
		}
	}
	
	private void patchMethods(ClassNode cn) {
		if (isLogging) JSTLogger.get("JSTweakerASM").info("Methods:");
		HashSet<String> typedNameTable = new HashSet<String>();
		for (MethodNode mn: cn.methods) {
			String typedName = mn.name + ": " + ASMHelper.getTypeFromDesc(mn.desc);
			typedNameTable.add(typedName);
			ArrayList<MethodPatch> patches = methods.get(typedName);
			Instructions insns = new Instructions(cn, mn, superClass);
			int oldAccess = mn.access;
			insns.apply(patches);
			loggingMethods(mn, oldAccess, patches, insns);
		}
		for (Entry<String, ArrayList<MethodPatch>> e: methods.entrySet()) {
			String typedName = e.getKey();
			if (!typedNameTable.contains(typedName)) {
				JSTLogger.get().warning("\"" + typedName + "\" not exist in raw class!");
			}
		}
		if (superClass != null) cn.superName = ASMHelper.getIntlFromType(superClass);
	}
	
	private void loggingFields(FieldNode fn, char prefix, int oldAccess, ArrayList<FieldPatch> patches) {
		if (!isLogging && patches == null) return;
		StringBuilder info = new StringBuilder();
		info.append(prefix).append(' ');
		info.append(fn.name);
		info.append(": ");
		info.append(ASMHelper.getTypeFromDesc(fn.desc));
		if (fn.value != null) {
			info.append(" = ");
			if (fn.value instanceof String) {
				info.append('"').append(StringHelper.escape((String)fn.value)).append('"');
			} else {
				info.append(fn.value);
				if (fn.value instanceof Long) info.append("L");
				if (fn.value instanceof Float) info.append("F");
			}
		}
		info.append(ASMHelper.getModifierInfo(oldAccess, fn.access));
		if (patches != null) {
			JSTLogger.get().info(info.toString());
			if (patches.size() > 1) JSTLogger.get().warning("Multiple patches applied on the same field!");
		}
		if (isLogging) {
			JSTLogger.get("JSTweakerASM").info(info.toString());
		}
	}
	
	private void loggingMethods(MethodNode mn, int oldAccess, ArrayList<MethodPatch> patches, Instructions insns) {
		if (!isLogging && patches == null) return;
		StringBuilder info = new StringBuilder();
		info.append("  ");
		info.append(mn.name);
		info.append(": ");
		info.append(ASMHelper.getTypeFromDesc(mn.desc));
		info.append(ASMHelper.getModifierInfo(oldAccess, mn.access));
		if (patches != null) {
			JSTLogger.get().info(info.toString());
			if (patches.size() > 1) JSTLogger.get().warning("Multiple patches applied on the same method!");
		}
		if (isLogging) {
			JSTLogger.get("JSTweakerASM").info(info.toString());
			insns.debug(JSTLogger.get("JSTweakerASM"));
		}
	}
	
}
