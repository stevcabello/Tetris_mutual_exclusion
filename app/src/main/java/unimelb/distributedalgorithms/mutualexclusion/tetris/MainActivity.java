package unimelb.distributedalgorithms.mutualexclusion.tetris;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import unimelb.distributedalgorithms.mutualexclusion.tetris.game.PlayActivity;
import unimelb.distributedalgorithms.mutualexclusion.tetris.sip2peer.SimplePeer;
import unimelb.distributedalgorithms.mutualexclusion.tetris.utils.Globals;


public class MainActivity extends Activity {

    public String TAG = "MainActivity";

    public  Button btnPlay;
    private Button btnClose;
    private Button btnConnect;

    private RadioGroup radioAlgoGroup;
    private RadioButton radioAlgoButton;

    private static final int DIALOG_CONFIG = 5;

    private EditText etPlayerName;

    private Boolean peerPressedPlay = false;


    public static String ALGORITHM = "Algorithm";
    public static String PLAYERNAME = "Player Name";

    private String playerName;




    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Globals.IP_SERVER); //Home
        } catch (URISyntaxException e) {
            Log.i(TAG, "couldnt connect to server");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnPlay = (Button) findViewById(R.id.play_button);
        btnClose = (Button)findViewById(R.id.closeApp);
        btnConnect = (Button)findViewById(R.id.connect);
        etPlayerName = (EditText)findViewById(R.id.etPlayerName);
        radioAlgoGroup = (RadioGroup) findViewById(R.id.radioAlgorithm);


        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // get selected radio button from radioGroup
                int selectedId = radioAlgoGroup.getCheckedRadioButtonId();
                radioAlgoButton = (RadioButton) findViewById(selectedId);

                mSocket.emit("get peers list", radioAlgoButton.getText().toString()); //send the algorithm selected
                                                                                      //to be passed to the other peers
                                                                                      //through the server emit message

            }
        });




        btnConnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                playerName = etPlayerName.getText().toString();

                if(Globals.peer==null){
                    if (playerName.isEmpty())
                        Toast.makeText(MainActivity.this,"Please enter a player name",Toast.LENGTH_SHORT).show();
                    else{
                        init(etPlayerName.getText().toString().trim());

                        //To hide softkeyboard after pressing Connect button
                        View view = MainActivity.this.getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }

                        Globals.alreadyConnected = true;
                    }
                }

                if (Globals.peer != null) {
                    mSocket.connect();
                    btnPlay.setVisibility(View.VISIBLE);
                    btnClose.setVisibility(View.VISIBLE);
                    radioAlgoGroup.setVisibility(View.VISIBLE);
                    btnConnect.setVisibility(View.GONE);
                    etPlayerName.setVisibility(View.GONE);

                } else
                    showDialog(DIALOG_CONFIG);

            }
        });


        btnClose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Globals.peer.halt();

                mSocket.emit("disconnect peer", Globals.peer.getAddressPeer());
                mSocket.disconnect();

                Globals.peer.setPeerList(null);
                Globals.peer = null;

                Globals.alreadyConnected=false;

                finish();

            }
        });





        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "socket connected");
                // emit local ip addr to the Server
                mSocket.emit("new connection", Globals.peer.getAddressPeer());

            }

            // this is the emit from the server
        }).on("server message", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.i(TAG, args[0].toString());

                if (!peerPressedPlay) {

                    peerPressedPlay=true;

                    //Message structure : SelectedAlgorithm # PeersList
                    String selectedAlgo = args[0].toString().split("#")[0];
                    String message = cleanServerMsg(args[0].toString().split("#")[1]);

                    Globals.peer.setPeerList(new ArrayList<String>(Arrays.asList(message.split(","))));

                    startGame(selectedAlgo);
                }

            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "socket disconnected");
            }

        });





    }


    /**
     * Removes unnecessary characters from server's message
     * @param message
     */
    public String cleanServerMsg(String message) {

        message = message.replace("\\", "");
        message = message.replace("[", "");
        message = message.replace("]", "");
        message = message.replace("]", "");
        message = message.replace("\"", "");
        message = message.replace("'", "");
        message = message.replaceAll("\\s", "");


        if (message.endsWith("r")) { //some machines return \r char at the end
            int len_message = message.length();
            return message.substring(0, len_message - 1);
        }else
            return message;
    }


    /**
     * Starts the game on all peers based on the selected Algorithm
     * of the first peer that pressed the Play Button.
     * @param selectedAlgo
     */
    public void startGame(String selectedAlgo){

        if (Globals.peer.getPeerList()!=null){

            Intent intent = new Intent(MainActivity.this, PlayActivity.class);
            intent.putExtra(ALGORITHM, selectedAlgo);
            intent.putExtra(PLAYERNAME,playerName);
            startActivity(intent);


        }else{
            Toast.makeText(MainActivity.this,"Game couldn't be started. Please try again...",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Initializes the new peer based on the name provided
     * @param name
     */
    private void init(String name){
        Globals.peer = new SimplePeer(null, "4654amv65d4as4d65a4", name,  50250);
    }


    @Override
    public void onResume() {
        super.onResume();

        peerPressedPlay=false;

        if (Globals.alreadyConnected) {
            btnPlay.setVisibility(View.VISIBLE);
            btnClose.setVisibility(View.VISIBLE);
            radioAlgoGroup.setVisibility(View.VISIBLE);
            btnConnect.setVisibility(View.GONE);
            etPlayerName.setVisibility(View.GONE);
        }

    }



    @Override
    public void onPause() {
        super.onPause();

    }








}
