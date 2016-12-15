package com.mmyzd.jstweaker.core.asm;

import java.util.HashMap;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLRemappingAdapter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import com.mmyzd.jstweaker.JSTLogger;

public class ClassTransformer implements IClassTransformer {
	
	private static final HashMap<String, ClassPatch> CLASSES = new HashMap<String, ClassPatch>();
	private static boolean listClasses = false;
	
	public static void listClasses(boolean flag) {
		listClasses = flag;
	}
	
	public static ClassPatch patch(String name) {
		ClassPatch ret = CLASSES.get(name);
		if (ret == null) CLASSES.put(name, ret = new ClassPatch());
		return ret;
	}
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if (listClasses) JSTLogger.get("JSTweakerClasses").info(transformedName);
		ClassReader reader = new ClassReader(bytes);
		ClassNode node = new ClassNode();
		reader.accept(new FMLRemappingAdapter(node), ClassReader.EXPAND_FRAMES);
		ClassPatch patch = CLASSES.get(transformedName);
		if (patch == null) return bytes;
		patch.apply(transformedName, node);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

}
