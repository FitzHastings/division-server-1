package division.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

public class Paths {
  public static String CONF_PATH      = "conf" + File.separator;
  public static String CONF_FILE_NAME = CONF_PATH+"conf.properties";
  public static Properties properties = new Properties();
  
  public static String LIBS_PATH_PROP    = "LIBS_PATH_PROP";
  public static String PLUGINS_PATH_PROP = "PLUGINS_PATH_PROP";
  
  public static String LIBS_PATH    = "libs"+File.separator;
  public static String PLUGINS_PATH = "plugins"+File.separator;
  
  static {
    createIfNotExist(CONF_FILE_NAME,true);
    try {
      properties.load(new FileInputStream(CONF_FILE_NAME));
      
      if(!properties.containsKey(LIBS_PATH_PROP))
        properties.put(LIBS_PATH_PROP, LIBS_PATH);
      else LIBS_PATH = properties.getProperty(LIBS_PATH_PROP);
      
      if(!properties.containsKey(PLUGINS_PATH_PROP))
        properties.put(PLUGINS_PATH_PROP, PLUGINS_PATH);
      else PLUGINS_PATH = properties.getProperty(PLUGINS_PATH_PROP);
    }catch(IOException ex) {
      Logger.getRootLogger().error(ex.getMessage(),ex);
    }
  }
  
  public static synchronized void createIfNotExist(String fileName,boolean isFile) {
    try {
      File file = new File(fileName);
      if(!file.exists()) {
        if(isFile) {
          new File(fileName.substring(0,fileName.lastIndexOf(File.separator))).mkdirs();
          file.createNewFile();
        }else new File(fileName).mkdirs();
      }
    }catch(IOException e) {
      Logger.getRootLogger().error(e.getMessage(),e);
    }
  }
}