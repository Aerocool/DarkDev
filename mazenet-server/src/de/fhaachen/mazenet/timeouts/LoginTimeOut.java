package de.fhaachen.mazenet.timeouts;

import java.util.TimerTask;

import de.fhaachen.mazenet.server.Game;

public class LoginTimeOut extends TimerTask {
	private Game currentGame;

	public LoginTimeOut(Game currentGame) {
		super();
		this.currentGame = currentGame;
	}

	@Override
	public void run() {
		currentGame.closeServerSocket();
	}
	

}
