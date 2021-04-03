import java.rmi.*;
import java.net.*;
import java.io.*;

public class DNServer implements Runnable {
	public static int hostIndex;
	public static Host host; // fixed host
	public static String nnHost;
	public static String nnName;
	
	private Socket s;

	public DNServer(Socket s) {this.s = s;}

	private void uploadFile(String filename, InputStream is, OutputStream os, int size) throws IOException {
		PrintStream ps = new PrintStream(os);
		File f = new File("tmp/"+host.getHost()+"/"+String.valueOf(host.getPort())+"/"+filename);
		System.out.println("[DEBUG] generate file in path: "+f.getPath());
		if(!f.exists()) {
			f.createNewFile();
		}
		
		// write data to file
		FileOutputStream fos = new FileOutputStream(f);
		byte[] buffer = new byte[5000];
		while(size > 0) {
			ps.print("200\n");
			int len = is.read(buffer, 0, buffer.length);
			fos.write(buffer, 0, len);
			size -= len;
		}
		ps.print("201\n");
		
		ps.close();
		fos.close();
	}

	public void run() {
		try {
			System.out.println("[DEBUG] socket info: "+this.s.toString());
			InputStream is = this.s.getInputStream();
			OutputStream os = this.s.getOutputStream();
			InputStreamReader ir = new InputStreamReader(is);
			LineNumberReader lr = new LineNumberReader(ir);

			// get intention of request
			String action = lr.readLine();
			System.out.println("[DEBUG] received action: "+action);
			if(action.equals("upload")) {
				// TODO: upload file
				String filename = lr.readLine();
				int size = Integer.parseInt(lr.readLine());
				uploadFile(filename, is, os, size);
			} else if(action.equals("download")) {
				// TODO: download file
				;
			}

			lr.close();
			ir.close();
			is.close();
			os.close();
		} catch(Exception e) {
			e.printStackTrace();
		}	
	}

	public static void main(String args[]) {
		try {
			// setup env
			host = new Host(args[0], Integer.parseInt(args[1]));
			nnHost = args[2];
			nnName = args[3];
			System.out.println("[DEBUG] lookup rmi name //"+nnHost+"/"+nnName);
			Registration nns = (Registration) Naming.lookup("//"+args[2]+"/"+args[3]);
			hostIndex = nns.registerDataNode(host);
			nns = null;
			
			// run heart beat updator
			new Thread(new HeartBeatUpdator()).start();
			
			// TODO: loop to wait TCP connection
			ServerSocket s = new ServerSocket(host.getPort());
			while(true) {
				new Thread(new DNServer(s.accept())).start();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	static class HeartBeatUpdator implements Runnable {
		public void run() {
			while(true) {
				try {
					Thread.sleep(2000);
					Consultation nns = (Consultation) Naming.lookup("//"+DNServer.nnHost+"/"+DNServer.nnName);
					Host h = nns.getDataNode(DNServer.hostIndex);
					Host dnHost = DNServer.host;
					if(h.getPort() < 0 || !h.updateHeartBeat(dnHost.getHost(), dnHost.getPort(), 0)) {
						// TODO: register new slot
						;
					} else if (!nns.refreshDataNode(dnHost, DNServer.hostIndex)) {
						// TODO: register new slot
						;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
