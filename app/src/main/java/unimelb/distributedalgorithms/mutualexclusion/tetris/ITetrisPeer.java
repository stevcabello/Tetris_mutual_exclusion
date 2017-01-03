package unimelb.distributedalgorithms.mutualexclusion.tetris;

/**
 * Represents a {@link ITetrisPeer} in the network.
 *
 * Provides a level of abstraction for algorithms.
 * Will be used primarily to send and receive messages.
 *
 * Created by jorgen on 05/05/16.
 */
public interface ITetrisPeer extends Comparable<ITetrisPeer> {
    /**
     * Returns the ID of this {@link ITetrisPeer}.
     *
     * @return the ID of this {@link ITetrisPeer}.
     */
    String getID();

    /**
     * Send a message to this {@link ITetrisPeer}.
     *
     * @param msg  The message to send.
     *
     * @return <i>true</i> on delivery (successful, if that guarantee is provided),
     *  <i>false</i> otherwise.
     */
    boolean sendMessage(String senderID, String msg);

    @Override
    String toString();

    boolean equals(ITetrisPeer other);
}
