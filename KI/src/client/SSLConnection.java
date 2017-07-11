package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.UnmarshalException;

import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.networking.XmlInStream;
import de.fhac.mazenet.server.networking.XmlOutStream;

public class SSLConnection {
	private static final String truststorePath = "resources/truststore.jks"; // auf "myTrustStore" umstellen, wenn nicht getestet wird
	private static final String password = "geheim"; // auf "" umstellen, wean nicht getestet wird
	
	private XmlOutStream outputStream;
	private XmlInStream inputStream;
	private SSLSocket sslSocket;
	private File tempFile;
	
	public SSLConnection(String ip, int port) throws UnknownHostException, IOException {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		InputStream stream = currentClassLoader.getResourceAsStream(truststorePath);
		
		File tmpFile = File.createTempFile("truststore", "");
		FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
		int read = 0;
		byte[] b = new byte[1024];
		while ((read = stream.read(b)) != -1) {
			fileOutputStream.write(b, 0, read);
		}
		fileOutputStream.close();
		
		System.setProperty("javax.net.ssl.trustStore", tmpFile.getAbsolutePath());
		System.setProperty("javax.net.ssl.trustStorePassword", password);
		
		stream.close();

		System.out.println("Client: client starts");

		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		sslSocket = (SSLSocket) factory.createSocket(ip, port);

		outputStream = new XmlOutStream(sslSocket.getOutputStream());
		inputStream = new XmlInStream(sslSocket.getInputStream());
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
