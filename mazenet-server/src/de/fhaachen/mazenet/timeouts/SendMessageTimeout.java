package de.fhaachen.mazenet.timeouts;

import java.util.TimerTask;

import de.fhaachen.mazenet.generated.ErrorType;
import de.fhaachen.mazenet.networking.Connection;

public class SendMessageTimeout extends TimerTask {

	private Connection con;

	public SendMessageTimeout(Connection con) {
		this.con = con;
	}

	@Override
	public void run() {
		this.con.disconnect(ErrorType.TIMEOUT);
	}

}
