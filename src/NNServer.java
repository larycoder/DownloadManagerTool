import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;


public class NNServer extends RemoteServer implements Registration, Consultation {
	// this is singleton list for managing DataNode
	public static Host[] hostList = new Host[100]; // maximum 100 DataNodes
	public static Object lock = new Object(); // lock to work with hosts list

	public NNServer() throws RemoteException {;}

	private boolean isDeadNode(Host h) throws RemoteException{
		try {
			// check connection
			int t = ((int) new Date().getTime());
			if(t - h.getHeartBeat() > 6000) {
				return true;
			}
		} catch(Exception e) {
			return true;
		}
		return false;
	}

	public boolean refreshDataNode(Host h, int index) throws RemoteException{
		synchronized(lock) {
			try{
				int t = (int) new Date().getTime();
				return hostList[index].updateHeartBeat(h.getHost(), h.getPort(), t);
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public int registerDataNode(Host h) throws RemoteException {
		synchronized(lock) {
			for(int i = 0; i < 100; i++) {
				if(this.hostList[i] == null) {
					this.hostList[i] = h;
					return i;
				}
			}
		}
		return -1;
	}

	public Host getDataNode(int index) throws RemoteException {
		try {
			return hostList[index];
		} catch(Exception e) {
			e.printStackTrace();
			return new Host("0.0.0.0", -1);
		}
	} 

	public Collection<Host> listDataNode() throws RemoteException {
		synchronized(lock) {
			Collection<Host> listNode = new ArrayList<Host>();
			try {
				for(int i = 0; i < 100; i++) {
					if(hostList[i] != null) {
						if(isDeadNode(hostList[i])) {
							hostList[i] = null;
						} else {
							Host h = hostList[i];
							listNode.add(h);
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
				return new ArrayList<Host>();
			}
			return listNode;
		}
	}
	
	public static void main(String args[]) {
		try {
			NNServer nns = new NNServer();
			nns.bind(nns, args[0], args[1]);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
