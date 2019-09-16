/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package division.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author russo
 */
public class TestBean extends UnicastRemoteObject implements Remote {
  private String message;

  public TestBean() throws RemoteException {
    super();
  }

  public String getMessage() throws RemoteException {
    return message;
  }

  public void setMessage(String message) throws RemoteException {
    this.message = message;
  }
}
