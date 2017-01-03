package unimelb.distributedalgorithms.mutualexclusion.tetris;

import android.util.Log;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.Queue;

import unimelb.distributedalgorithms.mutualexclusion.tetris.utils.Globals;

/**
 * Implementation of the Ricart-Agrawala algorithm for distributed mutual exclusion.
 *
 * The algorithm is based on Lamport's timestamps and requires a multi-cast process which
 * then waits for the replies from all the peers before granting access to the critical section.
 *
 * Created by Andres on 18/05/2016.
 */
public class RicartAgrawala extends Algorithm {
    public String TAG = "Ricart Agrawala";

    private VectorClock requestClock;

    private TimeStampMessage currentCritSection;

    private VectorClock selfRequestClock = null;

    /**
     * The current state related to the critical section.
     */
    private String state;

    /**
     * All the possible states that the algorithm neeeds.
     */
    public final static String RELEASED = "RELEASED"; //CS released
    public final static String WANTED = "WANTED"; //CS wanted
    public final static String HELD = "HELD"; //CS held

    /**
     * Contains the current time stamp for the peer.
     */
    private int TimeStamp;

    /**
     * The number of replies received after a multi-cast reply has been sent to all peers.
     */
    private int replyNum;

    private boolean[] replyList;

    /**
     * Boolean value that indicates whether all the expected replies have been received.
     */
    private boolean allReplies = false;

    /**
     * Queue that contains all the pending request to be processed after the CS has been released.
     */
    private ArrayList<String> q;

    /**
     * Initialises the algorithm for a given list of {@link ITetrisPeer}s.
     *
     * @param peers
     * @param self
     *
     * Additionally, the state is initialized as RELEASED, as well as the number of replies, the queue
     * and the current time stamp.
     */
    public RicartAgrawala(ArrayList<ITetrisPeer> peers, ITetrisPeer self) {
        super(peers, self);
        int selfIndex = selfIndex();
        requestClock = new VectorClock(peers.size(), selfIndex);
        state = RELEASED;
        replyNum = 0;
        replyList = new boolean[peers.size()];
        q = new ArrayList<>();
        TimeStamp = 0;
        Log.i(TAG, "Initial state--> ID: " + self.getID() + " size of reply list: " + peers.size());
    }

    /**
     * Returns an index based on the peer ID.
     */
    private int getPeerIndex(String peerID) {
        int peerIndex = 1;

        for(int i = 0; i < peers.size(); i++) {
            if (peerID.equals(peers.get(i).getID())) {
                peerIndex = i;
                break;
            }
        }

        return peerIndex;
    }

    /**
     * Returns the index of the current peer.
     */
    private int selfIndex() {
        int selfIndex = -1;

        for (int i = 0; i < peers.size(); i++) {
            if (this.self.equals(peers.get(i).getID())) {
                selfIndex = i;
                break;
            }
        }

        if (selfIndex == -1) {
            throw new NullPointerException("Couldn't find 'self'-peer in the list of peers.");
        }

        return selfIndex;
    }

    /**
     * Helper to send a message, including all the data managed by the TImeStampMessage class.
     */
    private synchronized boolean sendMessage(ITetrisPeer p, String msg) {
        TimeStampMessage toSend;
        toSend = new TimeStampMessage(self, TimeStamp, msg);
        return p.sendMessage(self, toSend.toString());
    }

    /**
     * Receives a message and adds all the necessary details to be handled correctly.
     */
    @Override
    public void receiveMessage(ITetrisPeer sender, String msg) {
        Log.i(TAG, "message received :" + msg + " from " + sender.getID());

        TimeStampMessage qMsg = TimeStampMessage.fromString(msg);

        String senderID = sender.getID();
        String msgID = qMsg.getSenderID();

        if (!senderID.equals(msgID)) {
            throw new InputMismatchException("Mismatch in sender/msg ID;"
                    + "Sender: " + senderID + "; Message: " + msgID);
        }

        handleMessage(sender, qMsg);
    }

    /**
     * Handles the message generated by the previous method, depending on the received text
     */
    private synchronized void handleMessage(ITetrisPeer sender, TimeStampMessage qMsg) {
        String type = qMsg.getMessage();

        Log.i(TAG, "message to handle :" + type);

        switch (type) {
            case AlgorithmMessage.REQUEST:
                TimeStamp = qMsg.getTimestamp() + 1;
                handleRequest(sender, qMsg);
                break;

            case AlgorithmMessage.REPLY:
                handleReply(sender);
                break;
        }
    }

    /**
     * Handles all the requests
     */
    private synchronized void handleRequest(ITetrisPeer sender, TimeStampMessage qMsg) {
        Log.i(TAG, "STATE: " + state);
        if (state.equals(HELD) || (state.equals(WANTED) && comparePeers(TimeStamp, qMsg.getTimestamp(), sender.toString()))) {
            q.add(sender.toString());
        } else {
            sendMessage(sender, AlgorithmMessage.REPLY);
            Log.i(TAG, "sending reply to " + sender.getID());
        }
    }

    /**
     * Helper to compare between two timestamps, and, if necessary, compare between peer ids.
     * Used to resolve any conflicts when requests have the same time stamp
     */
    private boolean comparePeers(int r, int s, String sender) {
        Log.i(TAG, "timestamp receiver: " + String.valueOf(r));
        Log.i(TAG, "timestamp sender: " + String.valueOf(s));
        Log.i(TAG, "sender : " + sender);
        if (r < s) {
            Log.i(TAG,"send to queue");
            return true;
        } else if (r > s) {
            Log.i(TAG,"reply immediately");
            return false;
        } else {
            Log.i(TAG, "index receiver: " + String.valueOf(selfIndex()));
            Log.i(TAG, "index sender: " + String.valueOf(getPeerIndex(sender)));
            if (selfIndex() < getPeerIndex(sender)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Handles all the replies
     */
    private synchronized void handleReply(ITetrisPeer sender) {
        replyNum++;
        if (replyNum == peers.size() - 1) {
            Log.i(TAG,"reply = N");
            allReplies = true;
            this.notify();
        }
    }

    /**
     * Handles the Critical Section granting part
     */
    @Override
    public synchronized boolean obtainCritSection() {
        Log.i(TAG, "request CS access");
        state = WANTED;
        selfRequestClock = requestClock.copy();
        replyList = new boolean[peers.size()];
        Log.i(TAG, "start multicast");
        sendMulticast();

        try {
            while (!allReplies) {
                Log.i(TAG, "Number of replies:" + replyNum);
                this.wait();
                state =  HELD;
            }
        } catch (InterruptedException e) {

        }

        return true;
    }

    /**
     * Sends a multi-cast message to all the peers
     */
    public void sendMulticast() {
        for (ITetrisPeer peer : peers) {
            if(!peer.toString().equals(self.toString())) {
                sendMessage(peer, AlgorithmMessage.REQUEST);
                Log.i(TAG, "sending message to :" + peer.getID());
            }
        }
    }

    /**
     * Gets a peer depending on the index
     */
    public ITetrisPeer getPeer(String peerIndex) {
        return peers.get(getPeerIndex(peerIndex));
    }

    /**
     * Handles the releasing of the Critical Section
     */
    @Override
    public boolean releaseCritSection() {
        state = RELEASED;
        allReplies = false;
        replyNum = 0;
        for(int i = 0; i < q.size(); i++) {
            sendMessage(getPeer(q.get(i)), AlgorithmMessage.REPLY);
        }
        q.clear();
        return true;
    }

    @Override
    public ITetrisPeer getCurrentCritSectPeer() {
        return null;
    }
}
