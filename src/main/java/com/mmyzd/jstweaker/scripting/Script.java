package com.mmyzd.jstweaker.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mmyzd.jstweaker.JSTLogger;

import jdk.nashorn.api.scripting.NashornScriptEngine;

public class Script {
	
	private static final String JST = "JSTweaker";
	private static final String CORE = "Core";
	private static final String LIBRARY = "Library";
	private static final String HAS_CORE_API = "HasCoreAPI";
	private static final String HAS_MAIN_API = "HasMainAPI";
	private static final String EVALUATED = "Evaluated";
	
	private NashornScriptEngine engine = ScriptManager.getInstance().createEngine();
	private String path, code;
	private String ID;
	private HashMap<String, ArrayList<String>> tagGroups = new HashMap<String, ArrayList<String>>();
	
	public NashornScriptEngine getEngine() {
		return engine;
	}
	
	public String getID() {
		return ID;
	}
	
	public void setID(String ID) {
		this.ID = ID;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getCode() {
		return code;
	}
	
	public Logger getLogger() {
		return LogManager.getLogger(ID);
	}
	
	public ArrayList<String> getTags(String key) {
		ArrayList<String> tags = tagGroups.get(key);
		if (tags == null) {
			tagGroups.put(key, tags = new ArrayList<String>());
		}
		return tags;
	}
	
	public Object eval(String code) {
		if (code == null) return null;
		try {
			return getEngine().eval(code);
		} catch (ScriptException e) {
			JSTLogger.get().error(e);
		}
		return null;
	}
	
	public void load() {
		ScriptManager manager = ScriptManager.getInstance();
		boolean isCore = getTags(JST).contains(CORE);
		if (getTags(JST).contains(HAS_CORE_API) == false && isCore) {
			getTags(JST).add(HAS_CORE_API);
			eval("var asm = Object.bindProperties({}, new (Java.type('com.mmyzd.jstweaker.scripting.module.ModuleASM')))");
		}
		if (getTags(JST).contains(HAS_MAIN_API) == false && !manager.isCorePhase()) {
			getTags(JST).add(HAS_MAIN_API);
			eval("var events = Object.bindProperties({}, new (Java.type('com.mmyzd.jstweaker.scripting.module.ModuleEvents')))");
			eval("var get = Java.type('com.mmyzd.jstweaker.utils.NameRemapper').get");
			eval("var set = Java.type('com.mmyzd.jstweaker.utils.NameRemapper').set");
			eval("var invoke = Java.type('com.mmyzd.jstweaker.utils.NameRemapper').invoke");
		}
		if (getTags(JST).contains(EVALUATED) == false) {
			getTags(JST).add(EVALUATED);
			String libsName = isCore ? ScriptManager.CORE_LIBS : ScriptManager.MAIN_LIBS;
			for (Script lib: manager.getScripts(libsName)) eval(lib.getCode());
			eval(getCode());
		}
	}
	
	public void unload() {
		
	}
	
	public void init(InputStream stream, String path) throws IOException {
		JSTLogger.get().info("Visiting " + path);
		this.path = path;
		ID = getSuggestedID(path);
		boolean isMetaZone = true;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder builder = new StringBuilder();
		while (true) {
			String line = reader.readLine();
			if (line == null) break;
			if (isMetaZone && line.startsWith("//?")) {
				StringTokenizer tokens = new StringTokenizer(line.substring(3));
				ArrayList<String> tags = null;
				while (tokens.hasMoreTokens()) {
					String tag = tokens.nextToken();
					if (tags == null) {
						tags = getTags(tag);
					} else {
						tags.add(tag);
					}
				}
			} else {
				isMetaZone = false;
			}
			builder.append(line).append('\n');
		}
		reader.close();
		code = builder.toString();
		engine.getBindings(ScriptContext.ENGINE_SCOPE).put("script", this);
	}
	
	public void register() {
		if (getTags(JST).isEmpty()) return;
		ScriptManager manager = ScriptManager.getInstance();
		boolean isCore = getTags(JST).contains(CORE);
		boolean isLib = getTags(JST).contains(LIBRARY);
		if ( isCore &&  isLib) manager.getScripts(ScriptManager.CORE_LIBS).add(this);
		if ( isCore && !isLib) manager.getScripts(ScriptManager.CORE_SCRIPTS).add(this);
		if (!isCore &&  isLib) manager.getScripts(ScriptManager.MAIN_LIBS).add(this);
		if (!isCore && !isLib) manager.getScripts(ScriptManager.MAIN_SCRIPTS).add(this);
	}
	
	private static String getSuggestedID(String path) {
		int start = -1;
		start = Math.max(start, path.lastIndexOf('/') + 1);
		start = Math.max(start, path.lastIndexOf('\\') + 1);
		start = Math.max(start, path.lastIndexOf(' ') + 1);
		return path.substring(start, path.length() - 3);
	}
	
}
