package soc.robot;

import soc.baseclient.ServerConnectInfo;
import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;



public class BSBRobotClient extends SOCRobotClient{

private static final String BSB_NAME_STRING = "BS_bot_1";
private static final String RBCLASSNAME_BSB = BSBRobotBrain.class.getName();

    public BSBRobotClient(final ServerConnectInfo sci, final String nn, final String pw)
        throws IllegalArgumentException
        {
            super(sci, BSB_NAME_STRING, pw);
            this.rbclass = RBCLASSNAME_BSB;
        }
    
    @Override
    public SOCRobotBrain createBrain(final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq)
    {
        return new BSBRobotBrain(this, params, ga, mq);
    }
        public static void main(String[] args)
    {
        if (args.length < 5)
        {
            System.err.println("Java Settlers sample robotclient");
            System.err.println("usage: java " + RBCLASSNAME_BSB + " hostname port_number bot_nickname password cookie");

            return;
        }

    /**
     * Main method.
     * @param args  Expected arguments: server hostname, port, bot username, bot password, server cookie
     */
        BSBRobotClient cli = new BSBRobotClient
            (new ServerConnectInfo(args[0], Integer.parseInt(args[1]), args[4]), args[2], args[3]);
        cli.init();
    }
}
