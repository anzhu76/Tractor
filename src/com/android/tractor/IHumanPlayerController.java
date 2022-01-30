package com.android.tractor;

import java.util.concurrent.Semaphore;

public interface IHumanPlayerController {

	public void setPlayer(HumanPlayer player);
	public HumanPlayer getPlayer();
	
	void SetGameControllerMode(int mode, String id);
	void SendNotification(Object o);
	
	Semaphore getGameControllerLock();
}
