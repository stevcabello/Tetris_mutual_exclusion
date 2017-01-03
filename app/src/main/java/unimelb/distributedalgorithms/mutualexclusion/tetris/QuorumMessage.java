package unimelb.distributedalgorithms.mutualexclusion.tetris;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to hold messages used in the Agrawal-El Abbadi algorithm.
 *
 * A {@link QuorumMessage} has a timestamp and a {@link AlgorithmMessage} value.
 * The timestamp is used to avoid starvation and deadlocks as specified in the paper
 * on page 14. (see e.g. http://dl.acm.org/citation.cfm?id=103728).
 *
 * Created by jorgen on 15/05/16.
 */
public final class QuorumMessage implements Comparable<QuorumMessage> {
    private static final String SENDER_ID = "SenderID";
    private static final String TIMESTAMP = "TimeStamp";
    private static final String MESSAGE = "Message";

    private final String senderID;
    private final VectorClock timestamp;
    private final String message;

    QuorumMessage(String senderID, VectorClock timestamp, String message) {
        this.senderID = senderID;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getSenderID() {
        return senderID;
    }

    public VectorClock getTimestamp () {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public static QuorumMessage fromString(String s) {
        QuorumMessage ret = null;

        try {
            JSONObject j = new JSONObject(s);

            String sender = j.getString(SENDER_ID);
            VectorClock ts = VectorClock.fromString(j.getString(TIMESTAMP));
            String msg = j.getString(MESSAGE);

            ret = new QuorumMessage(sender, ts, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return ret;
    }

    public String toString() {
        JSONObject j = new JSONObject();
        try {
            j.put(SENDER_ID, senderID);
            j.put(TIMESTAMP, timestamp.toString());
            j.put(MESSAGE, message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return j.toString();
    }

    /** Returns other.getTimestamp() - this.getTimestamp() since
     * "Greater" in this context means earlier.
     *
     * @param other {@link QuorumMessage} to compare to
     *
     * @return the difference between {@param other}'s timestamp and the local timestamp.
     */
    @Override
    public int compareTo(QuorumMessage other) {
        int cmp = timestamp.compareTo(other.getTimestamp());

        switch (cmp) {
            case VectorClock.EQUAL:
                return 0;

            case VectorClock.LESS_THAN:
                return -1;

            case VectorClock.GREATER_THAN:
                return 1;

            default:
                // Messages are concurrent: Prefer the first one.
                return senderID.compareTo(other.getSenderID());
        }
    }
}
