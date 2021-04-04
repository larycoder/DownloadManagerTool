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

	private void uploadFile(String filename, InputStream is, OutputStream os, long size) throws IOException {
		PrintStream ps = new PrintStream(os);
		File f = new File("tmp/"+host.getHost()+"/"+String.valueOf(host.getPort())+"/"+filename);
		System.out.println("[DEBUG] generate file in path: "+f.getPath());
		if(!f.exists()) {
			new File(f.getParent()).mkdirs();
			f.createNewFile();
		}
		
		// write data to file
		FileOutputStream fos = new FileOutputStream(f);
		byte[] buffer = new byte[5000];
		while(size > 0) {
			ps.print("200\n");
			int len = is.read(buffer, 0, buffer.length);
			fos.write(buffer, 0, len);
			fos.flush();
			size -= len;
		}
		ps.print("201\n");
		
		ps.close();
		fos.close();
	}

	private void downloadFile(String filename, InputStream is, OutputStream os) throws IOException {
		PrintStream ps = new PrintStream(os);
		InputStreamReader ir = new InputStreamReader(is);
		LineNumberReader lr = new LineNumberReader(ir);
		
		File f = new File("tmp/"+host.getHost()+"/"+String.valueOf(host.getPort())+"/"+filename);
		System.out.println("[DEBUG] request to download file: "+f.getPath());
		if(f.isDirectory() || !f.exists()) {
			ps.println("500");
		} else {
			System.out.println("[DEBUG] start stream download");

			// header handshake
			ps.println("200");
			ps.println(f.length());
			
			String processCode = lr.readLine();
			if(processCode.startsWith("200")) {
				System.out.println("[DEBUG] requester stop process in download handshake step");
				ps.println("200");
			} else if(!processCode.startsWith("100")) {
				throw new IOException("Download handshake get unexpected code");
			} else {
				long offset = Long.parseLong(lr.readLine());
				long size = Long.parseLong(lr.readLine());
				ps.println("200");

				System.out.println("[DEBUG] download request with offset: "+offset+" and size: "+size);

				// send file process
				InputStream fis = new FileInputStream(f);
				byte[] buffer = new byte[5000];
				fis.skip(offset);
				while(size > 0 && fis.available() > 0) {

					//System.out.println("[DEBUG] sending chunk file with remain chunk size: "+size);

					if(!lr.readLine().startsWith("200")) {
						fis.close();
						throw new IOException("File transfer broken - could not get 200 status !!!");
					}

					// retrieve and send data
					int len = fis.read(buffer, 0, buffer.length);
					if(len < size) {
						os.write(buffer, 0, len);
						os.flush();
					} else {
						os.write(buffer, 0, (int) size);
						os.flush();
					}
					size -= len;
				}

				System.out.println("[DEBUG] end stream download");

				// close file stream
				fis.close();
			}
		}

		// close stream
		ps.close();
		lr.close();
		ir.close();
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
				long size = Long.parseLong(lr.readLine());
				uploadFile(filename, is, os, size);
			} else if(action.equals("download")) {
				// TODO: download file
				String filename = lr.readLine();
				downloadFile(filename, is, os);
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
					// TODO: register new slot
					;
				}
			}
		}
	}
}
