package de.fhac.mazenet.server.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by bongen on 21.06.17.
 */
public class TCPConnectionCreationTask implements Runnable{
    private Socket incoming;
    private ServerSocket serverSocket;
    private CyclicBarrier barrier;

    public TCPConnectionCreationTask(ServerSocket serverSocket, CyclicBarrier barrier){
        this.serverSocket =serverSocket;
        this.barrier = barrier;

    }
    public Socket getIncoming() {
        return incoming;
    }

    @Override
    public void run() {
        try {
            incoming = serverSocket.accept();
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(de.fhac.mazenet.server.Messages.getString("Game.errorWhileConnecting")); //$NON-NLS-1$
            System.err.println(e.getMessage());
        }
    }
}
