package com.mmyzd.jstweaker.core;

import java.util.Map;

import com.mmyzd.jstweaker.JSTLogger;
import com.mmyzd.jstweaker.core.asm.ClassTransformer;
import com.mmyzd.jstweaker.scripting.ScriptManager;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@TransformerExclusions("com.mmyzd.jstweaker.core")
public class JSTCore implements IFMLLoadingPlugin {
	
	private ScriptManager scripts = ScriptManager.getInstance();
	
	public JSTCore() {
		JSTLogger.get("JSTweaker");
		JSTLogger.get("JSTweakerASM");
		scripts.init();
	}
	
	@Override
	public String[] getASMTransformerClass() {
		return new String[] { ClassTransformer.class.getName() };
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
