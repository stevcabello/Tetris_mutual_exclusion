package unimelb.distributedalgorithms.mutualexclusion.tetris;

/**
 * Interface for {@link Algorithm}s.
 *
 * Includes functionality to receive messages, obtain and release the critical section,
 * as well as returning the {@link ITetrisPeer} currently holding the critical section.
 *
 * Created by jorgen on 05/05/16.
 */
public interface IAlgorithm {

    /**
     * Receive a message from the given {@link ITetrisPeer}.
     *
     * @param sender  {@link ITetrisPeer} that sent the message to this user.
     * @param msg  the message sent.
     */
    void receiveMessage(ITetrisPeer sender, String msg);

    /**
     * Requests the algorithm to obtain the critical section.
     *
     * @return <i>true</i> if successful, <i>false</i> otherwise (if allowed to fail..
     *  otherwise use this as an indicator whether there is an error in the algorithm).
     */
    boolean obtainCritSection();

    /**
     * Request the algorithm to release the critical section.
     *
     * @return <i>true</i> if successful, <i>false</i> otherwise (if allowed to fail..
     *  otherwise use this as an indicator whether there is an error in the algorithm).
     */
    boolean releaseCritSection();

    /**
     * Returns the {@link ITetrisPeer} currently holding the critical section according to the algorithm.
     * If the algorithm thinks no-one currently holds the critical section, returns <i>null</i>.
     *
     * @return The {@link ITetrisPeer} currently holding the critical section accorting to the algorithm,
     *  or <i>null</i> if the algorithm thinks no-one currently holds the critical section.
     *
     */
    ITetrisPeer getCurrentCritSectPeer();

}
