package client;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import de.fhac.mazenet.server.networking.UTFInputStream;
import de.fhac.mazenet.server.networking.UTFOutputStream;

public class SSLConnectionTest {
	public class SSLServer {
		public SSLServer() {
			try {
				System.setProperty("javax.net.ssl.keyStore", "rn-ssl.jks");
				System.setProperty("javax.net.ssl.keyStorePassword", "geheim");
				
				System.out.println("Server: Server starts");
				
				SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
				SSLServerSocket sslserversocket = (SSLServerSocket) factory.createServerSocket(1234);
				System.out.println("Server: Waiting for client....");
				SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
				System.out.println("Server: Connected to Client " + sslsocket.getInetAddress().getHostAddress());

				UTFInputStream is = new UTFInputStream(sslsocket.getInputStream());
				UTFOutputStream os = new UTFOutputStream(sslsocket.getOutputStream());
//				DataInputStream is = new DataInputStream(sslsocket.getInputStream());
//				DataOutputStream os = new DataOutputStream(sslsocket.getOutputStream());
				System.out.println("Server: In- and Output Streams have been initiliazed");
				
				System.out.println("Server: Receiving Message from Client...");
				String input = is.readUTF8();
				System.out.println("Server: Received from Client: " + input);
				String ketqua = input.toUpperCase();
				os.writeUTF8(ketqua);
					
			} catch (IOException e) {
				System.out.print(e);
			}
		}
	}

	public class SSLClient {
		public SSLClient() {
			try {
				System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
				System.setProperty("javax.net.ssl.trustStorePassword", "geheim");
				
				System.out.println("Client: client starts");
				
				SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				SSLSocket sslsocket = (SSLSocket) factory.createSocket("127.0.0.1", 1234);

				UTFInputStream is = new UTFInputStream(sslsocket.getInputStream());
				UTFOutputStream os = new UTFOutputStream(sslsocket.getOutputStream());
//				DataOutputStream os = new DataOutputStream(sslsocket.getOutputStream());
//				DataInputStream is = new DataInputStream(sslsocket.getInputStream());

				String str = "test";
				
				System.out.println("Client: Sending to server...");
				os.writeUTF8(str);

				String responseStr;
				
				while(true)
				{
					System.out.println("Client: Waiting for response...");
					if ((responseStr = is.readUTF8()) != null) {
						System.out.println(responseStr);
						break;
					}
				}

				os.close();
				is.close();
				sslsocket.close();
			} catch (UnknownHostException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public static void main(String[] args) {
		SSLConnectionTest test = new SSLConnectionTest();
		Thread server;
		Thread client;
		server = new Thread(() -> test.new SSLServer());
		client = new Thread(() -> test.new SSLClient());
		server.start();
		client.start();
	}
}
