package client;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.UnmarshalException;

import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.networking.XmlInStream;
import de.fhac.mazenet.server.networking.XmlOutStream;

public class SSLConnection {
	private static final String truststorePath = "myTrustStore"; // auf "public-key_maze-server.crt" umstellen, wenn nicht getestet wird
	private static final String password = "geheim";
	
	private XmlOutStream outputStream;
	private XmlInStream inputStream;
	private SSLSocket sslSocket;
	
	public SSLConnection(String ip, int port) throws UnknownHostException, IOException {
		System.setProperty("javax.net.ssl.trustStore", truststorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", password);

		System.out.println("Client: client starts");

		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		sslSocket = (SSLSocket) factory.createSocket(ip, port);

		outputStream = new XmlOutStream(sslSocket.getOutputStream());
		inputStream = new XmlInStream(sslSocket.getInputStream());

		// os.close();
		// is.close();
		// sslsocket.close();
	}
	
	public void sendToServer(MazeCom mazecom) {
		outputStream.write(mazecom);
	}
	
	public MazeCom receiveFromServer() {
		try {
			return inputStream.readMazeCom();
		} catch (UnmarshalException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void closeConnection(){
		try {
			outputStream.close();
			inputStream.close();
			sslSocket.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
