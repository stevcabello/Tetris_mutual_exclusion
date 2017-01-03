package unimelb.distributedalgorithms.mutualexclusion.tetris.utils;


import unimelb.distributedalgorithms.mutualexclusion.tetris.sip2peer.SimplePeer;

/**
 * Common attributes among Classes
 */

public class Globals {

    public static String IP_SERVER = "http://192.168.1.10:3000"; //CHANGE THIS IP ADDRESS WITH YOUR LOCAL MACHINE IP ADDRESS
    public static SimplePeer peer = null;
    public static Boolean alreadyConnected = false; //to avoid a reconnection of an already connected peer

}
