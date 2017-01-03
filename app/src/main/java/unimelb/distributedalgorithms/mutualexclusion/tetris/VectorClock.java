package unimelb.distributedalgorithms.mutualexclusion.tetris;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.InputMismatchException;

/**
 * Models a vector clock as described in Lecture#2.
 * Created by ANDRES on 16/05/2016.
 */
public class VectorClock {
    private static final String ARRAY_KEY = "Array";
    private static final String ID_KEY = "ID";

    public static final int EQUAL = 0;
    public static final int GREATER_THAN = 1;
    public static final int LESS_THAN = 2;
    public static final int NOT_EQUAL = 3;
    public static final int NUM_TYPES = 4;

    private int v[];
    private int myId;
    private int N;

    public VectorClock(int numProc, int id) {
        myId = id;
        N = numProc;
        v = new int[numProc];
        for (int i = 0; i < N; i++) { v[i] = 0; }
        // v[myId] = 1;
    }

    public synchronized void sendAction() {
        v[myId]++;
    }

    /**
     * Update each value in the clock to the max value of each dimension.
     *
     * @param timeStamp  received {@link VectorClock}.
     */
    public synchronized void receiveAction(VectorClock timeStamp) {
        for (int i = 0; i < N; i++) {
            v[i] = java.lang.Math.max(this.getValue(i), timeStamp.getValue(i));
        }
    }

    public synchronized int getValue(int i) {
        return v[i];
    }

    public synchronized String toString() {
        JSONObject j;

        j = new JSONObject();
        try {
            j.put(ARRAY_KEY, Arrays.toString(v));
            j.put(ID_KEY, myId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return j.toString();
    }

    public String getArrayString() {
        return Arrays.toString(v);
    }

    public static VectorClock fromString(String input) {
        JSONObject j;

        VectorClock ret = null;

        try {
            j = new JSONObject(input);
            String senderArray = j.getString(ARRAY_KEY);
            int senderId = j.getInt(ID_KEY);

            String stripBrackets = senderArray.substring(1, senderArray.length() - 1);
            String[] values = stripBrackets.split(", ");

            ret = new VectorClock(values.length, senderId);
            for (int i = 0; i < ret.size(); i++) {
                ret.setValue(i, Integer.valueOf(values[i]));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public int size() {
        return v.length;
    }

    public synchronized void setValue(int index, int value) {
        v[index] = value;
    }

    public synchronized int compareTo(VectorClock other) {
        if (this.size() != other.size()) {
            throw new InputMismatchException("Vector clock dimensions do not agree");
        }

        Boolean[] result = new Boolean[NUM_TYPES];

        for (int i = 0; i < result.length; i++) {
            result[i] = true;
        }

        // Disqualify possible results by comparing each pair of values:
        for (int i = 0; i < this.size(); i++) {
            int v1 = getValue(i);
            int v2 = other.getValue(i);

            if (v1 < v2) {
                result[GREATER_THAN] = false;
                result[EQUAL] = false;
            } else if (v1 > v2) {
                result[LESS_THAN] = false;
                result[EQUAL] = false;
            }
        }

        // Return the first result that is still valid.
        // Note that NOT_EQUAL will always be technically "valid", but will only
        // be returned if the vector clocks are not EQUAL, LESS_THAN or GREATER_THAN
        for (int i = 0; i < result.length; i++) {
            if (result[i]) {
                return i;
            }
        }
        return NOT_EQUAL;
    }

    /**
     * Returns an exact copy of this {@link VectorClock}'s current state.
     *
     * @return an exact copy of this {@link VectorClock}'s current state.
     */
    public synchronized VectorClock copy() {
        VectorClock ret = new VectorClock(size(), myId);

        for (int i = 0; i < size(); i++) {
            ret.setValue(i, getValue(i));
        }

        return ret;
    }

}
