package unimelb.distributedalgorithms.mutualexclusion.tetris.sip2peer;

/*
 * Copyright (C) 2010 University of Parma - Italy
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Designer(s):
 * Marco Picone (picone@ce.unipr.it)
 * Fabrizio Caramia (fabrizio.caramia@studenti.unipr.it)
 * Michele Amoretti (michele.amoretti@unipr.it)
 * 
 * Developer(s)
 * Fabrizio Caramia (fabrizio.caramia@studenti.unipr.it)
 * 
 */

import java.util.ArrayList;

import it.unipr.ce.dsg.s2p.org.json.JSONException;
import it.unipr.ce.dsg.s2p.org.json.JSONObject;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;
import it.unipr.ce.dsg.s2p.sip.Address;
import unimelb.distributedalgorithms.mutualexclusion.tetris.game.PlayActivity;

/**
 * Class <code>SimplePeer</code> implements many features of a peer.
 * SimplePeer extend the Peer class of sip2peer.
 * 
 * 
 * @author Fabrizio Caramia
 *
 */

public class SimplePeer extends Peer {
	
	private Address bootstrapPeer = null;
	private PlayActivity peerActivity=null;
	private ArrayList<String> peerList = null;
	
	
	public SimplePeer(String pathConfig, String key, String peerName,
			int peerPort) {
		super(pathConfig, key, peerName, peerPort);
		
	}


	public  void setPeerList(ArrayList<String> peers){
		this.peerList = peers;
	}

	public ArrayList<String> getPeerList(){
		return peerList;
	}

	public String getAddressPeer(){

		return getAddress().getURL();
	}
	

	
	public void pingToPeer(String address,String msg){

		PingMessage newPingMsg = new PingMessage(peerDescriptor,msg);

		//!!!!!!send to local address 
		send(new Address(address), newPingMsg);

	}

	

	@Override
	protected void onReceivedJSONMsg(final JSONObject jsonMsg, Address sender) {
		
		try {
			JSONObject params = jsonMsg.getJSONObject("payload").getJSONObject("params");

			//Sends message received from peer in order to execute the gesture received
			PlayActivity.handler.obtainMessage(0,jsonMsg.get("type").toString()).sendToTarget();

			PeerDescriptor neighborPeerDesc = new PeerDescriptor(params.get("name").toString(), params.get("address").toString(), params.get("key").toString(), params.get("contactAddress").toString());
			addNeighborPeer(neighborPeerDesc);


		} catch (JSONException e) {
		
			e.printStackTrace();
		}
		
		
	}

	public void setPeerActivity(PlayActivity peerActivity) {
		this.peerActivity=peerActivity;
		
	}

	@Override
	protected void onDeliveryMsgFailure(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onDeliveryMsgSuccess(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}

		

}
