package unimelb.distributedalgorithms.mutualexclusion.tetris;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import unimelb.distributedalgorithms.mutualexclusion.tetris.game.PlayActivity;
import unimelb.distributedalgorithms.mutualexclusion.tetris.utils.Globals;

/**
 * Created by pc on 5/16/2016.
 */
public final class TetrisPeer implements ITetrisPeer {
    public String TAG = "TetrisPeer";
    public TetrisPeer(String id) {
        this.id = id;
    }

    /** The ID of this {@link TetrisPeer}. */
    private String id;


    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public boolean sendMessage(String senderID, String msg) {

        try {

            JSONObject json = new JSONObject();
            try {
                json.put(PlayActivity.MESSAGE_SENDER_ID, senderID);
                json.put(PlayActivity.MESSAGE_TYPE, PlayActivity.ALGORITHM_MESSAGE);
                json.put(PlayActivity.MESSAGE_CONTENT, msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            printFormattedSendMessage(msg);

            Globals.peer.pingToPeer(id, json.toString());

        } catch (Exception e) {
            Log.e(TAG, "Failed to send message to " + id);
            return false;
        }
        return true;
    }




    @Override
    public int compareTo(ITetrisPeer peer) {
        return getID().compareTo(peer.getID());
    }

    @Override
    public String toString() {
        return id;
    }

    public static ITetrisPeer fromString(String from) {
        return new TetrisPeer(from);
    }

    public boolean equals(ITetrisPeer other) {
        return id.equals(other.getID());
    }


    /**
     * Helper that formats the messages sent between
     * the algorithms to a more readable format.
     *
     * @param msg  message to format.
     */
    private void printFormattedSendMessage(String msg){
        //To print on the log the message sent by this pe
        String recipient = id.split("@")[0];
        String message = "";
        String timestamp = "";

        switch (PlayActivity.currentAlgo){
            case PlayActivity.LOGICALCLOCK_ALGO:
                TimeStampMessage tm =TimeStampMessage.fromString(msg);
                message = tm.getMessage();
                timestamp = " " + Integer.toString(tm.getTimestamp());
                break;
            case PlayActivity.TOKEN_ALGO:
                message = msg;
                timestamp = "";
                break;
            case PlayActivity.QUORUM_ALGO:
                QuorumMessage qm =QuorumMessage.fromString(msg);
                message = qm.getMessage();
                timestamp = " " + qm.getTimestamp().getArrayString();
                break;
        }
        String outMsg = "Sending " + message + " to " + recipient + timestamp;
        PlayActivity.handler.obtainMessage(0, outMsg).sendToTarget();

    }

}
