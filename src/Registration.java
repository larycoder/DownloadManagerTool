import java.rmi.*;


public interface Registration extends Remote {
	public int registerDataNode(Host h) throws RemoteException;
}
