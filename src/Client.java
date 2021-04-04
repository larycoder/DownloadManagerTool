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
	private long offset;
	private long size;
	private int index;

	public Client() {;} // default constructor
	public Client(Socket s, String action, String filename) {
		this.s = s;
		this.action = action;
		this.filename = filename;
	}

	public Client(Socket s, String action, String filename, long offset, long size, int index) {
		this(s, action, filename);
		this.offset = offset;
		this.size = size;
		this.index = index;
	}
	
	private Collection<Host> listNameNode() {
		try {
			System.out.println("[DEBUG] binding to rmi naming service at: "+"//"+host+"/"+name);
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

				// prepare connection
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
			try {
				File f = new File(this.filename);

				// validate file
				if(f.exists() || f.isDirectory()) {
					throw new IOException("Method expects non exists file !!!");
				} else {
					System.out.println("[DEBUG] file info: "+f.toString());
				}

				// prepare connection
				OutputStream os = this.s.getOutputStream();
				InputStream is = this.s.getInputStream();
				PrintStream ps = new PrintStream(os);
				InputStreamReader ir = new InputStreamReader(is);
				LineNumberReader lr = new LineNumberReader(ir);

				// header handshake
				ps.println("download");
				ps.println(f.getName());
				if(!lr.readLine().startsWith("200")) {
					throw new Exception("Handshake request file fail, could not get 200 status !!!");
				}
				lr.readLine(); // this handshake is for main thread process, ignore in here
				ps.println("100");
				ps.println(offset);
				ps.println(size);
				if(!lr.readLine().startsWith("200")) {
					throw new Exception("Handshake process broken, could not get 200 status !!!");
				}

				// receive data
				OutputStream fos = new FileOutputStream(new File(f.getPath()+"$"+this.index));
				byte[] buffer = new byte[5000];
				while(size > 0) {
					//System.out.println("[DEBUG] remain filesize: "+size);

					ps.println("200");
					int len = is.read(buffer, 0, buffer.length);
					fos.write(buffer, 0, len);
					size -= len;	
				}

				// close stream
				fos.close();
				ps.close();
				os.close();
				lr.close();
				ir.close();
				is.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
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

	public void download(String filename) {
		Collection<Host> hostList = this.listNameNode();
		if(hostList != null) {
			// truth ground size
			long filesize = 0;
			Collection<FileChunkBuilder> fcbList = new ArrayList<FileChunkBuilder>();
			

			// validate DataNode connection
			for(Host h: hostList) {
				FileChunkBuilder fcb = new FileChunkBuilder(h, "download", filename);
				fcb.validateDataNode();
				if(fcb.s == -1) {
					; // we don't use hostList anymore hence ignore this
				} else {
					filesize = fcb.s;
					fcbList.add(fcb);
				}
			}

			System.out.println("[DEBUG] end validate DataNode");

			if(fcbList.size() <= 0) {
				 new Exception("Could not get DataNode have expected file !!!").printStackTrace();
				 return;
			}

			// update filename, offset and size of each chunk
			long chunkSize = (filesize + fcbList.size() - (filesize%fcbList.size())) / fcbList.size();
			FileChunkBuilder[] fcbArr = fcbList.toArray(new FileChunkBuilder[0]);
		       	fcbList.toArray(fcbArr);
			for(int i=0; i < fcbList.size(); i++) {
				FileChunkBuilder fcb = fcbArr[i];
				fcb.index = i;
				fcb.offset = chunkSize*i;
				if(fcb.offset + chunkSize < filesize) {
					fcb.s = chunkSize;
				} else {
					fcb.s = filesize - fcb.offset;
				}
			}

			System.out.println("[DEBUG] begin download chunk file");
			
			// download chunk
			try {
				Collection<Thread> t = new ArrayList<Thread>();
				for(int i=0; i < fcbArr.length; i++) {
					FileChunkBuilder fcb = fcbArr[i];
					Host h = fcb.h;
					System.out.println("[DEBUG] get DataNode ("+h.getHost()+", "+h.getPort()+")");
					Thread tt = new Thread(new Client(new Socket(h.getHost(), h.getPort()), "download", fcb.f, fcb.offset, fcb.s, fcb.index));
					tt.start();
					t.add(tt);
				}
				
				// wait for all thread done
				for(Thread tt: t) {
					tt.join();
				}

				// TODO: merge file to single file
				File f = new File(filename);
				OutputStream fos = new FileOutputStream(f, true);
				byte[] buffer = new byte[5000];
				for(int i = 0; i < fcbArr.length; i++) {
					File tempFile = new File(fcbArr[i].f+"$"+fcbArr[i].index);
					InputStream fis = new FileInputStream(tempFile);
					while(fis.available() > 0) {
						int len = fis.read(buffer, 0, buffer.length);
						fos.write(buffer, 0, len);
					}

					fis.close();
					tempFile.delete();
				}

				// close
				fos.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("[INFO] get DataNode: Empty DataNode");
		}
	}

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
			long startTime = System.currentTimeMillis();
			System.out.println("[DEBUG] action request: "+args[2]);
			c.download(args[3]);
			System.out.println("[INFO] download time in milliseconds: "+(System.currentTimeMillis() - startTime)+" s");
		}
	}

	class FileChunkBuilder {
		private String action;
		private String filename;
		public Host h;

		public String f = null;
		public long s = -1;
		public long offset = -1;
		public int index = -1;

		public FileChunkBuilder(Host h, String action, String filename) {
			this.action = action;
			this.filename = filename;
			this.h = h;
		}

		public void validateDataNode() {
			try {
				System.out.println("[DEBUG] start run DataNode validate process.");
				File f = new File(filename);
				Socket s = new Socket(h.getHost(), h.getPort());
				OutputStream os = s.getOutputStream();
				InputStream is = s.getInputStream();
				PrintStream ps = new PrintStream(os);
				InputStreamReader ir = new InputStreamReader(is);
				LineNumberReader lr = new LineNumberReader(ir);
	
				// header handshake
				ps.println(action);
				ps.println(f.getName());
				if(lr.readLine().startsWith("200")) {
					long size = Long.parseLong(lr.readLine());
					ps.println("200");
					if(lr.readLine().startsWith("200")) {
						this.f = this.filename;
						this.s = size;
					}
				}

				// close
				lr.close();
				ir.close();
				ps.close();
				os.close();
				is.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
