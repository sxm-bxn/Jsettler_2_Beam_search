package soc.robot;

import soc.disableDebug.D;
import soc.baseclient.ServerConnectInfo;
import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

public class RANRobotClient extends SOCRobotClient {
    

private static final String RBCLASSNAME_RAN = RANRobotBrain.class.getName();

    public RANRobotClient(final ServerConnectInfo sci, final String nn, final String pw)
        throws IllegalArgumentException
        {
            super(sci, nn, pw);
            this.rbclass = RBCLASSNAME_RAN;
        }
    
    @Override
    public SOCRobotBrain createBrain(final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq)
    {
        return new RANRobotBrain(this, params, ga, mq);
    }
        public static void main(String[] args)
    {
        if (args.length < 5)
        {
            D.ebugPrintlnINFO("Java Settlers sample robotclient");
            D.ebugPrintlnINFO("usage: java " + RBCLASSNAME_RAN + " hostname port_number bot_nickname password cookie");

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
