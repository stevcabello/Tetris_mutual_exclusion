package unimelb.distributedalgorithms.mutualexclusion.tetris;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * message class for Ricart Agrawala algorithm
 */
public final class TimeStampMessage {
    private static final String SENDER_ID = "SenderID";
    private static final String TIMESTAMP = "TimeStamp";
    private static final String MESSAGE = "Message";

    private final String senderID;
    private final int timestamp;
    private final String message;

    TimeStampMessage(String senderID, int timestamp, String message) {
        this.senderID = senderID;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getSenderID() {
        return senderID;
    }

    public int getTimestamp () {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public static TimeStampMessage fromString(String s) {
        TimeStampMessage ret = null;

        try {
            JSONObject j = new JSONObject(s);

            String sender = j.getString(SENDER_ID);
            int ts = Integer.parseInt(j.getString(TIMESTAMP));
            String msg = j.getString(MESSAGE);

            ret = new TimeStampMessage(sender, ts, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return ret;
    }

    public String toString() {
        JSONObject j = new JSONObject();
        try {
            j.put(SENDER_ID, senderID);
            j.put(TIMESTAMP, String.valueOf(timestamp));
            j.put(MESSAGE, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return j.toString();
    }
}