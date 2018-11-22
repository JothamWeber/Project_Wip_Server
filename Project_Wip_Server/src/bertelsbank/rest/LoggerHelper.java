package bertelsbank.rest;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class LoggerHelper {
	private static Logger logger;

	public LoggerHelper(){
		if(logger == null){
			try {
				logger = getLogger();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public Logger getLogger() throws IOException {
		logger = Logger.getRootLogger();
		logger.setAdditivity(false);
		SimpleLayout layout = new SimpleLayout();
		ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		logger.addAppender(consoleAppender);
		FileAppender fileAppender = new FileAppender(layout, "logs/ServerLogFile.log", false);
		logger.addAppender(fileAppender);
		logger.setLevel(Level.ALL);

		return logger;
	}

	public void makeInfoLog(String infoLog){
		logger.info(infoLog);
	}

}
