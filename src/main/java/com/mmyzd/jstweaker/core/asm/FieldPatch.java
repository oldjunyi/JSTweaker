package com.mmyzd.jstweaker.core.asm;

import org.objectweb.asm.tree.FieldNode;

public class FieldPatch {

	private int action = -1;
	private int access = -1;
	private ClassPatch owner = null;
	private Object  value = null;
	private boolean valueVisited = false;
	
	public MethodPatch method(String typedName) {
		return getOwner().method(typedName);
	}
	
	public FieldPatch field(String typedName) {
		return getOwner().field(typedName);
	}
	
	public FieldPatch setModifiers(String text) {
		access = ASMHelper.getModifierFlag(text);
		return this;
	}
	
	public FieldPatch setValue(Object obj) {
		value = obj;
		valueVisited = true;
		return this;
	}
	
	public FieldPatch create() {
		action = 0;
		return this;
	}
	
	public FieldPatch remove() {
		action = 1;
		return this;
	}
	
	public FieldPatch(ClassPatch owner) {
		this.owner = owner;
	}
	
	public ClassPatch getOwner() {
		return owner;
	}
	
	public FieldNode doCreate(String name, String desc) {
		return new FieldNode(access == -1 ? 0 : access, name, desc, null, value);
	}
	
	public boolean toCreate() {
		return action == 0;
	}
	
	public boolean toRemove() {
		return action == 1;
	}
	
	public void apply(FieldNode fn) {
		if (access != -1) fn.access = access;
		if (valueVisited) fn.value  = value;
	}
	
}
