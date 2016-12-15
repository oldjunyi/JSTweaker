package com.mmyzd.jstweaker.core.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.mmyzd.jstweaker.JSTLogger;
import com.mmyzd.jstweaker.utils.StringHelper;

public class Instructions extends MethodVisitor {
	
	private StringBuilder info = new StringBuilder();
	private HashMap<String, Label> idx2lbl = new HashMap<String, Label>();
	private HashMap<Label, String> lbl2idx = new HashMap<Label, String>();
	private ArrayList<Object> locals = new ArrayList<Object>();
	private ArrayList<Object> stacks = new ArrayList<Object>();
	private AbstractInsnNode[]  nodes = null;
	private String[] codes = null;
	private ArrayList<AbstractInsnNode>[] prev = null;
	private ArrayList<AbstractInsnNode>[] succ = null;
	private ArrayList<AbstractInsnNode>[] repl = null;
	private MethodNode mn = null;
	
	@SuppressWarnings("unchecked")
	public Instructions(ClassNode cn, MethodNode mn, String newSuperClass) {
		super(Opcodes.ASM5);
		ListIterator<AbstractInsnNode> t = mn.instructions.iterator();
		int n = mn.instructions.size();
		codes = new String[n];
		nodes = new AbstractInsnNode[n];
		prev = new ArrayList[n];
		succ = new ArrayList[n];
		repl = new ArrayList[n];
		for (int i = 0; i < n; i++) {
			nodes[i] = t.next();
			if (newSuperClass != null && nodes[i].getOpcode() == Opcodes.INVOKESPECIAL) {
				MethodInsnNode node = (MethodInsnNode)nodes[i];
				if (node.owner.equals(cn.superName)) {
					node.owner = ASMHelper.getIntlFromType(newSuperClass);
				}
			}
			if (nodes[i].getType() == AbstractInsnNode.LABEL) visit(nodes[i]);
		}
		for (int i = 0; i < n; i++) codes[i] = visit(nodes[i]);
		this.mn = mn;
	}
	
	@SuppressWarnings("unchecked")
	public void apply(ArrayList<MethodPatch> mps) {
		if (mps == null) return;
		for (MethodPatch mp: mps) {
			try {
				ArrayList<InstructionPatch> ips = mp.getPatches();
				int n = ips.size(), pos = 0;
				int[] line = new int[n];
				ArrayList<AbstractInsnNode>[] prevCache = new ArrayList[n];
				ArrayList<AbstractInsnNode>[] succCache = new ArrayList[n];
				ArrayList<AbstractInsnNode>[] replCache = new ArrayList[n];
				for (int i = 0; i < n; i++) {
					InstructionPatch ip = ips.get(i);
					int selectorPos = -1;
					String selector = ip.getSelector();
					if (selector.startsWith("/") && selector.endsWith("/")) {
						selectorPos = -2;
						selector = selector.substring(1, selector.length() - 1);
					} else if (selector.startsWith("$")) {
						if (selector.equals("$")) selectorPos = pos - 1;
						else if (selector.startsWith("$+")) selectorPos = pos + Integer.parseInt(selector.substring(2)) - 1;
						else if (selector.startsWith("$-")) selectorPos = pos - Integer.parseInt(selector.substring(2)) - 1;
						else selectorPos = Integer.parseInt(selector.substring(1)) - 1;
						if (selectorPos < 0 || selectorPos >= nodes.length) {
							JSTLogger.get().error("at(" + selector + ") located at a wrong place!");
							selectorPos = -3;
						}
					} else {
						selector = visit(build(selector)).replaceAll("^\\s+", "");
					}
					if (selectorPos < 0) {
						for (; pos < codes.length; pos++) {
							String toMatch = codes[pos].replaceAll("^\\s+", "");
							if (selectorPos == -1) {
								if (toMatch.equals(selector)) break;
							} else if (selectorPos == -2) {
								if (toMatch.matches(selector)) break;
							}
						}
					} else {
						pos = selectorPos;
					}
					if (pos == codes.length) throw new Exception("Unmatched patch: " + ip.getSignature());
					prevCache[i] = preparePatches(ip.getPrev(), pos);
					succCache[i] = preparePatches(ip.getSucc(), pos);
					replCache[i] = preparePatches(ip.getRepl(), pos);
					line[i] = pos++;
				}
				for (int i = 0; i < n; i++) {
					pos = line[i];
					collectPatches(prevCache[i], prev, pos);
					collectPatches(succCache[i], succ, pos);
					collectPatches(replCache[i], repl, pos);
				}
				mp.apply(mn);
			} catch (Exception e) {
				JSTLogger.get().error(e);
			}
		}
		mn.instructions.clear();
		for (int i = 0; i < codes.length; i++) {
			applyPatches(mn.instructions, prev[i]);
			if (repl[i] == null) mn.instructions.add(nodes[i]);
			applyPatches(mn.instructions, repl[i]);
			applyPatches(mn.instructions, succ[i]);
		}
	}
	
	public void debug(JSTLogger logger) {
		int n = codes.length;
		for (int i = 0; i < n; i++) {
			debugPatches(prev[i], logger);
			if (repl[i] == null) {
				logger.info(codes[i]);
			} else {
				logger.info("-" + codes[i].substring(1));
			}
			debugPatches(repl[i], logger);
			debugPatches(succ[i], logger);
		}
		logger.info("");
	}

	public AbstractInsnNode build(String code) throws Exception {
		try {
			LinkedList<String> tokens = ASMHelper.getTokens(code);
			String symbol = tokens.poll();
			if (symbol.equalsIgnoreCase("line")) return buildLineNumber(tokens.poll());
			if (symbol.equalsIgnoreCase("local")) return buildFrame(tokens);
			if (symbol.equalsIgnoreCase("stack")) return buildFrame(tokens);
			if (symbol.equalsIgnoreCase("const")) return buildConstant(tokens.poll());
			if (symbol.equalsIgnoreCase("iconst")) return buildIntegerConstant(tokens.poll());
			if (symbol.equalsIgnoreCase("fconst")) return buildFloatConstant(tokens.poll());
			if (symbol.equalsIgnoreCase("dconst")) return buildDoubleConstant(tokens.poll());
			if (symbol.equalsIgnoreCase("lconst")) return buildLongConstant(tokens.poll());
			Integer opcode = LOOKUP.get(symbol.toLowerCase());
			if (opcode == null && symbol.startsWith("L")) return buildLabel(symbol);
			if (opcode != null) switch (opcode) {
			case Opcodes.BIPUSH:
			case Opcodes.SIPUSH:
			case Opcodes.NEWARRAY:
				return buildIntInsn(opcode, tokens.poll());
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:
			case Opcodes.ISTORE:
			case Opcodes.LSTORE:
			case Opcodes.FSTORE:
			case Opcodes.DSTORE:
			case Opcodes.ASTORE:
			case Opcodes.RET:
				return buildVarInsn(opcode, tokens.poll());
			case Opcodes.NEW:
			case Opcodes.ANEWARRAY:
			case Opcodes.CHECKCAST:
			case Opcodes.INSTANCEOF:
				return buildTypeInsn(opcode, tokens.poll());
			case Opcodes.GETSTATIC:
			case Opcodes.PUTSTATIC:
			case Opcodes.GETFIELD:
			case Opcodes.PUTFIELD:
				return buildFieldInsn(opcode, tokens.poll(), tokens.poll());
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				return buildMethodInsn(opcode, tokens.poll(), tokens);
			case Opcodes.INVOKEDYNAMIC:
				return buildInvokedynamicInsn("Sorry, invokedynamic is not supported yet!");
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
			case Opcodes.GOTO:
			case Opcodes.JSR:
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
				return buildJumpInsn(opcode, tokens.poll());
			case Opcodes.LDC:
				return buildLdcInsn(tokens.poll());
			case Opcodes.IINC:
				return buildIincInsn(tokens.poll(), tokens.poll());
			case Opcodes.TABLESWITCH:
				return buildTableSwitchInsn(tokens);
			case Opcodes.LOOKUPSWITCH:
				return buildLookupSwitchInsn(tokens);
			case Opcodes.MULTIANEWARRAY:
				return buildMultiANewArrayInsn(tokens.poll(), tokens.poll());
			default:
				return buildInsn(opcode);
			}
		} catch (Exception e) {
		}
		throw new Exception("Incorrect asm patch: <" + code + ">");
	}

	public String visit(AbstractInsnNode node) {
		node.accept(this);
		return info.toString();
	}
	
	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		if (FRAME_DISABLED) return;
		switch (type) {
		case Opcodes.F_NEW:
		case Opcodes.F_FULL:
			locals.clear();
			stacks.clear();
			for (int i = 0; i < nLocal; i++) locals.add(local[i]);
			for (int i = 0; i < nStack; i++) stacks.add(stack[i]);
			break;
		case Opcodes.F_APPEND:
			stacks.clear();
			for (int i = 0; i < nLocal; i++) locals.add(local[i]);
			break;
		case Opcodes.F_CHOP:
			stacks.clear();
			nLocal = Math.min(nLocal, locals.size());
			for (int i = 0; i < nLocal; i++) locals.remove(locals.size() - 1);
			break;
		case Opcodes.F_SAME:
			stacks.clear();
			break;
		case Opcodes.F_SAME1:
			stacks.clear();
			stacks.add(stack[0]);
			break;
		}
		info.setLength(0);
		info.append(TAB2);
		if (!locals.isEmpty()) {
			info.append("local ");
			info.append(getFrame(locals));
		}
		if (!stacks.isEmpty()) {
			if (!locals.isEmpty()) info.append(", ");
			info.append("stack ");
			info.append(getFrame(stacks));
		}
	}
	
	public AbstractInsnNode buildFrame(LinkedList<String> tokens) {
		locals.clear();
		stacks.clear();
		ArrayList<Object> target = null;
		while (!tokens.isEmpty()) {
			String s = tokens.poll();
			if (s.equalsIgnoreCase("local")) {
				target = locals;
			} else if (s.equalsIgnoreCase("stack")) {
				target = stacks;
			} else if (target != null) {
				if (s.equals("top")) target.add(Opcodes.TOP);
				else if (s.equals("int")) target.add(Opcodes.INTEGER);
				else if (s.equals("float")) target.add(Opcodes.FLOAT);
				else if (s.equals("double")) target.add(Opcodes.DOUBLE);
				else if (s.equals("long")) target.add(Opcodes.LONG);
				else if (s.equals("null")) target.add(Opcodes.NULL);
				else if (s.equals("uninitialized_this")) target.add(Opcodes.UNINITIALIZED_THIS);
				else if (s.startsWith("L")) target.add(getLabel(s));
				else {
					String desc = ASMHelper.getDescFromType(s);
					String intl = ASMHelper.getIntlFromType(s);
					if (desc.startsWith("[")) {
						target.add(desc);
					} else {
						target.add(intl);
					}
				}
			}
		}
		Object[] localArray = new Object[locals.size()];
		Object[] stackArray = new Object[stacks.size()];
		locals.toArray(localArray);
		stacks.toArray(stackArray);
		return new FrameNode(Opcodes.F_FULL, localArray.length, localArray, stackArray.length, stackArray);
	}

	@Override
	public void visitInsn(final int opcode) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]);
	}
	
	public AbstractInsnNode buildInsn(int opcode) {
		return new InsnNode(opcode);
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]).append(' ');
		info.append(opcode == Opcodes.NEWARRAY ? NEWARRAY_TYPES[operand] : operand);
	}
	
	public AbstractInsnNode buildIntInsn(int opcode, String operand) throws Exception {
		if (opcode == Opcodes.NEWARRAY) {
			for (int i = 0; i < NEWARRAY_TYPES.length; i++) {
				if (NEWARRAY_TYPES[i].equals(operand)) return new IntInsnNode(opcode, i);
			}
			throw new Exception();
		} else {
			return new IntInsnNode(opcode, Integer.parseInt(operand));
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]).append(' ');
		info.append(var);
	}
	
	public AbstractInsnNode buildVarInsn(int opcode, String var) {
		return new VarInsnNode(opcode, Integer.parseInt(var));
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]).append(' ');
		info.append(ASMHelper.getTypeFromIntl(type));
	}
	
	public AbstractInsnNode buildTypeInsn(int opcode, String type) {
		return new TypeInsnNode(opcode, ASMHelper.getIntlFromType(type));
	}
	
	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]).append(' ');
		info.append(ASMHelper.getTypeFromIntl(owner)).append('.');
		info.append(name).append(": ");
		info.append(ASMHelper.getTypeFromDesc(desc));
	}
	
	public AbstractInsnNode buildFieldInsn(int opcode, String ownedName, String type) {
		int pos = ownedName.lastIndexOf('.');
		String owner = ownedName.substring(0, pos);
		String name  = ownedName.substring(pos + 1, ownedName.length());
		return new FieldInsnNode(opcode, ASMHelper.getIntlFromType(owner), name, ASMHelper.getDescFromType(type));
	}

	@Deprecated
	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
		visitMethodInsn(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE);
	}
	
	@Override
	public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]).append(' ');
		info.append(ASMHelper.getTypeFromIntl(owner)).append('.');
		info.append(name).append(": ");
		info.append(ASMHelper.getTypeFromDesc(desc));
		if (itf && opcode != Opcodes.INVOKEINTERFACE) info.append(" interface");
	}
	
	public AbstractInsnNode buildMethodInsn(int opcode, String ownedName, LinkedList<String> tokens) {
		int pos = ownedName.lastIndexOf('.');
		String owner = ownedName.substring(0, pos);
		String name  = ownedName.substring(pos + 1, ownedName.length());
		StringBuilder s = new StringBuilder();
		String last = tokens.getLast();
		boolean itf = opcode == Opcodes.INVOKEINTERFACE;
		if (last.equalsIgnoreCase("interface")) {
			itf = true;
			tokens.pollLast();
		}
		s.append(tokens.poll());
		s.append('(');
		for (int i = 0; !tokens.isEmpty(); i++) {
			if (i != 0) s.append(", ");
			s.append(tokens.poll());
		}
		s.append(')');
		return new MethodInsnNode(opcode, ASMHelper.getIntlFromType(owner), name, ASMHelper.getDescFromType(s.toString()), itf);
	}
	
	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
		info.setLength(0);
		info.append(TAB2).append("invokedynamic").append(' ');
		info.append(name).append(": ");
		info.append(ASMHelper.getTypeFromDesc(desc));
		info.append(" # Not fully supported yet");
	}
	
	public AbstractInsnNode buildInvokedynamicInsn(String info) throws Exception {
		JSTLogger.get().error(info);
		throw new Exception(info);
	}
	
	@Override
	public void visitJumpInsn(final int opcode, final Label label) {
		info.setLength(0);
		info.append(TAB2).append(OPCODES[opcode]).append(' ');
		info.append(getIndex(label));
	}
	
	public AbstractInsnNode buildJumpInsn(int opcode, String index) {
		return new JumpInsnNode(opcode, new LabelNode(getLabel(index)));
	}
	
	@Override
	public void visitLabel(Label label) {
		info.setLength(0);
		info.append(TAB1).append(getIndex(label));
	}
	
	public AbstractInsnNode buildLabel(String index) {
		return new LabelNode(getLabel(index));
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		info.setLength(0);
		info.append(TAB2).append("ldc ");
		if (cst instanceof String) {
			info.append('%').append(StringHelper.escape((String)cst));
		} else if (cst instanceof Type) {
			info.append(((Type)cst).getClassName());
		} else {
			info.append(cst);
			if (cst instanceof Long) info.append("L");
			if (cst instanceof Float) info.append("F");
		}
	}
	
	public AbstractInsnNode buildLdcInsn(String data) {
		char c = data.charAt(0);
		if (c == '%') return new LdcInsnNode(data.substring(1, data.length()));
		if (Character.isDigit(c)) {
			char d = data.charAt(data.length() - 1);
			if (d == 'f' || d == 'F') return new LdcInsnNode(Float.parseFloat(data.substring(0, data.length() - 1)));
			if (d == 'l' || d == 'L') return new LdcInsnNode(Long .parseLong (data.substring(0, data.length() - 1)));
			if (data.matches("^[+-]?\\d+$")) return new LdcInsnNode(Integer.parseInt(data));
			return new LdcInsnNode(Double.parseDouble(data));
		}
		return new LdcInsnNode(Type.getType(ASMHelper.getDescFromType(data)));
	}
	
	@Override
	public void visitIincInsn(final int var, final int increment) {
		info.setLength(0);
		info.append(TAB2).append("iinc ");
		info.append(var).append(' ');
		info.append(increment);
	}
	
	public AbstractInsnNode buildIincInsn(String var, String inc) {
		return new IincInsnNode(Integer.parseInt(var), Integer.parseInt(inc));
	}
	
	@Override
	public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
		info.setLength(0);
		info.append(TAB2).append("tableswitch {");
		info.append(min).append("-").append(max).append(": ");
		for (int i = 0; i < labels.length; ++i) {
			info.append(getIndex(labels[i])).append(", ");
		}
		info.append("default: ");
		info.append(getIndex(dflt));
		info.append('}');
	}
	
	public AbstractInsnNode buildTableSwitchInsn(LinkedList<String> tokens) {
		int min = Integer.parseInt(tokens.poll());
		int max = Integer.parseInt(tokens.poll());
		LabelNode[] labels = new LabelNode[max - min + 1];
		for (int i = min; i <= max; i++) {
			labels[i - min] = new LabelNode(getLabel(tokens.poll()));
		}
		tokens.poll(); // skip "default"
		LabelNode dflt = new LabelNode(getLabel(tokens.poll()));
		return new TableSwitchInsnNode(min, max, dflt, labels);
	}
	
	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
		info.setLength(0);
		info.append(TAB2).append("lookupswitch {");
		for (int i = 0; i < labels.length; ++i) {
			info.append(keys[i]).append(": ");
			info.append(getIndex(labels[i])).append(", ");
		}
		info.append("default: ");
		info.append(getIndex(dflt));
		info.append('}');
	}
	
	public AbstractInsnNode buildLookupSwitchInsn(LinkedList<String> tokens) {
		int n = tokens.size() / 2 - 1, idx = -1;
		int[] keys = new int[n];
		LabelNode[] labels = new LabelNode[n];
		LabelNode dflt = null;
		for (int i = 0; i <= n; i++) {
			String key = tokens.poll();
			LabelNode node = new LabelNode(getLabel(tokens.poll()));
			if (key.equalsIgnoreCase("default")) {
				dflt = node;
			} else {
				keys[++idx] = Integer.parseInt(key);
				labels[idx] = node;
			}
		}
		return new LookupSwitchInsnNode(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(final String desc, final int dims) {
		info.setLength(0);
		info.append(TAB2).append("multianewarray ");
		info.append(ASMHelper.getTypeFromDesc(desc)).append(' ');
		info.append(dims);
	}
	
	public AbstractInsnNode buildMultiANewArrayInsn(String type, String dims) {
		return new MultiANewArrayInsnNode(ASMHelper.getDescFromType(type), Integer.parseInt(dims));
	}
	
	@Override
	public void visitLineNumber(final int line, final Label start) {
		info.setLength(0);
		info.append(TAB2).append("line ");
		info.append(line);
	}
	
	public AbstractInsnNode buildLineNumber(String line) {
		return new LineNumberNode(Integer.parseInt(line), new LabelNode());
	}
	
	public AbstractInsnNode buildConstant(String data) {
		char c = data.charAt(0);
		if (c == '%') return new LdcInsnNode(data.substring(1, data.length()));
		if (Character.isDigit(c)) {
			char d = data.charAt(data.length() - 1);
			if (d == 'f' || d == 'F') return buildFloatConstant(data.substring(0, data.length() - 1));
			if (d == 'l' || d == 'L') return buildLongConstant(data.substring(0, data.length() - 1));
			if (data.matches("^[+-]?\\d+$")) return buildIntegerConstant(data);
			return buildDoubleConstant(data);
		}
		return new LdcInsnNode(Type.getType(ASMHelper.getDescFromType(data)));
	}

	public AbstractInsnNode buildFloatConstant(String data) {
		float cst = Float.parseFloat(data);
		int bits = Float.floatToIntBits(cst);
		if (bits == 0 || bits == 0x3f800000 || bits == 0x40000000) { // 0..2
			return new InsnNode(Opcodes.FCONST_0 + (int)cst);
		} else {
			return new LdcInsnNode(cst);
		}
	}
	
	public AbstractInsnNode buildLongConstant(String data) {
		long cst = Long.parseLong(data);
		if (cst == 0L || cst == 1L) {
			return new InsnNode(Opcodes.LCONST_0 + (int)cst);
		} else {
			return new LdcInsnNode(cst);
		}
	}

	public AbstractInsnNode buildIntegerConstant(String data) {
		int cst = Integer.parseInt(data);
		if (cst >= -1 && cst <= 5) {
			return new InsnNode(Opcodes.ICONST_0 + cst);
		} else if (cst >= Byte.MIN_VALUE && cst <= Byte.MAX_VALUE) {
			return new IntInsnNode(Opcodes.BIPUSH, cst);
		} else if (cst >= Short.MIN_VALUE && cst <= Short.MAX_VALUE) {
			return new IntInsnNode(Opcodes.SIPUSH, cst);
		} else {
			return new LdcInsnNode(cst);
		}
	}
	
	public AbstractInsnNode buildDoubleConstant(String data) {
		double cst = Double.parseDouble(data);
		long bits = Double.doubleToLongBits(cst);
		if (bits == 0L || bits == 0x3ff0000000000000L) { // +0.0d and 1.0d
			return new InsnNode(Opcodes.DCONST_0 + (int)cst);
		} else {
			return new LdcInsnNode(cst);
		}
	}
	
	private static final boolean FRAME_DISABLED = true;
	private static final String TAB1 = "  ";
	private static final String TAB2 = "    ";
	private static final HashMap<String, Integer> LOOKUP = new HashMap<String, Integer>();
	private static final String[] NEWARRAY_TYPES = {"", "", "", "", "boolean", "char", "float", "double", "byte", "short", "int", "long"};
	private static final String[] OPCODES = {
		"nop","aconst_null",
		"iconst_m1",
		"iconst_0","iconst_1","iconst_2","iconst_3","iconst_4","iconst_5",
		"lconst_0","lconst_1",
		"fconst_0","fconst_1","fconst_2",
		"dconst_0","dconst_1",
		"bipush","sipush",
		"ldc","ldc_w","ldc2_w",
		"iload","lload","fload","dload","aload",
		"iload_0","iload_1","iload_2","iload_3",
		"lload_0","lload_1","lload_2","lload_3",
		"fload_0","fload_1","fload_2","fload_3",
		"dload_0","dload_1","dload_2","dload_3",
		"aload_0","aload_1","aload_2","aload_3",
		"iaload","laload","faload","daload","aaload","baload","caload","saload",
		"istore","lstore","fstore","dstore","astore",
		"istore_0","istore_1","istore_2","istore_3",
		"lstore_0","lstore_1","lstore_2","lstore_3",
		"fstore_0","fstore_1","fstore_2","fstore_3",
		"dstore_0","dstore_1","dstore_2","dstore_3",
		"astore_0","astore_1","astore_2","astore_3",
		"iastore","lastore","fastore","dastore","aastore","bastore","castore","sastore",
		"pop","pop2","dup","dup_x1","dup_x2","dup2","dup2_x1","dup2_x2","swap",
		"iadd","ladd","fadd","dadd",
		"isub","lsub","fsub","dsub",
		"imul","lmul","fmul","dmul",
		"idiv","ldiv","fdiv","ddiv",
		"irem","lrem","frem","drem",
		"ineg","lneg","fneg","dneg",
		"ishl","lshl",
		"ishr","lshr",
		"iushr","lushr",
		"iand","land",
		"ior","lor",
		"ixor","lxor",
		"iinc",
		"i2l","i2f","i2d",
		"l2i","l2f","l2d",
		"f2i","f2l","f2d",
		"d2i","d2l","d2f",
		"i2b","i2c","i2s",
		"lcmp",
		"fcmpl","fcmpg",
		"dcmpl","dcmpg",
		"ifeq","ifne","iflt","ifge","ifgt","ifle",
		"if_icmpeq","if_icmpne","if_icmplt","if_icmpge","if_icmpgt","if_icmple",
		"if_acmpeq","if_acmpne",
		"goto","jsr","ret",
		"tableswitch","lookupswitch",
		"ireturn","lreturn","freturn","dreturn","areturn","return",
		"getstatic","putstatic","getfield","putfield",
		"invokevirtual","invokespecial","invokestatic","invokeinterface","invokedynamic",
		"new","newarray","anewarray","arraylength",
		"athrow","checkcast","instanceof",
		"monitorenter","monitorexit","",
		"multianewarray",
		"ifnull","ifnonnull",
		"","","","","","","","","","","","","","","","","","","",
		"","","","","","","","","","","","","","","","","","","",
		"","","","","","","","","","","","","","","","","",""
	};
	
	static {
		for (int i = 0; i < OPCODES.length; i++) LOOKUP.put(OPCODES[i], i);
	}
	
	private String getFrame(ArrayList<Object> u) {
		StringBuilder ret = new StringBuilder();
		ret.append('{');
		for (int i = 0; i < u.size(); i++) {
			if (i != 0) ret.append(", ");
			Object o = u.get(i);
			if (o instanceof String) {
				String s = (String)o;
				if (s.startsWith("[")) {
					ret.append(ASMHelper.getTypeFromDesc(s));
				} else{
					ret.append(ASMHelper.getTypeFromIntl(s));
				}
			} else if (o instanceof Integer) {
				int x = (Integer)o;
				if (x == Opcodes.TOP) ret.append("top");
				if (x == Opcodes.INTEGER) ret.append("int");
				if (x == Opcodes.FLOAT) ret.append("float");
				if (x == Opcodes.DOUBLE) ret.append("double");
				if (x == Opcodes.LONG) ret.append("long");
				if (x == Opcodes.NULL) ret.append("null");
				if (x == Opcodes.UNINITIALIZED_THIS) ret.append("uninitialized_this");
			} else if (o instanceof Label) {
				ret.append(getIndex((Label)o));
			}
		}
		ret.append('}');
		return ret.toString();
	}
	
	private String getIndex(Label label) {
		String ret = lbl2idx.get(label);
		if (ret == null) {
			ret = "L" + lbl2idx.size();
			lbl2idx.put(label, ret);
			idx2lbl.put(ret, label);
		}
		return ret;
	}
	
	private Label getLabel(String index) {
		Label ret = idx2lbl.get(index);
		if (ret == null) {
			ret = new Label();
			lbl2idx.put(ret, index);
			idx2lbl.put(index, ret);
		}
		
		return ret;
	}
	
	private ArrayList<AbstractInsnNode> preparePatches(ArrayList<String> list, int pos) throws Exception {
		if (list == null) return null;
		ArrayList<AbstractInsnNode> ret = new ArrayList<AbstractInsnNode>();
		for (String s: list) {
			String selector = s;
			if (selector.startsWith("$")) {
				if (selector.equals("$")) selector = codes[pos];
				else if (selector.startsWith("$+")) selector = codes[pos + Integer.parseInt(selector.substring(2))];
				else if (selector.startsWith("$-")) selector = codes[pos - Integer.parseInt(selector.substring(2))];
				else selector = codes[Integer.parseInt(selector.substring(1))];
			}
			ret.add(build(selector));
		}
		return ret;
	}
	
	private void collectPatches(ArrayList<AbstractInsnNode> nodes, ArrayList<AbstractInsnNode>[] nodesList, int pos) {
		if (nodes == null) return;
		if (nodesList[pos] == null) nodesList[pos] = new ArrayList<AbstractInsnNode>();
		nodesList[pos].addAll(nodes);
	}
	
	private void applyPatches(InsnList insns, ArrayList<AbstractInsnNode> nodes) {
		if (nodes == null) return;
		for (AbstractInsnNode node: nodes) insns.add(node);
	}
	
	private void debugPatches(ArrayList<AbstractInsnNode> nodes, JSTLogger logger) {
		if (nodes == null) return;
		for (AbstractInsnNode node: nodes) {
			String s = visit(node);
			logger.info("+" + s.substring(1));
		}
	}
	
}
