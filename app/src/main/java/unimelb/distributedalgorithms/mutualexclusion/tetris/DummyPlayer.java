package unimelb.distributedalgorithms.mutualexclusion.tetris;

/**
 * Models a {@link DummyPlayer} in distributed Tetris. This class's main functionality is
 * to provide an implementation of the {@link ITetrisPeer} interface in order to provide
 * {@link Algorithm}s with a resource to send and identify messages with.
 */
public final class DummyPlayer implements ITetrisPeer {

    /** The ID of this {@link DummyPlayer}. */
    private String id;

    /**
     * Initialise a new {@link DummyPlayer} with the given ID.
     *
     * @param id  ID of the DummyPlayer to initialise.
     */
    DummyPlayer(String id) {
        this.id = id;
    }

    @Override
    public String getID() {
        return this.id;
    }



    // TODO: Implement message passing..
    @Override
    public boolean sendMessage(String senderID, String msg) {
        System.out.printf("%s Sending message '%s' to %s", senderID, msg, id);
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

    public boolean equals(ITetrisPeer other) {
        return id.equals(other.getID());
    }
}
