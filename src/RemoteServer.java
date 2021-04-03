import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;


public class RemoteServer extends UnicastRemoteObject {
	public RemoteServer() throws RemoteException {;}

	protected void bind(RemoteServer obj, 
			      String host, String name) throws RemoteException {
		try{
			System.setProperty("java.rmi.server.hostname", host);
			Naming.rebind("//"+host+"/"+name, obj);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
