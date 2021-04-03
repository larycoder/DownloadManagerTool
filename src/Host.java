import java.rmi.*;
import java.io.*;


public class Host implements Serializable {
	String host;
	int port;
	int hb = 0;

	public Host(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {return this.host;}
	public int getPort() {return this.port;}

	public boolean updateHeartBeat(String host, int port, int hb) {
		if(this.host.equals(host) && this.port == port) {
			this.hb = hb;
			return true;
		}
		return false;
	}

	public int getHeartBeat() {return this.hb;}
}
