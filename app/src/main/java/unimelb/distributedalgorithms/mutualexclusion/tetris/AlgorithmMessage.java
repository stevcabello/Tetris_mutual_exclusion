package unimelb.distributedalgorithms.mutualexclusion.tetris;


/**
 * Messages used by processes in Critical Section algorithms to negotiate access.
 */
public final class AlgorithmMessage {

    /** Request to join a quorum. */
    public static final String REQUEST = "REQUEST";

    /** Accept request to join a quorum. */
    public static final String REPLY = "REPLY";

    /** Release a {@link ITetrisPeer} from a quorum. */
    public static final String RELINQUISH = "RELINQUISH";

    /** Request quorum from a node that is currently blocking access. */
    public static final String INQUIRE = "INQUIRE";

    /** Instruct requestor to wait for permission. */
    public static final String YIELD = "YIELD";

    /** Allow the process to notify the algorithm of node failure. */
    public static final String NODE_FAILURE = "FAILURE";
}
