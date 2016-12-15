package com.mmyzd.jstweaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class JSTLogger extends PrintStream {
	
	private static final HashMap<String, JSTLogger> LOGGERS = new HashMap<String, JSTLogger>();
	private static final String LOG_DIRECTORY = "logs/";
	
	private ArrayList<String> errorMessages = new ArrayList<String>();
	
	private JSTLogger() {
		super(System.out);
	}
	
	private JSTLogger(File file) throws FileNotFoundException {
		super(file);
	}
	
	public static JSTLogger get() {
		return get("", "JSTweaker");
	}
	
	public static JSTLogger get(String scriptID) {
		return get("", scriptID);
	}
	
	public static JSTLogger get(String subDir, String scriptID) {
		JSTLogger logger = LOGGERS.get(scriptID);
		if (logger == null) {
			File file = new File(LOG_DIRECTORY + subDir, scriptID + ".log");
			file.getParentFile().mkdirs();
			try {
				logger = new JSTLogger(file);
				LOGGERS.put(scriptID, logger);
			} catch (FileNotFoundException e) {
				logger = new JSTLogger();
			}
		}
		return logger;
	}
	
	public void info(Object obj) {
		println(getDateTimeString() + "[INFO] " + obj);
	}
	
	public void warning(Object obj) {
		println(getDateTimeString() + "[WARNING] " + obj);
	}
	
	public void error(Object obj) {
		println(getDateTimeString() + "[ERROR] " + obj);
	}
	
	public void error(Throwable e) {
		e.printStackTrace();
		errorMessages.add(e.getMessage());
		error(e.getMessage());
		for (StackTraceElement st: e.getStackTrace())
			error("  " + st.toString());
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getErrorMessages() {
		return (ArrayList<String>)errorMessages.clone();
	}
	
	public void clearErrorMessages() {
		errorMessages.clear();
	}
	
	private static String getDateTimeString() {
		final SimpleDateFormat datetime = new SimpleDateFormat("[yyyy/MM/dd/HH:mm:ss]");
		return datetime.format(new Date());
	}

}
