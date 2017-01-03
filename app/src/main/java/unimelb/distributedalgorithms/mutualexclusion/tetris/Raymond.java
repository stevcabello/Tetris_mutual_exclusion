package unimelb.distributedalgorithms.mutualexclusion.tetris;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Token-based mutual exclusion algorithm. The initial topology is a binary tree
 */
public class Raymond extends Algorithm{

    public String TAG = "Token Algorithm";

    /**
     * Self peer as a pointer to itself in TetrisPeer class.
     */
    private ITetrisPeer selfPeer;

    /**
     * Whether the peer is in criticalSection.
     */
    public boolean criticalSection;
    /**
     * Queue to be hold by every single peer to maintain the order of critical section request.
     */
    private Queue<String> queue;

    /**
     * To identify the queue's head
     */
    private String queuehead = "";

    /**
     * Peer's parent
     */
    private ITetrisPeer parent = null;


    /**
     * To handle the case when I have to wait for having a new head in order to send
     * the request message to my parent
     */
    public boolean hasnewhead;
    public boolean waitingForNewHead;


    public final static String TOKEN_REQUESTED = "TOKEN REQUESTED"; //Request message
    public final static String TOKEN_GRANTED = "TOKEN GRANTED"; //Privileged message



    /**
     * Initialises the algorithm for a given list of {@link ITetrisPeer}s.
     *
     * @param peers
     * @param self
     */
    public Raymond(ArrayList<ITetrisPeer> peers, ITetrisPeer self) {
        super(peers, self);

        int selfIndex = selfIndex();
        this.selfPeer = self;
        this.queue = new LinkedList<>(); //initially the queue is empty
        this.parent = getInitParent(selfIndex);

        if (iamtheRoot()) //The root peer has the token in the initial state
            criticalSection = true;
        else
            criticalSection = false;


        //Check the initial state of the tree
        Log.i(TAG, "Initial state--> ID: " + self.getID() + " parent: " + parent.getID() + " size of Q: " +
                String.valueOf(queue.size()) + " has token: " + String.valueOf(criticalSection));
    }


    /**
     * Get the peer's self index in the peers list
     * @return
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
     * get the index of a peer in the peers list based on its ID
     * @param peerID
     * @return
     */
    private int getPeerIndex(String peerID) {

        int peerIndex=-1;

        for (int i = 0; i < peers.size(); i++) {
            if (peerID.equals(peers.get(i).getID())) {
                peerIndex = i;
                break;
            }
        }

        return peerIndex;
    }


    /**
     * Returns a peer's parent based on peer's position on a Binary Tree topology
     * @param childIndex
     * @return peer parent, for root parent is null
     */
    private ITetrisPeer getInitParent(int childIndex) {
        int parentIndex;

        if (childIndex==0)
            parentIndex = childIndex; //root is its own parent
        else {
            if (childIndex % 2 == 0)
                parentIndex = childIndex - (childIndex / 2) - 1;
            else
                parentIndex = childIndex - (childIndex + 1) / 2;
        }

        return peers.get(parentIndex);
    }


    /**
     * Set the parent peer based on parent ID
     * @param parentId
     */
    public void setparent(String parentId){
        this.parent = getPeer(parentId);
    }


    /**
     * Return the peer based on its ID
     * @param peerIndex
     * @return
     */
    public ITetrisPeer getPeer(String peerIndex){
        return peers.get(getPeerIndex(peerIndex));
    }



    @Override
    public synchronized void receiveMessage(ITetrisPeer sender, String msg) {


        if (msg.equals(TOKEN_REQUESTED)) {

            //When a process receives a request for the token from a child it adds the
            // ID of this child at the end of its queue
            queue.add(sender.getID());

            //Each time a nonroot gets a new head at its (nonempty) queue, it sends a
            // request for the token to its parant in this sink tree
            boolean newHead = hasNewHead(queue);

            if (!iamtheRoot() && !queue.isEmpty() && newHead)
                parent.sendMessage(selfPeer.toString(), TOKEN_REQUESTED);


        } else {//msg.equals("token granted") if a nonroot get the token from its parent
            if (!iamtheRoot()){

                String p = selfPeer.getID(); //Let a nonroot p get the token from its parent
                String q = queue.peek(); //Let the ID of a peer q be at the head of p's queue

                if (!p.equals(q)){
                    //send the token to the peer with the ID at queue's head
                    getPeer(queue.peek()).sendMessage(selfPeer.toString(),TOKEN_GRANTED);
                    setparent(queue.peek()); //make to this peer its parent
                } else { //if the ID of the peer is the same that the ID of the peer at the head of its queue
                    setparent(selfPeer.getID()); //then itself is now the root (no parent)
                    criticalSection = true; //and is privileged
                    this.notify();
                }
                queue.poll(); //In both cases remove the ID from the head of the queue


                //In case the peer is waiting for a change in its queue's head
                //in order to send the request message to its parent
                if (waitingForNewHead) {
                    hasnewhead = true;
                    this.notify();
                }

            }
        }

    }

    /**
     * Checks if the queue's head has a new ID and updates the head
     * @param q
     * @return
     */
    public boolean hasNewHead(Queue<String> q){
        if (!q.isEmpty()) {
            if (!q.peek().equals(queuehead)){
                queuehead = q.peek();
                return true;
            }
        }else queuehead = "";


        return false;
    }





    @Override
    public synchronized boolean obtainCritSection() {

        //Default values
        hasnewhead = false;
        waitingForNewHead = false;


        if (!iamtheRoot()){

            //when a nonroot wants to enter its CS, it adds its ID o its own queue
            queue.add(self);


            //Each time a nonroot gets a new head at its (nonempty) queue, it sends a request for the
            //token to its parent in this sink tree
            boolean newHead = hasNewHead(queue);

            if (!queue.isEmpty() && newHead) {

                parent.sendMessage(selfPeer.toString(), TOKEN_REQUESTED);
                try {
                    // wait until token had been received:
                    while (!criticalSection) {
                        Log.i(TAG,"waiting for access to the CS");
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG,"access to CS granted");
                    // token obtained
                }
            }else { //In case the head's queue hasn't change and the peer wants access to the CS

                waitingForNewHead = true;

                try {
                    // wait until have a new head
                    while (!hasnewhead) {
                        this.wait();
                    }
                } catch (InterruptedException e) {

                }


                Log.i(TAG,"peer has new head at its queue");


                //Now proceed to send the request message
                parent.sendMessage(selfPeer.toString(), TOKEN_REQUESTED);

                try {
                    // wait until token had been received:
                    while (!criticalSection) {
                        this.wait();
                    }
                } catch (InterruptedException e) {

                }


            }


        }
        else {//if the root want to enter its critical section and
              // its queue is empty it can become privileged straightaway
            if (queue.isEmpty()){
                criticalSection = true;
            }
        }



        return true;
    }



    @Override
    public boolean releaseCritSection() {

        //release CS
        criticalSection = false;

        if (iamtheRoot()){ //If the root has left its Critical Section
            if (!queue.isEmpty() || queue.size()>1) { //and its queue is or becomes nonempty
                String q = queue.peek(); //peer ID q at the head of its queue
                getPeer(q).sendMessage(selfPeer.toString(),TOKEN_GRANTED);
                setparent(q); //make q its parent
                queue.poll(); //remove q's ID from the head of its queue

                boolean newHead = hasNewHead(queue); //used is this way because it helps me to update te head as well

                if (!queue.isEmpty() && newHead)
                    parent.sendMessage(selfPeer.toString(),TOKEN_REQUESTED);

                return true;
            }
        }

        return false;
    }


    /**
     * If the peer in the token holder return itself otherwise null
     * @return
     */
    @Override
    public ITetrisPeer getCurrentCritSectPeer() {
        if (criticalSection)
            return selfPeer;
        else
            return null;
    }


    /**
     * Check if I am the root peer
     * @return true is I am the root , false otherwise
     */
    public boolean iamtheRoot() {
        if (selfPeer.equals(parent)) //The root is its own parent
            return true;
        return false;
    }




}
