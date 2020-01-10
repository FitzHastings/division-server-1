package division.server;

import bum.pool.DBController;
import bum.realizations.ServerImpl;
import bum.util.DivisionClientSocketFactory;
import bum.util.DivisionServerSocketFactory;
import com.sun.javafx.reflect.ReflectUtil;
import conf.P;
import division.fx.PropertyMap;
import division.server.servlet.PersonalAccount;
import division.util.TurnOffUtil;
import division.util.Utility;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import mapping.MappingObject;
import mapping.MappingObjectImpl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;
import org.codehaus.groovy.reflection.ReflectionUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import util.DataBase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class DivisionServer {
  private static Server jetty_server;
  private static ExecutorService pool = Executors.newCachedThreadPool();


  public static void main(String[] args) throws Exception {
    
    switch(args[0]) {
      case "update":
        DataBase.updateDataBase();
        break;
      case "start":
        DivisionServer serverMain = new DivisionServer();
        serverMain.startJetty();
        serverMain.startServer();
        Utility.startCommandLine("Division server: ", s -> startCommand(s));
        
        break;
      case "stop":
        try {
          jetty_server.stop();
        }catch(Exception ex) {
          System.out.println("Error.");
          ex.printStackTrace();
        }
        
        TurnOffUtil.kill(P.String("dispose-server.host"), P.Integer("dispose-server.port"), P.String("dispose-server.command"));
        break;
    }
  }
  
  private static final List<String> publishedResources = Arrays.asList(
          "/web.xml",
          "/spring.xml",
          "/web-spring.xml"
  );
  
  private void startJetty() {
    pool.submit(() -> {
      Logger.getRootLogger().info("############### STARTING JETTY ON PORT "+P.Integer("jetty.port")+" ###################");
      
      jetty_server = new Server(P.Integer("jetty.port"));
      
      /*WebAppContext context = new ClasspathWebAppContext(new HashSet<>(publishedResources));
      context.setConfigurations(new Configuration[]{new ClasspathWebXmlConfiguration()});
      context.setDescriptor("web.xml");
      context.setResourceBase(P.String("jetty.resource-base"));
      context.setContextPath(P.String("jetty.context-path"));
      context.setParentLoaderPriority(true);
      context.setClassLoader(Thread.currentThread().getContextClassLoader());*/

      ResourceHandler html_handler = new ResourceHandler();
      html_handler.setResourceBase(P.String("html.resource-base"));
      html_handler.setDirectoriesListed(true);

      ServletContextHandler servlet_handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
      servlet_handler.setResourceBase(P.String("servlet.resource-base"));
      servlet_handler.setContextPath(P.String("servlet.context-path"));
      servlet_handler.addServlet(new ServletHolder(new PersonalAccount()), "/pc");

      HandlerList handlerList = new HandlerList();
      handlerList.setHandlers(new Handler[]{html_handler, servlet_handler});

      NCSARequestLog requestLog = new NCSARequestLog("./logs/jetty-yyyy_mm_dd.request.log");
      requestLog.setRetainDays(90);
      requestLog.setAppend(true);
      requestLog.setExtended(false);
      requestLog.setLogTimeZone("GMT");
      jetty_server.setRequestLog(requestLog);


      jetty_server.setHandler(handlerList);

      Logger.getRootLogger().info("############### STARTING JETTY... ###################");
      try {
        jetty_server.start();
        jetty_server.join();
        System.out.println("Listening port : " + P.Integer("jetty.port"));
      }catch(Exception ex) {
        Logger.getRootLogger().info("############### ERROR STARTING JETTY... ###################");
        Logger.getRootLogger().info(ex.getMessage());
        Logger.getRootLogger().info(ex.getMessage(), ex);
        System.out.println("Error.");
        ex.printStackTrace();
      }
    });
  }

  private void startServer() {
    try {

      //Map<String,Object> map = P.Map("dispose-server");
      
      TurnOffUtil.kill(P.String("dispose-server.host"), P.Integer("dispose-server.port"), P.String("dispose-server.command"));
      Thread.sleep(2000);
      TurnOffUtil.startTurnOffServer(P.Integer("dispose-server.port"), P.String("dispose-server.command"));


      //((List<String>)P.List("libs")).stream().flatMap(lib -> getJars(new File(lib)).stream()).forEach(jar -> DivisionAgent.addClassPath(jar));

      /*for(String dir:(List<String>)P.List("libs"))
        ClassPathLoader.addFile(new File(dir));*/
      
      /*File[] pluginlist = new File[0];
      for(String plugin:P.List("plugins", String.class)) {
        File f = new File(plugin);
        ClassPathLoader.addFile(f);
        if(f.isDirectory())
          pluginlist = (File[]) ArrayUtils.addAll(pluginlist, new File(plugin).listFiles((File pathname) -> !pathname.isDirectory() && pathname.getName().endsWith(".jar")));
        else pluginlist = (File[]) ArrayUtils.add(pluginlist, f);
      }
      loadPlugins(pluginlist);*/

      //try {


      List<Class> classes = new ArrayList<>();

      try (ScanResult implScanResult = new ClassGraph().whitelistPackages("bum.realizations").scan();
           ScanResult interfaceScanResult = new ClassGraph().whitelistPackages("bum.interfaces").scan()) {

        for(ClassInfo classInfo : implScanResult.getAllClasses()) {
          Class cl = Class.forName(classInfo.getName());
          if(cl.getSuperclass().equals(MappingObjectImpl.class))
            classes.add(cl);
        }

        for(ClassInfo classInfo : interfaceScanResult.getAllClasses()) {
          Class cl = Class.forName(classInfo.getName());
          Class[] cll = cl.getInterfaces();
          if(Arrays.asList(cl.getInterfaces()).contains(MappingObject.class))
            classes.add(cl);
        }
      }
        classes.stream().filter((interfaceClass) -> (interfaceClass.isInterface())).forEach((interfaceClass) -> {
            classes.stream().filter((realizationclass) -> (!realizationclass.isInterface())).filter((realizationclass) -> (ArrayUtils.contains(realizationclass.getInterfaces(), interfaceClass))).forEach((realizationclass) -> {
                DataBase.put(realizationclass, interfaceClass);
            });
        });

      DataBase.configure();
      
      for(Map m:(List<Map>)P.List("servers")) {
        PropertyMap s = PropertyMap.copy(m);
        System.setProperty("java.rmi.server.hostname", s.getString("host"));
        Registry registry = LocateRegistry.createRegistry(s.getInteger("rmi-port"));
        
        ServerImpl server = new ServerImpl(s.getInteger("object-port"), new DivisionClientSocketFactory(s.getString("host")), new DivisionServerSocketFactory());
        
        registry.rebind(s.getString("name"), server);
        Logger.getRootLogger().info("Запущен "+s.getString("name")+": host: "+s.getString("host")+" port:"+s.getInteger("rmi-port")+" object-port:"+s.getInteger("object-port"));
      }
      
      Thread t = new Thread(() -> {
        while(true) {
          try {
            Thread.sleep(60000);
            memory();
            Runtime.getRuntime().gc();
            memory();
          }catch(NumberFormatException | InterruptedException ex){}
        }
      });
      t.setDaemon(true);
      t.start();
    }catch(Exception ex) {
      Logger.getRootLogger().fatal(ex.getMessage(), ex);
      System.exit(0);
    }
  }

  /*private static void startCommandLine() {
    Thread t = new Thread(() -> {
      try {
        System.out.print("Division Server: ");
        String cmd = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while((cmd = br.readLine()) != null) {
          startCommand(cmd);
        }
      }catch(Exception ex) {
        Logger.getRootLogger().fatal(ex.getMessage(), ex);
        System.exit(0);
      }
    });
    t.setDaemon(true);
    t.start();
  }*/
  
  private static boolean startCommand(String command) {
    try {
      switch(command) {
        case "update":
          DataBase.updateDataBase();
          break;
        case "u":
          DataBase.updateDataBase();
          break;
        case "exit":
          memory();
          DBController.stop();
          System.exit(0);
          break;
        case "e":
          memory();
          DBController.stop();
          System.exit(0);
          break;
        case "memory":
          memory();
          break;
        case "m":
          memory();
          break;
        case "reindex":
          DataBase.reIndex();
          break;
        case "retriggers":
          DataBase.reTriggers();
          break;
        case "gc":
          Runtime.getRuntime().gc();
          break;
        case "cash":
          //System.out.println("CASH SIZE: "+Session.clientsObjectsCash.size());
          break;
        case "ac":
          System.out.println("ACTIVE DB CONNECTIONS: "+DBController.getActiveConnectionCount());
          System.out.println("IDLE CONNECTIONS: "+DBController.getIdleConnectionCount());
          break;
        default:
          System.out.println("Command not found.");
          break;
      }
    }catch(Exception ex) {
      Logger.getRootLogger().error(ex);
    }/*finally {
      System.out.print("Division Server: ");
    }*/
    return true;
  }
  
  private static void memory() {
    long total  = Runtime.getRuntime().totalMemory();
    long free   = Runtime.getRuntime().freeMemory();
    long memory = total - free;
    long max    = Runtime.getRuntime().maxMemory();
    
    System.out.print("MEMORY["+(memory/1024<1?memory+"b":memory/1024/1024<1?(memory/1024)+"Kb":(memory/1024/1024)+"Mb")+"] ");
    System.out.print("TOTAL["+(total/1024<1?total+"b":total/1024/1024<1?(total/1024)+"Kb":(total/1024/1024)+"Mb")+"] ");
    System.out.print("FREE["+(free/1024<1?free+"b":free/1024/1024<1?(free/1024)+"Kb":(free/1024/1024)+"Mb")+"] ");
    System.out.println("MAX["+(max/1024<1?max+"b":max/1024/1024<1?(max/1024)+"Kb":(max/1024/1024)+"Mb")+"]");
  }
  
  private boolean isMappingObjectClass(Class cl) {
    boolean is = false;
    is = cl == MappingObject.class;
    if(!is) {
      for(Class interfaceClass:cl.getInterfaces()) {
        is = isMappingObjectClass(interfaceClass);
        if(is)
          break;
      }
    }
    return is;
  }
  
  private void loadPlugins(File... pluginList) throws MalformedURLException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, 
          InvocationTargetException, NoSuchFieldException, IOException, ClassNotFoundException {
    if(pluginList == null)
      return;
    /*List<Class> classes = new ArrayList<>();
    for(File plugin:pluginList) {
      if(plugin.exists()) {
        JarFile jarFile = new JarFile(plugin);
        Enumeration<JarEntry> en = jarFile.entries();
        while(en.hasMoreElements()) {
          JarEntry entry = en.nextElement();
          if(!entry.isDirectory() && entry.getName().endsWith(".class")) {
            String name = entry.getName().substring(0, entry.getName().lastIndexOf(".class")).replaceAll("/", ".");
            if(name.indexOf("$") != -1)
              name = name.substring(0, name.indexOf("$"));
            Logger.getRootLogger().info("LOAD CLASS: "+name);
            Class cl = Class.forName(name);
            if(!classes.contains(cl) && isMappingObjectClass(cl))
              classes.add(cl);
          }
        }
      }
    }*/

    /*List<Class> classes = new ArrayList<>();
    Reflections reflections = new Reflections("bum.realizations");
    classes.addAll(reflections.getSubTypesOf(Object.class));

    reflections = new Reflections("bum.interfaces");
    classes.addAll(reflections.getSubTypesOf(Object.class));

    classes.stream().filter((interfaceClass) -> (interfaceClass.isInterface())).forEach((interfaceClass) -> {
      classes.stream().filter((realizationclass) -> (!realizationclass.isInterface())).filter((realizationclass) -> (ArrayUtils.contains(realizationclass.getInterfaces(), interfaceClass))).forEach((realizationclass) -> {
        DataBase.put(realizationclass, interfaceClass);
      });
    });*/
  }
}
