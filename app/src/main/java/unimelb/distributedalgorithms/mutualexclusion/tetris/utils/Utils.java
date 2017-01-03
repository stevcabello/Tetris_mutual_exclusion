package unimelb.distributedalgorithms.mutualexclusion.tetris.utils;

import android.util.Log;

public class Utils {

	private final static String TAG = "utils";


	/**
	 * Send message to peers
	 * @param gamemsg
	 */
	public static void sendGameMsg(String gamemsg){

		Log.i(TAG,"Send Game Msg: " + gamemsg);

		String localIP = Globals.peer.getAddressPeer();

		for (String peerIP: Globals.peer.getPeerList()){
			if (!peerIP.equals(localIP))
				Globals.peer.pingToPeer(peerIP,gamemsg);

		}

	}


}
