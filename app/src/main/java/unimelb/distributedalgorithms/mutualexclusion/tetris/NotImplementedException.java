package unimelb.distributedalgorithms.mutualexclusion.tetris;

/**
 * Thrown by a method which needs to be implemented.
 *
 * Created by jorgen on 05/05/16.
 */
public class NotImplementedException extends Throwable {
    /**
     * Takes a message describing where the exception occured.
     *
     * @param msg  description of what needs to be implemented.
     */
    NotImplementedException(String msg) {
        super(msg);
    }
}
