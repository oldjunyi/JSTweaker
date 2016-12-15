package com.mmyzd.jstweaker.scripting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.script.ScriptEngineManager;

import com.mmyzd.jstweaker.JSTLogger;

import jdk.nashorn.api.scripting.NashornScriptEngine;

public class ScriptManager {
	
	private static final ScriptManager INSTANCE = new ScriptManager();
	
	public static final String SCRIPTS_DIR = "scripts/";
	public static final String BUILTIN_DIR = "assets/scripts/";
	public static final String MODS_DIR = "mods/";
	public static final String CORE_LIBS = "CoreLibs";
	public static final String MAIN_LIBS = "MainLibs";
	public static final String CORE_SCRIPTS = "CoreScripts";
	public static final String MAIN_SCRIPTS = "MainScripts";

		
	private ScriptEngineManager engineManager = new ScriptEngineManager();
	private HashMap<String, ArrayList<Script>> scriptGroups = new HashMap<String, ArrayList<Script>>();
	private boolean isCorePhase = true;
	
	public static ScriptManager getInstance() {
		return INSTANCE;
	}
	
	public NashornScriptEngine createEngine() {
		return (NashornScriptEngine)(engineManager.getEngineByName("nashorn"));
	}
	
	public ArrayList<Script> getScripts() {
		return getScripts("");
	}
	
	public ArrayList<Script> getScripts(String key) {
		ArrayList<Script> scripts = scriptGroups.get(key);
		if (scripts == null) {
			scriptGroups.put(key, scripts = new ArrayList<Script>());
		}
		return scripts;
	}
	
	public boolean isCorePhase() {
		return isCorePhase;
	}
	
	public void init() {
		scriptGroups.clear();
		File scriptsDir = new File(SCRIPTS_DIR);
		scriptsDir.mkdirs();
		for (File file: listAllFiles(scriptsDir, new ArrayList<File>(), "^.*\\.js$")) {
			try {
				InputStream stream = new FileInputStream(file);
				Script script = new Script();
				script.init(stream, file.getPath());
				script.register();
			} catch (IOException e) {
				JSTLogger.get().error(e);
			}
		}
		File modsDir = new File(MODS_DIR);
		modsDir.mkdirs();
		for (File file: listAllFiles(modsDir, new ArrayList<File>(), "^.*\\.jar$")) {
			try {
				JarFile jarFile = new JarFile(file.getCanonicalPath());
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					if (entryName.startsWith(BUILTIN_DIR) && entryName.endsWith(".js") && !entry.isDirectory()) {
						InputStream stream = jarFile.getInputStream(entry);
						Script script = new Script();
						String name = file.getCanonicalPath().substring(modsDir.getCanonicalPath().length());
						name += "/" + entryName.substring(BUILTIN_DIR.length());
						script.init(stream, name);
						script.register();
					}
				}
				jarFile.close();
			} catch (IOException e) {
				JSTLogger.get().error(e);
			}
		}
		for (Script script: getScripts(CORE_SCRIPTS)) script.load();
		getScripts().addAll(getScripts(CORE_SCRIPTS));
		getScripts().addAll(getScripts(MAIN_SCRIPTS));
		isCorePhase = false;
	}
	
	public void exec(String fileName) {
		try {
			File scriptsDir = new File(SCRIPTS_DIR);
			scriptsDir.mkdirs();
			File file = new File(scriptsDir, fileName);
			InputStream stream = new FileInputStream(file);
			Script script = new Script();
			script.init(stream, file.getCanonicalPath().substring(scriptsDir.getCanonicalPath().length()));
			script.load();
		} catch (IOException e) {
			JSTLogger.get().error(e);
		}
	}
	
	public void load() {
		ArrayList<Script> scripts = getScripts();
		for (int i = 0; i < scripts.size(); i++) {
			Script script = scripts.get(i);
			script.load();
		}
	}
	
	public void unload() {
		ArrayList<Script> scripts = getScripts();
		for (int i = scripts.size() - 1; i >= 0; i--) {
			Script script = scripts.get(i);
			script.unload();
		}
	}
	
	public void reload() {
		unload();
		init();
		load();
	}
	
	private static ArrayList<File> listAllFiles(File root, ArrayList<File> result, String regex) {
		for (File file: root.listFiles()) {
			if (file.isDirectory()) {
				listAllFiles(file, result, regex);
			} else {
				if (file.getName().matches(regex)) result.add(file);
			}
		}
		return result;
	}
	
}
