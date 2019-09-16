package division.server.servlet;

import bum.interfaces.Comment;
import bum.interfaces.JSModul;
import client.util.ObjectLoader;
import division.fx.PropertyMap;
import division.util.BytesFile;
import division.util.GzipUtil;
import division.util.Utility;
import documents.FOP;
import java.io.IOException;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mapping.MappingObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.xmlgraphics.util.MimeConstants;
import util.Session;
import util.filter.local.DBFilter;

// 7725608286

public class PersonalAccount extends HttpServlet {
  public enum Command {
    SINEUP,
    GETYEARS,
    SHOWDOCS,
    PREVIEWDOCUMENT,
    PREVIEWACT,
    DOWNLOADDOCUMENT,
    DOWNLOADZIP,
    GETCONTRACTS,
    GETPASSWORD,
    LOGOUT,
    GETEQUIPMENTS,
    SAVEREQUEST,
    SHOWREQUESTS,
    GETREQUESTDATA,
    ADDCOMMENT,
    GETCOMMENTS,
    SETWRITABLE
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      Command command = Command.valueOf(String.valueOf(Client.getData(request).get("command")));
      if(command != Command.GETCOMMENTS)
        Client.active_session_minutes = 30;
      
      if(command == Command.SINEUP) {
        Client.out(saveCommand(request, Client.sineup(request), command), response);
      }else if(command == Command.GETPASSWORD)
        Client.out(saveCommand(request, Client.getPassword(request), command), response);
      else if(Client.validateClient(request)) {
        switch(command) {
          case SETWRITABLE:
            Client.out(setWritable(request), response);
            break;
          case GETCOMMENTS:
            Client.out(getComments(request), response);
            break;
          case ADDCOMMENT:
            Client.out(saveCommand(request, addComment(request), command), response);
            break;
          case GETREQUESTDATA:
            Client.out(saveCommand(request, getRequestData(request), command), response);
            break;
          case SHOWREQUESTS:
            Client.out(saveCommand(request, showRequests(request), command), response);
            break;
          case SAVEREQUEST:
            Client.out(saveCommand(request, saveRequest(request), command), response);
            break;
          case GETEQUIPMENTS:
            Client.out(getEquipments(request), response);
            break;
          case GETYEARS:
            Client.out(saveCommand(request, getYears(request), command),response);
            break;
          case SHOWDOCS:
            Client.out(saveCommand(request, showDocs(request), command),response);
            break;
          case PREVIEWDOCUMENT:
            saveCommand(request, Client.OK(new JSONObject()), command);
            getDocument(false, request, response);
            break;
          case PREVIEWACT:
            saveCommand(request, Client.OK(new JSONObject()), command);
            getAct(request, response);
            break;
          case DOWNLOADDOCUMENT:
            saveCommand(request, Client.OK(new JSONObject()), command);
            getDocument(true, request, response);
            break;
          case DOWNLOADZIP:
            saveCommand(request, Client.OK(new JSONObject()), command);
            getDocument(true, request, response);
            break;
          case GETCONTRACTS:
            Client.out(saveCommand(request, getContracts(request), command),response);
            break;
          case LOGOUT:
            saveCommand(request, Client.OK(new JSONObject()), command);
            Client.logout(request, response);
            break;
        }
      }else Client.out(Client.ERROR(-1), response);
    }catch(Exception ex) {
      ex.printStackTrace();
      Client.out(Client.ERROR(ex.getMessage()),response);
    }finally {
      ObjectLoader.clear();
      response.getOutputStream().flush();
      response.getOutputStream().close();
    }
  }
  
  private Map setWritable(HttpServletRequest request) throws Exception {
    PropertyMap comment = PropertyMap.create().setValue("id", Client.getData(request).get("id")).setValue("type", Comment.Type.CURRENT);
    ObjectLoader.update(Comment.class, comment);
    return Client.OK(comment);
  }
  
  private Map getComments(HttpServletRequest request) throws Exception {
    Client.getClientId(request);
    return Client.OK(PropertyMap.create().setValue("comments", ObjectLoader.getList(DBFilter.create(Comment.class)
            .AND_EQUAL("type", Comment.Type.PROJECT)
            .AND_EQUAL("objectClass", "Request")
            .AND_NOT_LIKE("author", "Company%")
            .AND_IN("objectId", PropertyMap.getArrayFromList(ObjectLoader.getList(DBFilter.create("Request").AND_EQUAL("applicant", Client.getClientId(request)), "id"), "id", Integer.class)))));
  }
  
  private Map addComment(HttpServletRequest request) throws Exception {
    Integer requestId = Integer.valueOf(String.valueOf(Client.getData(request).get("id")));
    String text = String.valueOf(Client.getData(request).get("text"));
    
    PropertyMap comment = PropertyMap.create()
            .setValue("type", Comment.Type.PROJECT) // Типа непрочитанный комментарий
            .setValue("objectClass", Client.Class("Request").getSimpleName())
            .setValue("objectId", requestId)
            .setValue("text", text)
            .setValue("author", "Company:"+Client.getClientId(request));
    comment.copyFrom(ObjectLoader.getMap(Comment.class, ObjectLoader.createObject(Comment.class, comment)));
    return Client.OK(comment);
  }
  
  private Map getRequestData(HttpServletRequest request) throws Exception {
    PropertyMap o = ObjectLoader.getMap(Client.Class("Request"), Client.getData(request).get("id"));
    o.setValue("equipments", ObjectLoader.getList(DBFilter.create("Equipment").AND_IN("id", o.getValue("equipments", Integer[].class)),
            "id",
            "identity_value_name",
            "group_name",
            "address=query:(SELECT name FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND [EquipmentFactorValue(factor)]=(SELECT id FROM [Factor] WHERE name ilike '%адрес установки%'))",
            "factors=query:SELECT ARRAY_AGG([EquipmentFactorValue(factor_name)]||':'||name) FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND "
                    + "[EquipmentFactorValue(factor)] IN (SELECT [Group(factors):target] FROM [Group(factors):table] WHERE [Group(factors):object]=[Equipment(group)])"))
            .setValue("comments", ObjectLoader.getList(DBFilter.create(Comment.class).AND_EQUAL("objectClass", "Request").AND_EQUAL("objectId", o.getInteger("id")))
                    .sorted((PropertyMap o1, PropertyMap o2) -> o1.getTimestamp("date").compareTo(o2.getTimestamp("date"))));
    return Client.OK(o);
  }
  
  private Map showRequests(HttpServletRequest request) throws Exception {
    PropertyMap requests = PropertyMap.create().setValue("rows", ObjectLoader.getList(DBFilter.create("Request").AND_EQUAL("applicant", Client.getClientId(request)).AND_EQUAL("tmp", false).AND_EQUAL("type", MappingObject.Type.CURRENT),
            "id",
            "reason",
            "identificator=query:SELECT array_agg([Equipment(identity_value_name)]) FROM [Equipment] WHERE id=ANY(SELECT [Request(equipments):target] FROM [Request(equipments):table] WHERE [Request(equipments):object]=[Request(id)])",
            "startDate",
            "acceptDate",
            "executDate",
            "exitDate",
            "finishDate").sorted((PropertyMap o1, PropertyMap o2) -> o2.getLocalDateTime("startDate").compareTo(o1.getLocalDateTime("startDate"))));
    requests.getList("rows").stream().forEach(r -> {
      r.setValue("startDate"  , Utility.format(r.getLocalDateTime("startDate")));
      r.setValue("acceptDate" , Utility.format(r.getLocalDateTime("acceptDate")));
      r.setValue("executDate" , Utility.format(r.getLocalDateTime("executDate")));
      r.setValue("exitDate"   , Utility.format(r.getLocalDateTime("exitDate")));
      r.setValue("finishDate" , Utility.format(r.getLocalDateTime("finishDate")));
    });
    return Client.OK(requests);
  }
  
  private Map saveRequest(HttpServletRequest request) throws Exception {
    PropertyMap req = PropertyMap.fromJson(Client.getData(request).toString());
    req.setValue("id", ObjectLoader.createObject(Client.Class("Request"), req
            .setValue("applicant", Client.getClientId(request))
            .setValue("startDate", LocalDateTime.now()).getSimpleMap("reason","startDate","applicant","equipments")));
    return Client.OK(req);
  }
  
  private Map getEquipments(HttpServletRequest request) throws Exception {
    ObservableList<PropertyMap> companyPartitionList = ObjectLoader.getList(DBFilter.create("CompanyPartition").AND_EQUAL("company", Client.getClientId(request)), "id");
    
    ObservableList<PropertyMap> storeList = ObjectLoader.getList(DBFilter.create("Store")
            .AND_IN("companyPartition", PropertyMap.getListFromList(companyPartitionList, "id", Integer.TYPE).toArray(new Integer[0]))
            .AND_EQUAL("objectType", "ТМЦ")
            .AND_EQUAL("storeType", "НАЛИЧНЫЙ"), "id");
    
    List equipments = new ArrayList();
    ObjectLoader.getList(DBFilter.create("Equipment")
            .AND_IN("store", PropertyMap.getListFromList(storeList, "id", Integer.TYPE).toArray(new Integer[0]))
            .AND_EQUAL("zakaz", false), "id","group_name","amount","identity_value_name",
            "address=query:(SELECT name FROM [EquipmentFactorValue] WHERE [EquipmentFactorValue(equipment)]=[Equipment(id)] AND [EquipmentFactorValue(factor)]=(SELECT id FROM [Factor] WHERE name ilike '%адрес установки%'))").stream().forEach(e -> {
              if(!e.isNull("address") && e.getString("address").startsWith("json"))
                e.setValue("address", PropertyMap.fromJson(e.getString("address").substring(4)).getString("title"));
              equipments.add(JSONObject.fromObject(e.toJson()));
            });
    return Client.OK(new JSONObject().accumulate("equipments", equipments));
  }
  
  private Map saveCommand(HttpServletRequest request, Map json, Command command) throws Exception {
    if(json.get("action").toString().equals(Client.ACTION.OK.toString()))
      Client.executeUpdate("INSERT INTO [LKCompany]([LKCompany(company)],[LKCompany(command)],[LKCompany(session)]) VALUES("+Client.getClientId(request)+",'"+command+"','"+request.getSession().getId()+"')");
    return json;
  }
  
  private Map getContracts(HttpServletRequest request) throws RemoteException {
    List<List> data = Client.getData("select "
            + "id,"
            + "[Contract(number)],"
            + "[Contract(templatename)] ,"
            + "(select array_agg([ContractProcess(process)]||','||[ContractProcess(process_name)]) from [ContractProcess] where [ContractProcess(contract)]=[Contract(id)]),"
            
            + "(select "
            + " (select sum([DealPayment(amount)]) from [DealPayment] where tmp=false and type='CURRENT' and [DealPayment(deal)] in (select id from [Deal] where [Deal(contract)]=[Contract(id)])) "
            + " - "
            + " (select sum([DealPosition(customProductCost)] * [DealPosition(amount)]) from [DealPosition] where tmp=false and type='CURRENT' and "
            + "    [DealPosition(dispatchId)] NOTNULL and"
            + "    [DealPosition(deal)] in (select id from [Deal] where [Deal(contract)]=[Contract(id)]))"
            + ")"
            
            + "from [Contract] "
            + "where tmp=false and type='CURRENT' and [Contract(customerCompany)]=?", new Object[]{Client.getClientId(request)});
    if(data != null) {
      List contracts = new ArrayList();
      data.stream().forEach(d -> {
        Map contract = new HashMap();
        contract.put("id",        d.get(0));
        contract.put("number",    d.get(1));
        contract.put("name",      d.get(2));
        contract.put("balance",   d.get(4) == null ? BigDecimal.ZERO : d.get(4));
        List processes = new ArrayList();
        if(d.get(3) != null) {
          Integer[] ids = new Integer[0];
          for(String p:(String[])d.get(3)) {
            if(!ArrayUtils.contains(ids, Integer.valueOf(p.split(",")[0]))) {
              ids = (Integer[]) ArrayUtils.add(ids, Integer.valueOf(p.split(",")[0]));
              processes.add(PropertyMap.create()
                      .put("id", Integer.valueOf(p.split(",")[0]))
                      .put("name", p.substring(p.indexOf(",")+1))
                      .getSimpleMap());
            }
          }
        }
        contract.put("processes", processes);
        contracts.add(contract);
      });
      return Client.OK(PropertyMap.create()
              .put("name", Client.getClientName(request))
              .put("contracts", contracts));
    }else return Client.ERROR("Ошибка запроса в БД");
  }
  
  private void getAct(HttpServletRequest request, HttpServletResponse response) throws Exception {
    String XMLTemplate = getXML();
    if(XMLTemplate != null && !XMLTemplate.equals("")) {
      Map currentData = Client.getData(request);
      Integer year  = Integer.valueOf(String.valueOf(currentData.get("year")));
      Integer kv    = currentData.containsKey("kv") ? Integer.valueOf(String.valueOf(currentData.get("kv"))) : null;
      PropertyMap customerCompany = Client.getMap((Class<? extends MappingObject>)Class.forName("bum.interfaces.Company"), Client.getClientId(request), "id","ownership","name");
      Map<Integer,ObservableList<PropertyMap>> acts = new TreeMap<>();
      Client.getList(DBFilter.create("Contract").AND_IN("id", ((List)currentData.get("contract")).toArray(new Integer[0])),
              "id","templatename","number","sellerCompany","sellerName=query:getCompanyName([Contract(sellerCompany)])","seller-bookkeeper","customerCompany","customerName=query:getCompanyName([Contract(customerCompany)])","customer-bookkeeper",
              "seller-stamp","seller-chifSignature","seller-bookkeeperSignature").stream().forEach(contract -> {
        if(!acts.containsKey(contract.getValue("sellerCompany", Integer.TYPE)))
          acts.put(contract.getValue("sellerCompany", Integer.TYPE), FXCollections.observableArrayList());
        acts.get(contract.getValue("sellerCompany", Integer.TYPE)).add(contract);
      });
      
      
      String[] xmls = null;
      
      for(ObservableList<PropertyMap> contracts:acts.values()) {
        ObservableList vector = FXCollections.observableArrayList();
        LocalDate start = LocalDate.of(year, (kv == null ? 1 : kv) * 3 - 2, 1);
        LocalDate end   = LocalDate.of(year, (kv == null ? 4 : kv) * 3, 1);
        end = end.withDayOfMonth(end.lengthOfMonth());
        
        List<Integer> ids = new ArrayList();
        contracts.stream().forEach(c -> ids.add(c.getValue("id", Integer.TYPE)));
        
        List<String> contractNumbers = new ArrayList();
        contracts.stream().forEach(c -> contractNumbers.add(c.getValue("number", String.class)));
        
        List<List> data = Client.getData("SELECT "
                + "NULLTOZERO((SELECT SUM([Payment(amount)]) FROM [Payment] WHERE tmp=false AND type='CURRENT' AND [Payment(id)] IN "
                + "(SELECT DISTINCT [CreatedDocument(payment)] FROM [CreatedDocument] "
                 + "WHERE "
                  + "[CreatedDocument(payment)] in (SELECT [DealPayment(payment)] FROM [DealPayment] WHERE [DealPayment(deal)] in (SELECT [Deal(id)] FROM [Deal] WHERE [Deal(contract)]=ANY(?))) AND "
                  + "[CreatedDocument(stornoDate)] IS NULL AND "
                  + "[CreatedDocument(document_name)] IN ('Приходный-кассовый ордер','Платёжное поручение') AND "
                  + "[CreatedDocument(date)]::date < ? AND tmp=false AND type='CURRENT' AND "
	                + "[CreatedDocument(customerCompanyPartition)] IN (SELECT [CompanyPartition(id)] FROM [CompanyPartition] WHERE [CompanyPartition(company)]=?) AND "
	                + "[CreatedDocument(sellerCompanyPartition)] IN (SELECT [CompanyPartition(id)] FROM [CompanyPartition] WHERE [CompanyPartition(company)]=?)))) - "
	              +"NULLTOZERO(getDealPositionsCost(ARRAY(SELECT DISTINCT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] "
	                +"WHERE [CreatedDocument(dealPositions):object] IN "
                   + "(SELECT [CreatedDocument(id)] FROM [CreatedDocument] "
                   + "WHERE "
                    + "id in (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target] in (SELECT [DealPosition(id)] "
                      + "FROM [DealPosition] WHERE [DealPosition(deal)] in (SELECT [Deal(id)] FROM [Deal] WHERE [Deal(contract)]=ANY(?)))) AND "
                    + "[CreatedDocument(stornoDate)] IS NULL AND [CreatedDocument(document_name)] = 'Акт завершения работ' AND "
	                  + "[CreatedDocument(date)]::date < ? AND tmp=false AND type='CURRENT' AND "
	                  + "[CreatedDocument(customerCompanyPartition)] IN "
	                  + "(SELECT [CompanyPartition(id)] FROM [CompanyPartition] WHERE [CompanyPartition(company)]=?) AND "
	                  + "[CreatedDocument(sellerCompanyPartition)] IN "
	                  + "(SELECT [CompanyPartition(id)] FROM [CompanyPartition] WHERE [CompanyPartition(company)]=?)))))",
                new Object[]{ids.toArray(new Integer[0]), Utility.convertToSqlDate(start, 0, 0, 0, 0), customerCompany.getValue("id"), contracts.get(0).getValue("sellerCompany"), ids.toArray(new Integer[0]),  Utility.convertToSqlDate(start, 0, 0, 0, 0), customerCompany.getValue("id"), contracts.get(0).getValue("sellerCompany")});
        
        PropertyMap map = PropertyMap.create()
                .setValue("sellerName",          contracts.get(0).getValue("sellerName"))
                .setValue("sellerBookkeeper",    contracts.get(0).getValue("seller-bookkeeper"))
                .setValue("customerName",        contracts.get(0).getValue("customerName"))
                .setValue("customerBookkeeper",  contracts.get(0).getValue("customer-bookkeeper"))
                .setValue("contractNumbers",     contractNumbers)
                .setValue("stamp",               Utility.writeBytesToFile((byte[])contracts.get(0).getValue("seller-stamp")))
                .setValue("chifSignature",       Utility.writeBytesToFile((byte[])contracts.get(0).getValue("seller-chifSignature")))
                .setValue("bookkeeperSignature", Utility.writeBytesToFile((byte[])contracts.get(0).getValue("seller-bookkeeperSignature")))
                .setValue("startSaldo",          data.get(0).get(0));
        
        data = Client.getData("SELECT "
	            /*0*/+"[CreatedDocument(document_name)], "
	            /*1*/+"to_char([CreatedDocument(date)], 'DD.MM.YYYY'), "
	            /*2*/+"[CreatedDocument(number)], "
	            /*3*/+"[CreatedDocument(actionType)]::text, "
	            /*4*/+"getDealPositionsCost(ARRAY(SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] "
	                    +"WHERE [CreatedDocument(dealPositions):object]=[CreatedDocument(id)])), "
	            /*5*/+"(SELECT [Payment(amount)] FROM [Payment] WHERE [Payment(id)]=[CreatedDocument(payment)]), "
	            /*6*/+"(SELECT DISTINCT [DealPosition(dispatchId)] FROM [DealPosition] WHERE [DealPosition(id)] IN "
	                    +"(SELECT [CreatedDocument(dealPositions):target] FROM [CreatedDocument(dealPositions):table] "
	                    +"WHERE [CreatedDocument(dealPositions):object]=[CreatedDocument(id)])) AS dispatchId "
	            +"FROM [CreatedDocument] "
	            +"WHERE "
                + "(id in (SELECT [CreatedDocument(dealPositions):object] FROM [CreatedDocument(dealPositions):table] WHERE [CreatedDocument(dealPositions):target] in (SELECT [DealPosition(id)] "
                 + "FROM [DealPosition] WHERE [DealPosition(deal)] in (SELECT [Deal(id)] FROM [Deal] WHERE [Deal(contract)]=ANY(?)))) OR "
                 + "[CreatedDocument(payment)] in (SELECT [DealPayment(payment)] FROM [DealPayment] WHERE [DealPayment(deal)] in (SELECT [Deal(id)] FROM [Deal] WHERE [Deal(contract)]=ANY(?)))) AND"
                +"[CreatedDocument(document_name)] IN ('Приходный-кассовый ордер','Платёжное поручение','Акт завершения работ') AND "
                +"tmp=false AND "
                +"type='CURRENT' AND "
                +"[CreatedDocument(actionType)] IN ('ОТГРУЗКА','ОПЛАТА') AND "
                +"[CreatedDocument(stornoDate)] IS NULL AND "
                +"[CreatedDocument(date)]::date BETWEEN ? AND ? AND "
                +"[CreatedDocument(sellerCompanyPartition)] IN (SELECT [CompanyPartition(id)] FROM [CompanyPartition] WHERE [CompanyPartition(company)]=?) AND "
                +"[CreatedDocument(customerCompanyPartition)] IN (SELECT [CompanyPartition(id)] FROM [CompanyPartition] WHERE [CompanyPartition(company)]=?) "
                +"ORDER BY [CreatedDocument(date)], [CreatedDocument(actionType)], [CreatedDocument(document_name)], dispatchId", 
                new Object[]{ids.toArray(new Integer[0]), ids.toArray(new Integer[0]), Utility.convertToSqlDate(start, 0, 0, 0, 0), Utility.convertToSqlDate(end, 23, 59, 59, 999), contracts.get(0).getValue("sellerCompany"), customerCompany.getValue("id")});
        
        String date = "";
        String actionType = "";
        String documentString = "";
        Object amount = 0.0;
        for(List it:data) {
          amount = (BigDecimal)(it.get(5) == null ? it.get(4) : it.get(5));
          amount = amount == null ? "?" : amount;
          documentString += (documentString.equals("") ? "" : ", ")+it.get(0)+" "+it.get(2)+" от "+it.get(1);
          date = (String) it.get(1);
          actionType = (String) it.get(3);

          ObservableList row = FXCollections.observableArrayList(date, actionType, documentString);
          if(actionType.equals("ОТГРУЗКА"))
           row.addAll(amount,"");
          else row.addAll("",amount);
          vector.add(row);
          
          amount = 0.0;
          documentString = "";
          date = "";
          actionType = "";
        }
        
        map.setValue("vector",vector).setValue("startDate",Utility.format(start)).setValue("endDate",Utility.format(end));
        xmls = (String[]) ArrayUtils.add(xmls, FOP.get_XML_From_XMLTemplate(XMLTemplate, map.getSimpleMap()));
      }
      BytesFile bf = new BytesFile("Акт сверки", FOP.export_from_XML_to_bytea(Client.mergeXML(xmls), MimeConstants.MIME_PDF));
      response.setContentType("application/pdf");
      response.setContentLength(bf.getSource().length);
      response.getOutputStream().write(bf.getSource());
    }
  }
  
  private String getXML() throws ClassNotFoundException, Exception {
    String XML = null;
    List<List> data = Client.getData(DBFilter.create("Document").AND_EQUAL("name","Акт сверки"), "id");
    if(!data.isEmpty()) {
      data = Client.getData(DBFilter.create("DocumentXMLTemplate").AND_EQUAL("document",data.get(0).get(0)), "XML","main","name");
      for(List it:data) {
        if(it.get(2).equals("web")) {
          XML = (String)it.get(0);
          break;
        }
      }
      
      if(XML == null) {
        for(List it:data) {
          if((Boolean)it.get(1)) {
            XML = (String)it.get(0);
            break;
          }
        }
      }
      
      if(XML == null && !data.isEmpty())
        XML = (String)data.get(0).get(0);
    }
    return XML;
}
  
  private void getDocument(boolean download, HttpServletRequest request, HttpServletResponse response) throws Exception {
    Integer[] ids = new Integer[0];
    Map currentData = Client.getData(request);
    if(currentData.containsKey("ids")) {
      for(Object id:(List)currentData.get("ids"))
        ids = (Integer[]) ArrayUtils.add(ids, id);
    }else if(currentData.containsKey("id")) 
      ids = (Integer[]) ArrayUtils.add(ids, currentData.get("id"));
    
    if(ids.length > 0) {
      Session session = new Session();
      try {
        BytesFile[] bfs = new BytesFile[0];
        for(Integer id:ids) {
          List<List> data = session.executeQuery("select XML from [DocumentXMLTemplate] "
                  + "where [DocumentXMLTemplate(document)]=(select [CreatedDocument(document)] from [CreatedDocument] where [CreatedDocument(id)]=?) "
                  + "and [DocumentXMLTemplate(name)]='web'", new Object[]{id});
                  //+ "and [DocumentXMLTemplate(main)]=?", new Object[]{id,true});
          if(!data.isEmpty()) {
            Map map = getDocumentsData(session, new Integer[]{id});
            String xml = FOP.get_XML_From_XMLTemplate((String)data.get(0).get(0), PropertyMap.getListFromList(ObjectLoader.getList(JSModul.class, "script"), "script", String.class), map);
            bfs = (BytesFile[]) ArrayUtils.add(bfs, new BytesFile((String)map.get("fileName"), FOP.export_from_XML_to_bytea(xml, MimeConstants.MIME_PDF)));
          }
        }
        if(bfs.length > 0) {
          if(download) {
            if(ids.length == 1) {
              response.setContentType("application/pdf");
              response.setHeader("Content-Disposition","attachment; filename=\""+javax.mail.internet.MimeUtility.encodeWord(bfs[0].getName())+"\"");
              response.getOutputStream().write(bfs[0].getSource());
            }else {
              response.setContentType("application/zip");
              response.setHeader("Content-Disposition","attachment; filename=\""+javax.mail.internet.MimeUtility.encodeWord("документы.zip")+"\"");
              response.getOutputStream().write(GzipUtil.zipFiles(bfs));
            }
          }else {
            response.setContentType("application/pdf");
            response.setContentLength(bfs[0].getSource().length);
            response.getOutputStream().write(bfs[0].getSource());
          }
        }
      }catch(Exception ex) {
        ex.printStackTrace();
        throw new Exception(ex);
      }finally {
        session.commit();
        session.close();
        response.getOutputStream().flush();
        response.getOutputStream().close();
      }
    }else throw new Exception();
  }
  
  //7725608286
  
  private Map getDocumentsData(Session session, Integer[] ids) throws Exception {
    Map map = new HashMap();
    for(List d:session.executeQuery("SELECT "
            /*0*/+ "[CreatedDocument(params)], "
            /*1*/+ "[CreatedDocument(document_name)], "
            /*2*/+ "[CreatedDocument(number)], "
            /*3*/+ "(SELECT [Company(stamp)] FROM [Company] WHERE [Company(id)]=(SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(sellerCompanyPartition)])), "
            /*4*/+ "(SELECT [Company(chifSignature)] FROM [Company] WHERE [Company(id)]=(SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(sellerCompanyPartition)])), "
            /*5*/+ "(SELECT [Company(bookkeeperSignature)] FROM [Company] WHERE [Company(id)]=(SELECT [CompanyPartition(company)] FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(sellerCompanyPartition)])) "
            + "FROM [CreatedDocument] "
            + "WHERE [CreatedDocument(stornoDate)] ISNULL AND [CreatedDocument(id)]=ANY(?) "
            + "ORDER BY (SELECT trim(lower(replace(replace(replace([CompanyPartition(company_name)],'»',''),'«',''), '\"', ''))) FROM [CompanyPartition] WHERE [CompanyPartition(id)]=[CreatedDocument(customerCompanyPartition)]), "
            + "[CreatedDocument(customerCompanyPartition)], "
            + "[CreatedDocument(document)]", new Object[]{ids})) {
      try {
        map.putAll((Map)d.get(0));
      }catch(Exception ex) {
        try {
          map.putAll(GzipUtil.getObjectFromGzip((byte[])d.get(0), Map.class));
        }catch(Exception e) {
          map.putAll(GzipUtil.deserializable((byte[])d.get(0), Map.class));
        }
      }
      map.put("fileName", d.get(1)+" № "+d.get(2)+".pdf");
      map.put("stamp", Utility.writeBytesToFile((byte[])d.get(3)));
      map.put("chifSignature", Utility.writeBytesToFile((byte[])d.get(4)));
      map.put("bookkeeperSignature", Utility.writeBytesToFile((byte[])d.get(5)));
    }
    return map;
  }
  
  private Map showDocs(HttpServletRequest request) throws RemoteException {
    Integer[] contracts = new Integer[0];
    Map currentData = Client.getData(request);
    for(Object id:(List)currentData.get("contract"))
      contracts = (Integer[]) ArrayUtils.add(contracts, id);
    Integer year  = Integer.valueOf(String.valueOf(currentData.get("year")));
    Integer month = Integer.valueOf(String.valueOf(currentData.get("month")));
    long start,end;
    
    Calendar c = Calendar.getInstance();
    c.set(year, month, 1);
    start = c.getTimeInMillis();

    c.set(year, month, c.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
    c.set(Calendar.MILLISECOND, 999);
    end = c.getTimeInMillis();
    
    // 7734695207
    
    List<List> db = contracts.length == 0 ? new Vector() : Client.getData("select "
            + "id, "
            + "name, "
            + "number, "
            + "date, "
            + "(select count(id) from [DocumentXMLTemplate] where [DocumentXMLTemplate(name)]='web' and [DocumentXMLTemplate(document)]=[CreatedDocument(document)] and type='CURRENT' and tmp=false)"
            + "from [CreatedDocument] where tmp = false and type = 'CURRENT' and "
            + "[CreatedDocument(document-system)]=true and "
            + "(id in (select [CreatedDocument(dealPositions):object] from "
            + "  [CreatedDocument(dealPositions):table] where [CreatedDocument(dealPositions):target] in "
            + "    (select id from [DealPosition] where [DealPosition(deal)] in "
            + "      (select id from [Deal] where [Deal(contract)]=ANY(?)))) "
            + "or "
            + "[CreatedDocument(payment)] in (select [DealPayment(payment)] from [DealPayment] where "
            + "  [DealPayment(deal)] in (select id from [Deal] where [Deal(contract)]=ANY(?)))"
            + ") and "
            + "date between ? and ? order by date DESC", 
            new Object[]{contracts, contracts, new Timestamp(start), new Timestamp(end)});
    if(db != null) {
      List data = new ArrayList();
      for(List d:db) {
        data.add(PropertyMap.create()
                .put("id",      d.get(0))
                .put("name",    d.get(1))
                .put("number",  d.get(2))
                .put("date",  Utility.format((Timestamp)d.get(3)))
                .put("templ", d.get(4)).getSimpleMap());
      }
      return Client.OK(data);
    }
    return Client.ERROR("Ошибка запроса в БД");
  }
  
  private Map getYears(HttpServletRequest request) throws RemoteException {
    List<List> db = Client.getData("select extract(YEAR from max(date))::int, extract(YEAR from min(date))::int from [CreatedDocument] where "
            + "[CreatedDocument(document-system)]=true and "
            + "tmp=false and type='CURRENT' and "
            + "[CreatedDocument(customerCompanyPartition)] in (select id from [CompanyPartition] where [CompanyPartition(company)]=?)",
            new Object[]{Client.getClientId(request)});
    if(db != null) {
      Map data = new HashMap();
      db.stream().forEach(d -> {
        data.put("max", d.get(0));
        data.put("min", d.get(1));
      });
      return Client.OK(data);
    }
    return Client.ERROR("Ошибка запроса в БД");
  }
}
