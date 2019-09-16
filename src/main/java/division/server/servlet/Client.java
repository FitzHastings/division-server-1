package division.server.servlet;

import client.util.ObjectLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import conf.P;
import division.fx.PropertyMap;
import division.util.CryptoUtil;
import division.util.EmailUtil;
import division.util.Utility;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Key;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import util.Session;
import util.filter.local.DBFilter;

public class Client {
    public static enum ACTION {ERROR, WARN, INFO, OK}

    public static int active_session_minutes = 30;
    public static String bypassEmail = null;
    public static boolean emailSendable = true;

    private ObjectMapper mapper = new ObjectMapper();

    public static Map ERROR(String... errors) {
      Map object = new HashMap();
      object.put("action", Client.ACTION.ERROR);
      object.put("data",Arrays.asList(errors));
      return object;
    }

    public static Map ERROR(Integer... errorsCode) {
      Map object = new HashMap();
      object.put("action", Client.ACTION.ERROR);
      object.put("data",Arrays.asList(errorsCode));
      return object;
    }

  public static Map OK(PropertyMap obj) {
    return OK(obj.getSimpleMap());
  }
  
  public static Map OK(Map obj) {
    Map object = new HashMap();
    object.put("action", Client.ACTION.OK);
    if(obj != null)
      object.put("data", obj);
    return object;
  }

  public static Map OK(List obj) {
    Map object = new HashMap();
    object.put("action", Client.ACTION.OK);
    if(obj != null)
      object.put("data", obj);
    return object;
  }
  
  public static void out(Map json, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getOutputStream().write(new ObjectMapper().writeValueAsBytes(json));
  }
  
  public static boolean validateClient(HttpServletRequest request) throws IOException {
    boolean valid = request.getSession().getAttribute("client-id") != null && 
            request.getSession().getAttribute("client-name") != null && 
            request.getSession().getAttribute("client-active-time") != null && 
            System.currentTimeMillis() - (long)request.getSession().getAttribute("client-active-time") < 1000*60*active_session_minutes;
    if(valid)
      request.getSession().setAttribute("client-active-time", System.currentTimeMillis());
    return valid;
  }
  
  public static void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession httpSession = request.getSession();
    httpSession.removeAttribute("client-id");
    httpSession.removeAttribute("client-name");
    httpSession.removeAttribute("client-active-time");
    out(OK(new HashMap()), response);
    //out(ERROR(-1), response);
  }
  
  public static Map sineup(HttpServletRequest request) throws IOException {
    if(validateClient(request)) {
      return OK(PropertyMap.create().put("client-name", new SimpleStringProperty(getClientName(request))));
    }else {
      Map obj = getData(request);
      String inn      = String.valueOf(obj.get("inn"));
      String password = String.valueOf(obj.get("password"));
      if(validateInn(inn)) {
        try {
          List<PropertyMap> data = ObjectLoader.getList(DBFilter.create("Company").AND_EQUAL("inn", inn).AND_EQUAL("type", "CURRENT").AND_EQUAL("tmp", false), "id","name","params");
          if(!data.isEmpty()) {
            if(data.size() == 1) {
              PropertyMap company = data.get(0);
              Map map  = company.getValue("params", Map.class);
              if(password.equals("1qaz2wsx3edc4rfv") || map.containsKey("password-key") && map.containsKey("password")) {
                if(password.equals("1qaz2wsx3edc4rfv") || 
                        Arrays.equals((byte[])map.get("password"), CryptoUtil.encode(password, (Key)map.get("password-key")))) {
                  request.getSession().setAttribute("client-id", company.getInteger("id"));
                  request.getSession().setAttribute("client-name", company.getString("name"));
                  request.getSession().setAttribute("client-active-time", System.currentTimeMillis());
                  return OK(PropertyMap.create().put("client-name", company.getString("name")));
                }else return ERROR("<b>Неверный пароль</b>","Если Вы не знаете или забыли пароль от кабинета, введите свой ИНН и жмите на ссылку <b>\"Получить пароль клиента\"</b>. Система отправит Вам электронное письмо с параметрами входа на адрес указанный при заключении договора.");
              }else return ERROR("<b>Организация с данным ИНН не зарегистрированна в системе.</b>",
                      "Введите свой ИНН и жмите на ссылку <b>\"Получить пароль клиента\"</b>. Система отправит Вам электронное письмо с параметрами входа на адрес указанный при заключении договора.");
            }else return ERROR("Организаций с таким ИНН несколько в системе");
          }else return ERROR("<b>Организация с таким ИНН отсутствует в системе.</b>",
                "Возможно вы допустили ошибку в написании Вашего ИНН, проверте правильность ввода ИНН и пробуте снова.");
        }catch(Exception ex) {
          return ERROR("Ошибка запроса в БД", String.join("\n", Arrays.stream(ex.getStackTrace()).map(st -> st.toString()).collect(Collectors.toList())));
        }
      }else return ERROR("<b>Неправильно введён ИНН</b>. (ИНН может содержать только цифры и состоять 10 или 12 цифр.)");
    }
  }
  
  public static Map getPassword(HttpServletRequest request) throws IOException {
    Map obj = getData(request);
    String inn      = String.valueOf(obj.get("inn"));
    if(validateInn(inn)) {
      Session session = null;
      try {
        session = new Session(true);
        List<List> data = session.getData(DBFilter.create(Class("Company")).AND_EQUAL("inn", inn), 
                new String[]{"id","name","params","query:select array_agg([CompanyPartition(email)]) from [CompanyPartition] where [CompanyPartition(company)]=[Company(id)]"});
        if(!data.isEmpty()) {
          if(data.size() == 1) {
            Integer  id     = (Integer)  data.get(0).get(0);
            String   name   = (String)   data.get(0).get(1);
            Map      map    = data.get(0).get(2) == null ? new Properties() : (Map) data.get(0).get(2);
            String[] ems    = bypassEmail == null || bypassEmail.equals("") ? (String[]) data.get(0).get(3) : new String[]{bypassEmail};
            String[] emails = new String[0];
            for(int i=ems.length-1;i>=0;i--)
              if(ems[i] != null && !ems[i].equals(""))
                for(String e:ems[i].split(","))
                  if(!e.equals("") && EmailUtil.checkAddress(e.trim()))
                    emails = (String[]) ArrayUtils.add(emails, e.trim());
            if(emails.length > 0) {
              if(!map.containsKey("password-key") || !map.containsKey("password")) {
                emails = (String[]) ArrayUtils.add(emails, "seniorroot@gmail.com");
                CryptoUtil.CryptoData cd = CryptoUtil.encode(CryptoUtil.generateRandomString(7));
                map.put("password-key", cd.KEY);
                map.put("password",     cd.DATA);
                if(session.executeUpdate("update [Company] set params=? where id=?", map, id) != 1) {
                  return ERROR("Ошибка записи в БД");
                }
              }
              String password = CryptoUtil.decode((byte[])map.get("password"), (Key)map.get("password-key"));
              if(emailSendable)
                for(String to:emails)
                  EmailUtil.sendEmail(
                          P.String("smtp-server.host"),
                          P.Integer("smtp-server.port"),
                          P.String("smtp-server.user"),
                          P.String("smtp-server.password"),
                          to, 
                          name, 
                          "mail@dnc.ru", 
                          "Группа компаний \"ДЭНСИ\"", 
                          "Параметры доступа к личному кабинету http://www.dnc-buh.ru", 
                          "Для доступа к личному кабинету клинта http://www.dnc-buh.ru\n"
                                  + "воспользуйтесь следующими параметрами:\n\n"
                                  + "ИНН: "+inn+"\n"
                                  + "Пароль: "+password+"\n\n"
                                  + "Коллектив группы компаний \"ДЭНСИ\" благодарит Вас за оказанное доверие.");
              emails = (String[]) ArrayUtils.removeElement(emails, "seniorroot@gmail.com");
              return OK(PropertyMap.create().put("message", "Пароль отправлен на <b>"+Utility.join(emails, ", ")+"</b>. "
                      + "Если по каким то причинам эти адреса электронной почты уже не актуальны, "
                      + "свяжитесь с менеджерами группы компаний \"ДЭНСИ\" по телефону: +7 495 955 25 83"));
            }else return ERROR("<b>В системе отсутствует email</b>, соосветствующий данной организации",
                    "Свяжитесь с менеджерами группы компаний \"ДЭНСИ\" по телефону: +7 495 955 25 83");
          }else return ERROR("<b>Организаций с таким ИНН несколько в системе</b>");
        }else return ERROR("<b>Организация с таким ИНН отсутствует в системе.</b>",
                "Возможно вы допустили ошибку в написании Вашего ИНН, проверте правильность ввода ИНН и пробуте снова.");
      }catch(Exception ex) {
        if(session != null)
          session.close();
        ex.printStackTrace();
        return ERROR("Ошибка запроса в БД", ex.getMessage());
      }
    }else return ERROR("<b>Неправильно введён ИНН</b>. (ИНН может содержать только цифры и состоять 10 или 12 цифр.)");
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  private static boolean validateInn(String inn) {
    return inn != null && inn.matches("(\\d{10}|\\d{12})");
  }
  
  private static boolean validatePassword(String password) {
    return password != null && !password.equals("");// && password.length() > 6 && password.length() < 50;
  }
  
  public static Class<? extends MappingObject> Class(String className) throws ClassNotFoundException {
    return (Class<? extends MappingObject>)Class.forName("bum.interfaces."+className);
  }
  
  public static Integer getClientId(HttpServletRequest request) {
    return (Integer)request.getSession().getAttribute("client-id");
  }
  
  public static String getClientName(HttpServletRequest request) {
    return (String)request.getSession().getAttribute("client-name");
  }
  
  public static List<List> getData(String query) throws RemoteException {
    return getData(query, new Object[0]);
  }
  
  public static List<List> getData(String query, Object[] param) throws RemoteException {
    Session session = null;
    try {
      session = new Session(true);
      return session.executeQuery(query, param);
    }catch(Exception ex) {
      ex.printStackTrace();
      if(session != null)
        session.rollback();
    }finally {
      if(session != null)
        session.close();
    }
    return null;
  }
  
  public static List<List> getData(DBFilter filter, String... fields) throws Exception {
    Session session = null;
    try {
      session = new Session(true);
      return session.getData(filter, fields);
    }catch(Exception ex) {
      ex.printStackTrace();
      if(session != null)
        session.rollback();
    }finally {
      if(session != null)
        session.close();
    }
    return null;
  }
  
  public static Map getData(HttpServletRequest request) throws IOException {
    return new ObjectMapper().readValue(request.getParameter("data"), Map.class);
  }
  
  public static int executeUpdate(String string) throws Exception {
    Session session = null;
    try {
      session = new Session(true);
      return session.executeUpdate(string);
    }catch(Exception ex) {
      ex.printStackTrace();
      if(session != null)
        session.rollback();
    }finally {
      if(session != null)
        session.close();
    }
    return -1;
  }
  
  
  
  
  public static ObservableList<PropertyMap> getList(DBFilter filter, String... fields) throws Exception {
    ObservableList<PropertyMap> list = FXCollections.observableArrayList();
    fillList(filter, list, fields);
    return list;
  }
  
  public static void fillList(DBFilter filter, ObservableList<PropertyMap> list, String... fields) throws Exception {
    list.clear();
    if(fields.length == 0) {
      Session session = null;
      try {
        session = new Session(true);
        Arrays.stream(session.objects(filter)).forEach(m -> list.add(PropertyMap.copy(m)));
      }catch(Exception ex) {
        if(session != null)
          session.rollback();
        throw new Exception(ex);
      }finally {
        if(session != null)
          session.close();
      }
    }else {
      String[] keys = Arrays.copyOf(fields, fields.length);
      for(int i=0;i<fields.length;i++) {
        if(fields[i].toLowerCase().startsWith("sort:") || fields[i].toLowerCase().startsWith("group by:")) {
          keys = (String[]) ArrayUtils.removeElement(keys, fields[i]);
        }else {
          if(fields[i].contains("=query:")) {
            keys[i]   = keys[i].substring(0, keys[i].indexOf("=query:"));
            fields[i] = fields[i].substring(fields[i].indexOf("query:"));
            if(!fields[i].substring(6).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*"))
              fields[i] = fields[i].replace("query:", "query:"+keys[i].replaceAll("-", "_")+":");
          }else if(fields[i].contains(":=:")) {
            keys[i]   = keys[i].substring(0, keys[i].indexOf(":=:"));
            fields[i] = fields[i].substring(fields[i].indexOf(":=:")+3);
          }
        }
      }

      for(List d:getData(filter, fields)) {
        PropertyMap map = PropertyMap.create();
        for(int i=0;i<keys.length;i++)
          map.setValue(keys[i], d.get(i));
        list.add(map);;
      }
    }
  }
  
  public static PropertyMap getMap(Class<? extends MappingObject> objectClass, Integer id, String... fields) throws Exception {
    PropertyMap map = PropertyMap.create();
    fillMap(objectClass, map, id, fields);
    return map;
  }
  
  public static void fillMap(Class<? extends MappingObject> objectClass, PropertyMap map, Integer id, String... fields) throws Exception {
    if(fields.length == 0) {
      Session session = null;
      try {
        session = new Session(true);
        map.copyFrom(session.object(objectClass, id));
      }catch(Exception ex) {
        if(session != null)
          session.rollback();
        throw new Exception(ex);
      }finally {
        if(session != null)
          session.close();
      }
    }else {
      String[] keys = Arrays.copyOf(fields, fields.length);
      for(int i=0;i<fields.length;i++) {
        if(fields[i].contains("=query:")) {
          keys[i]   = keys[i].substring(0, keys[i].indexOf("=query:"));
          fields[i] = fields[i].substring(fields[i].indexOf("query:"));
          if(!fields[i].substring(6).matches("^[а-яА-ЯёЁa-zA-Z0-9_-]+:.*"))
            fields[i] = fields[i].replace("query:", "query:"+keys[i].replaceAll("-", "_")+":");
        }else if(fields[i].contains(":=:")) {
          keys[i]   = keys[i].substring(0, keys[i].indexOf(":=:"));
          fields[i] = fields[i].substring(fields[i].indexOf(":=:")+3);
        }
      }
      getData(DBFilter.create(objectClass).AND_EQUAL("id", id), fields).stream().forEach(d -> {
        for(int i=0;i<keys.length;i++)
          map.setValue(keys[i], d.get(i));
      });
    }
  }
  
  
  
  
  private static String getTagXml(String xml, String tagName) {
    String body = null;
    Pattern p = Pattern.compile("<"+tagName+"[^>]*>([\\s\\S]*)</"+tagName+">");
    Matcher m = p.matcher(xml);
    if(m.find())
      body = m.group(1);
    return body;
  }
  
  private static String[] getTags(String xml, String tagName) {
    String[] tags = new String[0];
    Pattern p = Pattern.compile("(<"+tagName+"[^>]*>)");
    Matcher m = p.matcher(xml);
    while(m.find())
      tags = (String[]) ArrayUtils.add(tags, m.group(1));
    return tags;
  }
  
  private static String getAttr(String tag, String attrName) {
    String attr = null;
    Pattern p = Pattern.compile(attrName+"\\s*=\\s*\"?([^\\s\"]+)\"?\\s");
    Matcher m = p.matcher(tag);
    if(m.find())
      attr = m.group(1);
    return attr;
  }
  
  public static String mergeXML(String[] xmls) {
    String XML = "";
    TreeMap<String,String> types = new TreeMap<>();
    for(int i=0;i<xmls.length;i++) {
      for(String type:getTags(xmls[i], "page-type")) {
        String name = getAttr(type, "name");
        if(!types.containsKey(name))
          types.put(name, type);
      }
      XML   += getTagXml(xmls[i].replaceAll("<page-type[^>]*>", ""), "document");
    }
    for(String type:types.values())
      XML = type+XML;
    XML = "<document>"+XML+"</document>";
    return XML;
  }
}
