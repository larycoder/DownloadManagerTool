import java.rmi.*;
import java.util.*;


public interface Consultation extends Remote {
	public Collection<Host> listDataNode() throws RemoteException;
	public Host getDataNode(int index) throws RemoteException;
	public boolean refreshDataNode(Host h, int index) throws RemoteException;
}
