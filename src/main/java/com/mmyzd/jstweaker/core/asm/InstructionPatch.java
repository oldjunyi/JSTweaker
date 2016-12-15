package com.mmyzd.jstweaker.core.asm;

import java.util.ArrayList;

import com.mmyzd.jstweaker.utils.StringHelper;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class InstructionPatch {

	private MethodPatch owner = null;
	
	private String selector = "";
	private ArrayList<String> prev = new ArrayList<String>();
	private ArrayList<String> succ = new ArrayList<String>();
	private ArrayList<String> repl = null;
	
	public InstructionPatch at(String selector) {
		return getOwner().at(selector);
	}
	
	public MethodPatch method(String typedName) {
		return getOwner().getOwner().method(typedName);
	}
	
	public FieldPatch field(String typedName) {
		return getOwner().getOwner().field(typedName);
	}
	
	public InstructionPatch insertBefore(Object... codes) {
		return addInsns(prev, codes);
	}
	
	public InstructionPatch insertAfter(Object... codes) {
		return addInsns(succ, codes);
	}
	
	public InstructionPatch replaceWith(Object... codes) {
		repl = new ArrayList<String>();
		return addInsns(repl, codes);
	}
	
	public InstructionPatch remove() {
		return replaceWith();
	}
	
	public InstructionPatch(MethodPatch owner, String selector) {
		this.owner = owner;
		this.selector = selector;
	}
	
	public MethodPatch getOwner() {
		return owner;
	}
	
	public String getSelector() {
		return selector;
	}
	
	public ArrayList<String> getPrev() {
		return prev;
	}
	
	public ArrayList<String> getSucc() {
		return succ;
	}
	
	public ArrayList<String> getRepl() {
		return repl;
	}
	
	public String getSignature() {
		StringBuilder ret = new StringBuilder();
		ret.append('"').append(StringHelper.escape(selector)).append('"');
		ret.append(" -> ");
		ret.append(getCodesLine(prev, false)).append(' ');
		ret.append(getCodesLine(repl, true)).append(' ');
		ret.append(getCodesLine(succ, false));
		return ret.toString();
	}
	
	private InstructionPatch addInsns(ArrayList<String> list, Object... codes) {
		for (Object code: codes) {
			if (code instanceof ScriptObjectMirror) {
				FunctionProxy.build("void()", (ScriptObjectMirror)code).inject(list);
			} else if (code instanceof FunctionProxy) {
				FunctionProxy proxy = (FunctionProxy)code;
				proxy.inject(list);
			} else {
				list.add(code.toString());
			}
		}
		return this;
	}
	
	private String getCodesLine(ArrayList<String> codes, boolean isReplacements) {
		if (codes == null) return "$";
		StringBuilder ret = new StringBuilder();
		ret.append('[');
		for (int i = 0; i < codes.size(); i++) {
			if (i != 0) ret.append(", ");
			ret.append('"').append(StringHelper.escape(codes.get(i))).append('"');
		}
		ret.append(']');
		return ret.toString();
	}
	
}
