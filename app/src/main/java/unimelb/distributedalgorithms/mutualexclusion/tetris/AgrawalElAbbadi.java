package unimelb.distributedalgorithms.mutualexclusion.tetris;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.TreeSet;

/**
 * Implementation of the Agrawal El-Abbadi algorithm for Mutual Exclusion in a distributed network.
 *
 * The tree structure used is a binary tree, which implicitly exisits in the list used
 * during construction. The root is at index 0, and children of the node at index i exist at
 * indices 2*i + 1 and 2*i + 2.
 *
 * Created by jorgen on 05/05/16.
 */
public class AgrawalElAbbadi extends Algorithm {
    public String TAG = "Agrawal El-Abbadi";
    /**
     * Local clock for request messages sequence. This is to be
     * incremented every time a message is sent or received.
     *
     * On receipt, the logical clock takes the maximum
     * value of its current state and the received timestamp.
     */
    private VectorClock requestClock;

    /** Queue for pending REQUESTs, ordered by their local timestamps.
     * Note that the {@link TreeSet} implements {@link java.util.SortedSet}
     * and seems better suited than e.g. a {@link java.util.PriorityQueue},
     * since the latter uses a heap, which requires removing elements in order
     * to get them in sorted order.
     */
    private TreeSet<QuorumMessage> requestQueue;


    /**
     * The REQUEST message of the node this instance has joined to quorum of.
     */
    private QuorumMessage reqQueueHead;

    /**
     * A helper variable to keep a copy of this instance's vector clock
     * when the first request was made. This allows us to compare the timestamp
     * with later incoming requests for deadlock handling.
     */
    private VectorClock selfRequestClock = null;

    /**
     * Dimension to keep track of requests sent.
     *
     * Used for RELINQUISHing nodes after exiting the Critical Section.
     */
    private static final int REQUEST_SENT = 0;

    /**
     * Dimension used to keep track of the current quorum
     * held to guarantee Critical Section access.
     */
    private static final int REPLY_RECEIVED = 1;

    /**
     * Dimension to track failed nodes and allow the algorithm to adapt.
     */
    private static final int FAILED_PEER = 2;

    private static final int NUM_PROPERTIES = 3;

    /**
     * Boolean array for tracked boolean properties of the peers. Each peer's
     * properties can be found at properties[PROPERTY_INDEX][PEER_INDEX],
     * where the PEER_INDEX is as in the sorted list of peers.
     */
    private boolean[][] properties;

    /** Map from an {@link ITetrisPeer}'s ID to its index. */
    private Map<String, Integer> peerIndexMap;

    /** The index of this node. */
    private int selfIndex;


    /**
     * Initialises the algorithm for a given list of {@link ITetrisPeer}s
     * by sorting the peers by ID.
     *
     * This way the representation of the network as a binary tree
     * will be consistent throughout the network.
     *
     * @param peers  the list of {@link ITetrisPeer}s in the network (other nodes).
     * @param self  the node this instance of {@link AgrawalElAbbadi} is working for.
     */
    public AgrawalElAbbadi(ArrayList<ITetrisPeer> peers, ITetrisPeer self) {
        super(peers, self);


        // Initialise the map from peers to their indices:
        int numPeers = peers.size();
        peerIndexMap = new HashMap<>(numPeers);

        selfIndex = -1;
        for (int i = 0; i < numPeers; i++) {
            ITetrisPeer pi = peers.get(i);
            peerIndexMap.put(pi.getID(), i);

            Log.i(TAG, "Put " + pi.getID() + " to index " + Integer.toString(i));

            if (self.equals(pi)) {
                selfIndex = i;
                Log.i(TAG, "Set self-Index to " + Integer.toString(i));
            }

        }

        if (selfIndex == -1) {
            throw new NullPointerException("Couldn't find 'self'-peer in the list of peers.");
        }

        requestClock = new VectorClock(numPeers, selfIndex);
        requestQueue = new TreeSet<>();

        properties = new boolean[NUM_PROPERTIES][numPeers];
    }


    /**
     * Helper to handle message passing/formatting.
     *
     * @param destination  {@link ITetrisPeer} to send the message to.
     * @param msg  the message to send.
     */
    private synchronized boolean sendMessage(ITetrisPeer destination, String msg) {
        QuorumMessage toSend;

        toSend = new QuorumMessage(self, requestClock, msg);

        boolean msgSent = destination.sendMessage(self, toSend.toString());

        int recepientIndex = peerIndexMap.get(destination.getID());

        if (!msgSent) {
            properties[FAILED_PEER][recepientIndex] = true;
            return false;
        }

        switch (msg) {
            case AlgorithmMessage.REQUEST:
                properties[REQUEST_SENT][recepientIndex] = true;
                break;

            case AlgorithmMessage.RELINQUISH:
                properties[REPLY_RECEIVED][recepientIndex] = false;
                properties[REQUEST_SENT][recepientIndex] = false;
                break;

            case AlgorithmMessage.REPLY:
                properties[REPLY_RECEIVED][recepientIndex] = true;
                break;
        }

        return true;
    }


    @Override
    public synchronized void receiveMessage(ITetrisPeer sender, String msg) {
        QuorumMessage qMsg = QuorumMessage.fromString(msg);

        String senderID = sender.getID();
        String msgID = qMsg.getSenderID();

        if (!senderID.equals(msgID)) {
            throw new InputMismatchException("Mismatch in sender/msg ID;"
                    + "Sender: " + senderID + "; Message: " + msgID);
        }

        handleMessage(sender, qMsg);
    }

    private synchronized void handleMessage(ITetrisPeer sender, QuorumMessage qMsg) {
        String type = qMsg.getMessage();

        // Update the local vector clock:
        requestClock.receiveAction(qMsg.getTimestamp());

        int senderIndex = peerIndexMap.get(sender.getID());

        switch (type) {
            case AlgorithmMessage.REQUEST:
                handleRequest(sender, qMsg);
                break;

            case AlgorithmMessage.REPLY:
                handleReply(sender, senderIndex);
                break;

            case AlgorithmMessage.RELINQUISH:
                handleRelinquish(sender, qMsg);
                break;

            case AlgorithmMessage.INQUIRE:
                handleInquire(sender, senderIndex);
                break;

            case AlgorithmMessage.YIELD:
                handleYield(sender);
                break;

            case AlgorithmMessage.NODE_FAILURE:
                handleFailure(senderIndex);

        }
    }

    /**
     * This method handles the failure by marking the node as FAILED, which will mean
     * obtaining a quorum through this node will mean obtaining a tree quorum of each
     * of its children.
     *
     * A NODE_FAILURE message can be sent to the algorithm from the owner process.
     *
     * @param senderIndex  the index of the failed node.
     */
    private void handleFailure(int senderIndex) {
        properties[FAILED_PEER][senderIndex] = true;
        boolean reobtainQuorum = properties[REQUEST_SENT][senderIndex];
        properties[REQUEST_SENT][senderIndex] = false;
        properties[REPLY_RECEIVED][senderIndex] = false;

        // Ensure
        if (reobtainQuorum) {
            requestQuorum(senderIndex);
        }
    }

    /**
     * Handles a REQUEST (R) for access to a node's critical section.
     *
     * If this node has not already given another node (X) a REPLY, it will REPLY to R.
     * Otherwise, R is added to the queue. If R has a strictly older timestamp (this
     * implementation uses vector clocks), X will be INQUIRED to YIELD it's position
     * as queue head for this process.
     *
     * @param sender  the {@link ITetrisPeer} that sent the REQUEST.
     * @param qMsg  the {@link QuorumMessage} that was sent.
     */
    private synchronized void handleRequest(ITetrisPeer sender, QuorumMessage qMsg) {
        String senderID = sender.getID();
        // Add the request to the queue if the requesting node is not already in the queue.
        boolean duplicate = false;
        for (QuorumMessage qm : requestQueue) {
            if (senderID.equals(qm.getSenderID())) {
                Log.e(TAG,"Duplicate REQUEST from: " + senderID);
                duplicate = true;
            }
        }

        if (!duplicate) {
            // If its timestamp is lower, it will be the next
            // to receive permission since we are using a priority queue.
            requestQueue.add(qMsg);
        }

        if (reqQueueHead == null) {
            replyToQueueHead();
        } else if (qMsg.compareTo(reqQueueHead) < 0) {
            // If the new request is older than the current permission holder,
            // INQUIRE the current holder.
            TetrisPeer currHolder = new TetrisPeer(reqQueueHead.getSenderID());
            sendMessage(currHolder, AlgorithmMessage.INQUIRE);
        }

    }

    /**
     * Helper to reply to the next pending request in the queue.
     *
     * Gives the next request in the queue a REPLY. The queue head has to be separate from
     * the queue itself since we need to know who we have replied to (i.e. who to INQUIRE
     * if applicable.)
     */
    private synchronized void replyToQueueHead() {
        Log.i(TAG, "replyToQueueHead");
        for (QuorumMessage qm : requestQueue) {
            Log.i(TAG, "  Queue items: ID: " + qm.getSenderID() + ", clock: " + qm.getTimestamp().toString());
        }

        Log.i(TAG, "Queue length: " + requestQueue.size());
        reqQueueHead = requestQueue.pollFirst();
        Log.i(TAG, "Queue length after pollFirst(): " + requestQueue.size());

        if (reqQueueHead != null) {
            ITetrisPeer toTell = new TetrisPeer(reqQueueHead.getSenderID());
            sendMessage(toTell, AlgorithmMessage.REPLY);
        }
    }

    /**
     * When a REPLY is received, add the sender to our quorum (by modifying
     * its REPLY_RECEIVED field).
     *
     * @param sender  the {@link ITetrisPeer} that sent the REPLY.
     * @param senderIndex  {@param sender}'s index in the list of peers.
     */
    private synchronized void handleReply(ITetrisPeer sender, int senderIndex) {
        if (properties[REQUEST_SENT][senderIndex]) {

            properties[REPLY_RECEIVED][senderIndex] = true;

            if (quorumObtained(0)) {
                // Create a dummy message as virtual head of the queue:
                reqQueueHead = new QuorumMessage(self, selfRequestClock, AlgorithmMessage.REQUEST);
                this.notify();
            }

        } else {
            Log.e(TAG, "Warning: Received unrequested REPLY.");
            sendMessage(sender, AlgorithmMessage.RELINQUISH);
        }
    }

    /**
     * Recursively check whether a tree quorum has been obtained.
     *
     * @param peerIndex  Index of node to check permission status for.
     *
     * @return <i>true</i> if permission was granted by some
     *  sub-quorum of the given node, <i>false otherwise</i>.
     */
    private synchronized boolean quorumObtained(Integer peerIndex) {
        int numPeers = peers.size();

        if (peerIndex >= numPeers) {
            // Empty node
            return true;
        }

        // List is zero-indexed:
        Integer leftChild = 2 * peerIndex + 1;
        Integer rightChild = leftChild + 1;

        boolean recPermGranted = properties[REPLY_RECEIVED][peerIndex];

        // If node i has not failed and has granted permission,
        // recursive permission from either child means a quorum has been obtained.
        // If the node only has one child (incomplete tree), it must provide permission.
        if (!properties[FAILED_PEER][peerIndex] && recPermGranted && rightChild < numPeers) {
            recPermGranted = (quorumObtained(leftChild) || quorumObtained(rightChild));
        } else {
            recPermGranted =
                    recPermGranted && quorumObtained(leftChild) && quorumObtained(rightChild);
        }

        return recPermGranted;
    }


    /**
     * Handes a RELINQUISH message by discarding the current request queue head
     * and REPLYing to the next request in the queue (if it is not empty).
     *
     * @param sender  the {@link ITetrisPeer} that sent the RELINQUISH.
     * @param qMsg  the message sent.
     */
    private synchronized void handleRelinquish(ITetrisPeer sender, QuorumMessage qMsg) {
        if (reqQueueHead != null && qMsg.getSenderID().equals(reqQueueHead.getSenderID())) {

            reqQueueHead = null;
            replyToQueueHead();

        } else {
            // Handling rogue relinquish messages, or in case of
            // releasing a request before it has been replied to (if allowed):
            QuorumMessage toRemove = null;

            for (QuorumMessage nextMsg : requestQueue) {
                if (sender.getID().equals(nextMsg.getSenderID())) {
                    toRemove = nextMsg;
                    break;
                }
            }

            if (toRemove != null) {
                requestQueue.remove(toRemove);
            } else {
                System.err.printf("Erroneous RELINQUISH from '%s'", sender.getID());
            }
        }
    }


    /**
     * Handles an INQUIRE message:
     *
     * If this node has not yet obtained the Critical Section, it should YIELD to
     * allow an older request to obtain it (thus avoiding deadlocks and starvation).
     *
     * Otherwise ignore the INQUIRE and send a RELINQUISH, as usual, after
     * exiting the Critical Section.
     *
     * @param sender  the {@link ITetrisPeer} that sent the INQUIRE.
     * @param senderIndex  {@param sender}'s index in the list of peers.
     */
    private synchronized void handleInquire(ITetrisPeer sender, int senderIndex) {
        if (!quorumObtained(0) && properties[REQUEST_SENT][senderIndex]) {
            properties[REPLY_RECEIVED][senderIndex] = false;
            sendMessage(sender, AlgorithmMessage.YIELD);

        } else {
            Log.e(TAG, "Erroneous INQUIRE from '" + sender.getID() + "'");
            sendMessage(sender, AlgorithmMessage.RELINQUISH);
        }
    }

    /**
     * Handles a YIELD message by adding the queue head back to the queue
     * and responding to the head of the queue. Note that we do not keep track
     * of who we initially INQUIREd, since other earlier requests may have come
     * in the meantime (and generated their own INQUIRE messages).
     *
     * Also note that in the case the YIELD is erroneous (Byzantine), the
     * queue head should simply appear at the head of the queue, and therefore
     * immediately receive another REPLY.
     *
     * @param sender  the {@link ITetrisPeer} that sent the YIELD.
     */
    private synchronized void handleYield(ITetrisPeer sender) {
        String senderID = sender.getID();

        if (senderID.equals(reqQueueHead.getSenderID())) {
            replyToQueueHead();
        } else {
            Log.e(TAG, "Erroneous YIELD from '" + senderID + "'");
        }
    }

    /**
     * Helper to check whether the Critical Section has been requested.
     *
     * @return <i>true</i> if the Critical Section has been requested, <i>false</i> otherwise.
     */
    private synchronized boolean csRequested() {
        for (Boolean b : properties[REQUEST_SENT]) {
            if (b) { return true; }
        }

        return false;
    }


    @Override
    public synchronized boolean obtainCritSection() {
        if (!csRequested()) {

            // Send request to self first, to ensure the timestamp on a self-request is the lowest
            // possible. This means one extra message exchange per critical section, but it makes
            // it easier to reason about correctness of deadlock resolve.
            ITetrisPeer self = peers.get(selfIndex);
            if (self == null) {
                throw new NullPointerException("self-peer is null!");
            }

            // Make a copy of the requestClock in case we have to send a message to ourselves.
            // We want to send the request to ourselves first in order to avoid deadlocks.

            requestClock.sendAction();
            selfRequestClock = requestClock.copy();

            if (!requestQuorum(0)) {
                // Failed to request access from a quorum of peers;
                return false;
            }
        }

        try {
            // Ensure quorum has actually been obtained before leaving the method:
            while (reqQueueHead == null || !self.equals(reqQueueHead.getSenderID())) {
                Log.i(TAG, "Sleeping while awaiting Critical Section access");
                this.wait();
            }
        } catch (InterruptedException e) {
            // Quorum has been obtained.
        }

        return true;
    }

    /**
     * Recursively REQUEST access to the critical section from a Quorum of {@link ITetrisPeer}s.
     *
     * @param i  next ITetrisPeer to REQUEST access from.
     * @return <i>true</i> if requests were successfully sent to a quorum of Peers,
     *         <i>false</i> otherwise.
     */
    private synchronized boolean requestQuorum(int i) {
        if (i >= peers.size()) {
            return true;
        }

        ITetrisPeer toAsk = peers.get(i);

        int leftChild = 2 * i + 1;
        int rightChild = leftChild + 1;

        boolean messageSent = !properties[FAILED_PEER][i] &&
                (properties[REQUEST_SENT][i] || sendMessage(toAsk, AlgorithmMessage.REQUEST));


        properties[REQUEST_SENT][i] = messageSent;

        boolean quorumRequested = requestQuorum(leftChild);

        if (!messageSent) {
            properties[FAILED_PEER][i] = true;
            quorumRequested = quorumRequested && requestQuorum(rightChild);
        } else {
            quorumRequested = quorumRequested || requestQuorum(rightChild);
        }

        return quorumRequested;
    }



    @Override
    public synchronized ITetrisPeer getCurrentCritSectPeer() {
        if (reqQueueHead == null) {
            return null;
        } else {
            return new TetrisPeer(reqQueueHead.getSenderID());
        }
    }


    /**
     * Releases each node which granted this instance access to its Critical Section.
     *
     * @return <i>true</i> if all nodes were successfully released, <i>false</i> otherwise.
     */
    @Override
    public synchronized boolean releaseCritSection() {
        int numPeers = peers.size();

        if (!csRequested()) {
            Log.e(TAG, "Tried to release CS without requesting it");
        } else if (!self.equals(reqQueueHead.getSenderID())) {
            Log.e(TAG, "releasing CS when !self.equals(reqQueueHead.getSenderID())");
        }

        for (int peerIndex = 0; peerIndex < numPeers; peerIndex++) {
            if (properties[REPLY_RECEIVED][peerIndex]) {
                sendMessage(peers.get(peerIndex), AlgorithmMessage.RELINQUISH);
            }
        }


        properties[REPLY_RECEIVED] = new boolean[numPeers];
        properties[REQUEST_SENT] = new boolean[numPeers];
        selfRequestClock = null;

        replyToQueueHead();

        return true;
    }

}
