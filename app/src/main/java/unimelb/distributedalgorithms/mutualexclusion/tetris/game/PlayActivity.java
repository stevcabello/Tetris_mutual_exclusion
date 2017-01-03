package unimelb.distributedalgorithms.mutualexclusion.tetris.game;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import unimelb.distributedalgorithms.mutualexclusion.tetris.AgrawalElAbbadi;
import unimelb.distributedalgorithms.mutualexclusion.tetris.Algorithm;
import unimelb.distributedalgorithms.mutualexclusion.tetris.ITetrisPeer;
import unimelb.distributedalgorithms.mutualexclusion.tetris.MainActivity;
import unimelb.distributedalgorithms.mutualexclusion.tetris.QuorumMessage;
import unimelb.distributedalgorithms.mutualexclusion.tetris.R;
import unimelb.distributedalgorithms.mutualexclusion.tetris.Raymond;
import unimelb.distributedalgorithms.mutualexclusion.tetris.RicartAgrawala;
import unimelb.distributedalgorithms.mutualexclusion.tetris.TetrisPeer;
import unimelb.distributedalgorithms.mutualexclusion.tetris.TimeStampMessage;
import unimelb.distributedalgorithms.mutualexclusion.tetris.utils.Globals;
import unimelb.distributedalgorithms.mutualexclusion.tetris.utils.Utils;

public class PlayActivity extends Activity implements Runnable, Handler.Callback
{
    private String TAG = "PlayActivity";

    public static final String MESSAGE_CONTENT = "CONTENT";
    public static final String MESSAGE_TYPE = "TYPE";
    public static final String MESSAGE_SENDER_ID = "MESSAGE";

    private static final String MOVE_RIGHT = "MOVE RIGHT";
    private static final String MOVE_LEFT = "MOVE LEFT";
    private static final String MOVE_DOWN = "MOVE DOWN";
    private static final String ROTATE = "ROTATE";


    private static final String YELLOW_LIGHT = "YELLOW";
    private static final String RED_LIGHT = "RED";
    private static final String GREEN_LIGHT = "GREEN";


    private final static int GAME_MESSAGE = 0;
    public final static int ALGORITHM_MESSAGE = 1;

    private Tetromino mCurBlock;
    private Tetromino mNextBlock;
    private Playfield mMyPlayfield;
    
    private PlayfieldView mMyFieldView;
    private PlayfieldView mNextBlockView;

    private Handler mInputHandler;
    private GestureDetector mGestureHandler;

    private int mGameState;
    private ArrayList<Integer> mActionQueue;

    private final static int BLOCK_DELAY = 1000;

    private long initTime=0;

    public static Handler handler = new Handler();

    int randIdx = 0;

    private String algorithm;
    private TextView tvAlgorithm;
    private ListView lvAlgorithmLog;

    ArrayList<String> logData=new ArrayList<String>();
    ArrayAdapter<String> logAdapter;

    public ImageView ivCSindicator;
    public TextView tvPlayerName;


    private Algorithm mutualExclusionAlgo;

    private ArrayList<ITetrisPeer> mutualExclusionPeers;

    private ITetrisPeer mutualExclusionSelf;

    private Object mutualExclusionStateLock;
    private boolean mutualExclusionRequested;
    private boolean criticalSectionObtained;


    public final static String LOGICALCLOCK_ALGO="Ricart-Agrawala algorithm";
    public final static String TOKEN_ALGO="Raymond’s algorithm";
    public final static String QUORUM_ALGO="Agrawal-El Abbadi algorithm";

    public static String currentAlgo;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);


        Intent intent = getIntent();
        algorithm = intent.getExtras().getString(MainActivity.ALGORITHM);
        String playername = intent.getExtras().getString(MainActivity.PLAYERNAME);

        currentAlgo = algorithm;

        //set up UI
        tvAlgorithm = (TextView)findViewById(R.id.tvAlgorithm);
        tvAlgorithm.setText(algorithm);

        lvAlgorithmLog = (ListView)findViewById(R.id.algorithmLog);

        logAdapter =new ArrayAdapter<String>(this,
                R.layout.logtext,
                logData);
        lvAlgorithmLog.setAdapter(logAdapter);


        ivCSindicator = (ImageView)findViewById(R.id.ivCSIndicator);

        tvPlayerName = (TextView)findViewById(R.id.tvPlayerName);
        tvPlayerName.setText(playername);


        // set up views and game data
        mCurBlock = new Tetromino();
        mNextBlock = new Tetromino();
        mMyPlayfield = new Playfield(20, 10, 2);

        mMyFieldView = (PlayfieldView)this.findViewById(R.id.play_field);
        mMyFieldView.setPlayfield(mMyPlayfield);
        mMyFieldView.setBlockHighlighted(true);

        mNextBlockView = (PlayfieldView)findViewById(R.id.next_piece);
        mNextBlockView.setPlayfield(new Playfield(4, 4));
        mNextBlockView.setBlockHighlighted(false);


        createNextBlock(randIdx);

        mInputHandler = new Handler(getMainLooper(), this);
        mGestureHandler = new GestureDetector(this, new TetrisGestureListener());


        Toast.makeText(this, "Starting...", Toast.LENGTH_SHORT).show();
        mGameState = TetrisApplication.WAITING;
        mInputHandler.post(this);


        //Identify myself and my peers
        Globals.peer.setPeerActivity(this);

        mutualExclusionSelf = new TetrisPeer(Globals.peer.getAddressPeer());
        Log.i(TAG, "self: " + mutualExclusionSelf.toString());

        ArrayList<String> gamePeers = Globals.peer.getPeerList();
        mutualExclusionPeers = new ArrayList<>();

        for (String p : gamePeers) {
            Log.i(TAG,p);
            mutualExclusionPeers.add(new TetrisPeer(p));
        }



        switch (algorithm){
            case LOGICALCLOCK_ALGO:
                mutualExclusionAlgo = new RicartAgrawala(mutualExclusionPeers,mutualExclusionSelf);
                break;
            case TOKEN_ALGO:
                mutualExclusionAlgo = new Raymond(mutualExclusionPeers, mutualExclusionSelf);
                if (mutualExclusionAlgo.getCurrentCritSectPeer() != null){
                    ivCSindicator.setImageResource(R.drawable.color_label_circle_green);
                    criticalSectionObtained = true;
                }
                break;
            case QUORUM_ALGO:
                mutualExclusionAlgo = new AgrawalElAbbadi(mutualExclusionPeers, mutualExclusionSelf);
                break;
        }


        mutualExclusionStateLock = new Object();


        //to receive the messages from peers
        handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                String jsonString = msg.obj.toString();
                JSONObject jsonObj;
                try {
                    jsonObj = new JSONObject(jsonString);
                    ITetrisPeer messageSender = TetrisPeer.fromString((String) jsonObj.get(MESSAGE_SENDER_ID));
                    String readMessage = (String) jsonObj.get(MESSAGE_CONTENT);
                    int messageType = (int) jsonObj.get(MESSAGE_TYPE);


                    switch (messageType) {
                        case GAME_MESSAGE:
                            String message_sender = messageSender.toString().split("@")[0];
                            writeAlgoLog("Received from " + message_sender+ ": "+ readMessage);
                            handleGameMessage(readMessage);
                            break;

                        case ALGORITHM_MESSAGE:
                            handleAlgorithmMessage(messageSender, readMessage);
                            break;
                    }

                } catch (JSONException e) {
                    //e.printStackTrace();

                    //In case the message was sent from myself just to print out in the log
                    writeAlgoLog(msg.obj.toString());
                }
            }

            private void handleAlgorithmMessage(ITetrisPeer sender, String msg) {
                String message = "";
                String senderID = "";
                String vectorClock = "";
                int timeStamp;

                switch (algorithm){
                    case LOGICALCLOCK_ALGO:
                        TimeStampMessage ts = TimeStampMessage.fromString(msg);
                        timeStamp = ts.getTimestamp();
                        senderID = ts.getSenderID().split("@")[0];
                        message = ts.getMessage();
                        writeAlgoLog("Received " + message + " from " + senderID + " " + timeStamp);
                        break;
                    case TOKEN_ALGO:
                        message = msg;
                        writeAlgoLog("Received " + message + " from " + sender.getID().split("@")[0]);
                        break;
                    case QUORUM_ALGO:
                        QuorumMessage qm = QuorumMessage.fromString(msg);
                        vectorClock = qm.getTimestamp().getArrayString();
                        senderID = qm.getSenderID().split("@")[0];
                        message = qm.getMessage();
                        writeAlgoLog("Received " + message + " from " + senderID + " " + vectorClock);
                        break;

                }

                mutualExclusionAlgo.receiveMessage(sender, msg);


            }

            private void handleGameMessage(String msg) {
                switch (msg) {
                    case MOVE_LEFT:
                        moveBlock(-1, 0);
                        mMyFieldView.invalidateBlock();
                        break;
                    case MOVE_RIGHT:
                        moveBlock(1, 0);
                        mMyFieldView.invalidateBlock();
                        break;
                    case MOVE_DOWN:
                        while (moveBlock(0, 1)) ;
                        mInputHandler.removeCallbacks(PlayActivity.this);
                        mInputHandler.postDelayed(PlayActivity.this, BLOCK_DELAY);
                        mMyFieldView.invalidatePlayfield();
                        break;
                    default:
                        rotateBlock();
                        mMyFieldView.invalidateBlock();
                        break;
                }

            }
        };

    }


    @Override
    public void onResume() {
        super.onResume();

    }



    @Override
    protected void onPause()
    {
        super.onPause();

        stopRunning();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        stopRunning();
        finish();
    }

    // run timed events
    @Override
    public void run()
    {
        runGame();
    }


    private void runGame()
    {
        if (mGameState == TetrisApplication.WAITING)
        {
            mGameState = TetrisApplication.IN_CONTROL;
            shiftNextBlock();
            mInputHandler.post(this);
        }
        // check if block can be moved down
        else if (!moveBlock(0, 1)) //Block reached bottom
        {
            releaseCS();

            if (mMyPlayfield.insertBlock(mCurBlock) == 0)
                mMyFieldView.invalidateBlock();
            else
                mMyFieldView.invalidatePlayfield();
            
            // check game over
            if (mMyPlayfield.reachedTop())
            {
                Toast.makeText(this, "Game over", Toast.LENGTH_SHORT).show();
                stopRunning();
            }
            else
            {
                mGameState = TetrisApplication.WAITING;
                mInputHandler.post(this);
            }
        }
        else //Block keep falling
        {
            mMyFieldView.invalidateBlock();
            mInputHandler.postDelayed(this, BLOCK_DELAY);
        }
    }


    /**
     * Set the color of the lights indicator
     * @param indicator
     */
    private void setLightIndicator(final String indicator){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                switch (indicator) {
                    case YELLOW_LIGHT:
                        ivCSindicator.setImageResource(R.drawable.color_label_circle_yellow);
                        break;
                    case RED_LIGHT:
                        ivCSindicator.setImageResource(R.drawable.color_label_circle_red);
                        break;
                    case GREEN_LIGHT:
                        ivCSindicator.setImageResource(R.drawable.color_label_circle_green);
                        break;

                }

            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return mGestureHandler.onTouchEvent(event);
    }



    /**
     * Create a random block and insert it into the next piece view
     */
    private void createNextBlock(int index)
    {
        randIdx++;

        if (randIdx==7)
            randIdx=0;

        mNextBlock.copyRandBlock(index);
        mNextBlock.getCenter().set(1, 2);
        mNextBlockView.setBlock(mNextBlock);
        mNextBlockView.invalidateBlock();
    }




    /**
     * Move the next block into the playfield
     */
    private void shiftNextBlock()
    {
        mNextBlock = mCurBlock;
        mCurBlock = mNextBlockView.getBlock();
        mCurBlock.getCenter().set((mMyPlayfield.getCols() - 1) / 2, -1);
        mMyFieldView.setBlock(mCurBlock);

        createNextBlock(randIdx);
    }




    // try to shift block in direction, return if successful
    private boolean moveBlock(int dx, int dy)
    {
        mCurBlock.getCenter().offset(dx, dy);
        if (!mMyPlayfield.canInsert(mCurBlock))
        {
            mCurBlock.getCenter().offset(-dx, -dy);
            return false;
        }
        return true;
    }

    // rotate the block until it reaches a valid orientation
    private void rotateBlock()
    {
        mCurBlock.rotateCW();
        while (!mMyPlayfield.canInsert(mCurBlock))
        {
            if (moveBlock(-1, 0))
                return;
            else if (moveBlock(1, 0))
                return;
            else 
                mCurBlock.rotateCW();
        }
    }

    
    private void stopRunning()
    {
        mNextBlockView.setBlock(null);
        mNextBlockView.invalidatePlayfield();
        mInputHandler.removeCallbacks(this);
    }
    



    private class TetrisGestureListener extends GestureDetector.SimpleOnGestureListener
    {


        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY)
        {

            if (criticalSectionObtained) {
                makeMove(e1, e2, velocityX, velocityY);
            } else {
                requestAccessCS();
            }

            return true;
        }


        private void makeMove(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mGameState == TetrisApplication.IN_CONTROL)
            {

                if (System.currentTimeMillis() - initTime > 1000) { //wait at least 1 second between successive gestures

                    initTime = System.currentTimeMillis();

                    String gameMsgToSend;

                    if (Math.abs(velocityX) > Math.abs(velocityY))
                    {

                        if (velocityX > 0) {
                            gameMsgToSend = MOVE_RIGHT;
                            moveBlock(1, 0);
                        }else{
                            gameMsgToSend = MOVE_LEFT;
                            moveBlock(-1, 0);
                        }
                        mMyFieldView.invalidateBlock();
                    }
                    else
                    {

                        if (velocityY > 0)
                        {
                            gameMsgToSend = MOVE_DOWN;
                            while (moveBlock(0, 1));
                            mInputHandler.removeCallbacks(PlayActivity.this);
                            mInputHandler.postDelayed(PlayActivity.this, BLOCK_DELAY);
                            mMyFieldView.invalidatePlayfield();
                        }
                        else
                        {
                            gameMsgToSend = ROTATE;
                            rotateBlock();
                            mMyFieldView.invalidateBlock();
                        }
                    }

                    Log.i(TAG, gameMsgToSend);
                    writeAlgoLog(gameMsgToSend);

                    JSONObject json = new JSONObject();
                    try {
                        json.put(MESSAGE_SENDER_ID, mutualExclusionSelf.toString());
                        json.put(MESSAGE_TYPE, GAME_MESSAGE);
                        json.put(MESSAGE_CONTENT, gameMsgToSend);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Utils.sendGameMsg(json.toString());

                }
            }
        }

    }


    /**
     * Insert data into the Algorithm's Log
     * @param data
     */
    private void writeAlgoLog(final String data){

    runOnUiThread(new Runnable() {
        @Override
        public void run() {

            logData.add(data);
            logAdapter.notifyDataSetChanged();

            //Change the background and text color of last item from the Log
            try {
                View v = lvAlgorithmLog.getChildAt(lvAlgorithmLog.getLastVisiblePosition());
                v.setBackgroundResource(R.color.green);
                ((TextView) v).setTextColor(Color.BLACK);

            } catch (Exception e) {

            }

        }
    });


}




    /**
     * Requests access to the Critical Section via the set algorithm and updates the
     * Critical Section indicator light accordingly.
     *
     *  The light can have the following colours:
     *      - Red:    Currently not in Critical Section
     *      - Green:  Currently in the Critical Section
     *      - Yellow: Critical Section requested or released, but not confirmed
     */
    private void requestAccessCS() {

        synchronized (mutualExclusionStateLock) {
            // If we have already requested access there is no use in flooding the network with
            // duplicate requests.
            if (!mutualExclusionRequested) {
                writeAlgoLog("CS access requested");
                setLightIndicator(YELLOW_LIGHT);
                mutualExclusionRequested = true;

                // Obtain Critical Section in a new thread, since this operation is blocking.
                Thread obtainCS = new Thread() {
                    public void run() {
                        boolean success = mutualExclusionAlgo.obtainCritSection();

                        synchronized (mutualExclusionStateLock) {
                            if (success) {
                                setLightIndicator(GREEN_LIGHT);
                                writeAlgoLog("CS access obtained");
                                criticalSectionObtained = true;
                            } else {
                                setLightIndicator(RED_LIGHT);
                                writeAlgoLog("CS access request denied");
                                mutualExclusionRequested = false;
                            }
                        }
                    }
                };

                obtainCS.start();
            }
        }
    }

    /**
     * Requests the algorithm to release the Critical Section and changes the colour of the
     * Critical Section "light" indicator accordingly.
     */
    private void releaseCS() {
        synchronized (mutualExclusionStateLock) {
            if (criticalSectionObtained) {
                setLightIndicator(YELLOW_LIGHT);

                boolean success = mutualExclusionAlgo.releaseCritSection();

                if (success) {
                    setLightIndicator(RED_LIGHT);
                    writeAlgoLog("CS access released");
                    criticalSectionObtained = false;
                    mutualExclusionRequested = false;
                } else {
                    setLightIndicator(GREEN_LIGHT);
                    writeAlgoLog("CS access not released");
                    criticalSectionObtained = true;
                    mutualExclusionRequested = true;
                }
            }
        }
    }


    @Override
    public boolean handleMessage(Message msg)
    {
        if (msg.what == TetrisApplication.UPDATE_FIELD)
        {
            //Not using this
        }
        else if (msg.what == TetrisApplication.CLEAR_ROW)
            mActionQueue.add(msg.what);
        else
        {
            mActionQueue.clear();
            mActionQueue.add(0, msg.what);
            mGameState = TetrisApplication.WAITING;
        }

        return true;
    }



}












