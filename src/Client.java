import java.rmi.*;
import java.util.*;
import java.net.*;
import java.io.*;


public class Client implements ManageFile, Runnable {
	private static String host;
	private static String name;
	private Socket s;
	private String filename;
	private String action;

	public Client() {;} // default constructor
	public Client(Socket s, String action, String filename) {
		this.s = s;
		this.action = action;
		this.filename = filename;
	}
	
	private Collection<Host> listNameNode() {
		try {
			Consultation nns = (Consultation) Naming.lookup("//"+host+"/"+name);
			return (Collection<Host>) nns.listDataNode();
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void run() {
		if(this.action.equals("store")) {
			try {
				File f = new File(this.filename);

				// validate file
				if(!f.exists() || f.isDirectory()) {
					throw new IOException("Method expects exists file !!!");
				} else {
					System.out.println("[DEBUG] file info: "+f.toString());
				}

				// prapre connection
				OutputStream os = this.s.getOutputStream();
				InputStream is = this.s.getInputStream();
				PrintStream ps = new PrintStream(os);
				InputStreamReader isr = new InputStreamReader(is);
				LineNumberReader lr = new LineNumberReader(isr);
	
				// send header
				ps.println("upload");
				ps.println(f.getName());
				ps.println(f.length());

				
				// send data
				byte[] buffer = new byte[5000];
				InputStream fis = new FileInputStream(f);
				while(fis.available() > 0) {
					// wait for ready signal
					if(!lr.readLine().startsWith("200")) {
						throw new Exception("file transfer process is break in middle !!!");
					}

					// send data
					int len = fis.read(buffer, 0, buffer.length);
					os.write(buffer, 0, len);
				}

				// wait for final confirm status
				if(!lr.readLine().startsWith("201")) {
					throw new Exception("file transfered but could not received 201 status !!!");
				}

				// close connection
				fis.close();
				ps.close();
				os.close();
				lr.close();
				isr.close();
				is.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else if(this.action.equals("download")) {
			// TODO: implement download action
			;
		}
	}

	public void store(String filename) {
		Collection<Host> hostList = this.listNameNode();
		if(hostList != null) {
			for(Host h: hostList) {
				try {
					System.out.println("[DEBUG] get DataNode ("+h.getHost()+", "+h.getPort()+")");
					new Thread(new Client(new Socket(h.getHost(), h.getPort()), "store", filename)).start();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("[INFO] get DataNode: Empty DataNode");
		}
	}

	public void download(String filename) {;}

	public static void main(String args[]) {
		// start client
		Client c = new Client();

		// get rmi host
		host = args[0];
		name = args[1];

		// get action
		if(args[2].equals("store")) {
			// TODO: store data
			System.out.println("[DEBUG] action request: "+args[2]);
			c.store(args[3]);
		} else if(args[2].equals("download")) {
			// TODO: download data
			;
		}
	}	
}
