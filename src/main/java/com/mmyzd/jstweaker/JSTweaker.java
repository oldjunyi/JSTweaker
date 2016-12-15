package com.mmyzd.jstweaker;

import com.mmyzd.jstweaker.scripting.ScriptManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
	modid = JSTweaker.MODID,
	useMetadata = true,
	acceptedMinecraftVersions = "[1.9,1.10.2]"
)
public class JSTweaker {
	
	public static final String MODID = "jstweaker";
	
	@Instance(MODID)
	public static JSTweaker instance;
	
	public ScriptManager scripts = ScriptManager.getInstance();
	
	@EventHandler
	public void construct(FMLConstructionEvent event) {
		scripts.load();
	}
	
	@EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		
	}
	
	@EventHandler
	public void onInit(FMLInitializationEvent event) {
		
	}
	
	@EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {
		
	}
	
	@EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new JSTCommand());
	}

}
