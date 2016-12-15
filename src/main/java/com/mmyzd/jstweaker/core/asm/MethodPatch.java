package com.mmyzd.jstweaker.core.asm;

import java.util.ArrayList;

import org.objectweb.asm.tree.MethodNode;

public class MethodPatch {
	
	private int access = -1;
	private ClassPatch owner = null;
	private ArrayList<InstructionPatch> instructions = new ArrayList<InstructionPatch>();
	
	public InstructionPatch at(String selector) {
		InstructionPatch patch = new InstructionPatch(this, selector);
		instructions.add(patch);
		return patch;
	}

	public MethodPatch method(String typedName) {
		return getOwner().method(typedName);
	}
	
	public FieldPatch field(String typedName) {
		return getOwner().field(typedName);
	}
	
	public MethodPatch setModifiers(String text) {
		access = ASMHelper.getModifierFlag(text);
		return this;
	}
	
	public MethodPatch(ClassPatch owner) {
		this.owner = owner;
	}
	
	public ClassPatch getOwner() {
		return owner;
	}
	
	public ArrayList<InstructionPatch> getPatches() {
		return instructions;
	}
	
	public void apply(MethodNode mn) {
		if (access != -1) mn.access = access;
	}
	
}
