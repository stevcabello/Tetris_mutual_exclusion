package unimelb.distributedalgorithms.mutualexclusion.tetris;

import java.util.ArrayList;

/**
 * A non-algorithm for testing.
 *
 * Created by jorgen on 16/05/16.
 */
public class DummyAlgorithm extends Algorithm {
    /**
     * Initialises the algorithm for a given list of {@link ITetrisPeer}s.
     *
     * @param peers
     * @param self
     */
    public DummyAlgorithm(ArrayList<ITetrisPeer> peers, ITetrisPeer self) {
        super(peers, self);
    }

    @Override
    public void receiveMessage(ITetrisPeer sender, String msg) {

    }

    @Override
    public boolean obtainCritSection() {
        return true;
    }

    @Override
    public boolean releaseCritSection() {
        return true;
    }

    @Override
    public ITetrisPeer getCurrentCritSectPeer() {
        return null;
    }
}
