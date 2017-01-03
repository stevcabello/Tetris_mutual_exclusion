package unimelb.distributedalgorithms.mutualexclusion.tetris;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Superclass for algorithms. This abstract super forces an algorithm to implement the
 * {@link IAlgorithm} interface, as well as ensuring all algorithms will have the same
 * constructor(s).
 *
 * Created by jorgen on 05/05/16.
 */
public abstract class Algorithm implements IAlgorithm{

    /**
     * The ID of the {@link ITetrisPeer} associated with this {@link Algorithm}.
     * Used to identify the "self node" in the algorithm logic.
     * */
    protected String self;

    /**
     * Holds all {@link ITetrisPeer}s this algorithm needs to function. In an algorithm with
     * a "limited view" of the network, this should be just the neighbouring Peers.
     */
    protected ArrayList<ITetrisPeer> peers;

    /**
     * Initialises the algorithm for a given list of {@link ITetrisPeer}s.
     * @param peers
     */
    Algorithm(ArrayList<ITetrisPeer> peers, ITetrisPeer self) {
        this.peers = peers;
        this.self = self.getID();

        Collections.sort(peers);
    }

}
