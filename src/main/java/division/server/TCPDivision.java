package division.server;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class TCPDivision {
  private final ServerSocket server;
  private final int tcpPort;

  public TCPDivision(ServerSocket server, int tcpPort) {
    this.server = server;
    this.tcpPort = tcpPort;
  }
  
  public void start() throws Exception {
    ServerSocket serverSocket = new ServerSocket(tcpPort);
    while(true) {
      Thread t = new Thread(new Command(serverSocket.accept()));
      t.setDaemon(true);
      t.start();
    }
  }
  
  class Command implements Runnable {
    private Socket clientSocket;

    public Command(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }
    
    @Override
    public void run() {
      /*BufferedReader in = null;
      PrintWriter out = null;
      String msg;
      try {
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        while((msg = in.readLine()) != null) {
          JSONObject json = JSONObject.fromObject(msg);
          switch(json.getString("command")) {
            case "get-system-document":
              break;
            case "executeQuery":
              List<List> data = server.createSession(null, true).executeQuery(json.getString("query"));
              JSONObject answ = new JSONObject();
              answ.accumulate("size", data.size());
              for(int i=0;i<data.size();i++) {
                for(int j=0;j<data.get(i).size();j++)
                  answ.accumulate(String.valueOf(i)+","+String.valueOf(j), data.get(i).get(j).toString());
              }
              
              break;
          }
        }
      }catch(Exception ex) {
      }finally {
        if(in != null) {
          try{in.reset();}catch(Exception ex){}
          try{in.close();}catch(Exception ex){}
        }
        if(out != null) {
          try{out.flush();}catch(Exception ex){}
          try{out.close();}catch(Exception ex){}
        }
        in = null;
        out = null;
        try{clientSocket.close();}catch(Exception ex){}
      }*/
    }
  }
}
