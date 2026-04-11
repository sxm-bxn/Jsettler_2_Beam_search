package soc.robot;

import soc.debug.D;
import soc.game.SOCGame;
import soc.game.SOCRoad;
import soc.game.SOCShip;
import soc.message.SOCMessage;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

public class RANRobotBrain extends SOCRobotBrain {
    public RANRobotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue<SOCMessage> mq) {
        super(rc, params, ga, mq);
    }

    @Override
    protected void placeIfExpectPlacing()
    {
     if (waitingForGameState)
            return;

        switch (game.getGameState())
        {
        case SOCGame.PLACING_SETTLEMENT:
            if (ourTurn && (! waitingForOurTurn) && (expectPLACING_SETTLEMENT))
            {
                expectPLACING_SETTLEMENT = false;
                waitingForGameState = true;
                counter = 0;
                expectPLAY1 = true;

                //D.ebugPrintln("!!! PUTTING PIECE "+whatWeWantToBuild+" !!!");
                pause(500);
                client.putPiece(game, whatWeWantToBuild);
                pause(1000);
            }
            break;

        case SOCGame.PLACING_ROAD:
            if (ourTurn && (! waitingForOurTurn) && (expectPLACING_ROAD))
            {
                expectPLACING_ROAD = false;
                waitingForGameState = true;
                counter = 0;
                expectPLAY1 = true;

                pause(500);
                client.putPiece(game, whatWeWantToBuild);
                pause(1000);
            }
            break;

        case SOCGame.PLACING_CITY:
            if (ourTurn && (! waitingForOurTurn) && (expectPLACING_CITY))
            {
                expectPLACING_CITY = false;
                waitingForGameState = true;
                counter = 0;
                expectPLAY1 = true;

                pause(500);
                client.putPiece(game, whatWeWantToBuild);
                pause(1000);
            }
            break;

        case SOCGame.PLACING_SHIP:
            if (ourTurn && (! waitingForOurTurn) && (expectPLACING_SHIP))
            {
                expectPLACING_SHIP = false;
                waitingForGameState = true;
                counter = 0;
                expectPLAY1 = true;

                pause(500);
                client.putPiece(game, whatWeWantToBuild);
                pause(1000);
            }
            break;

        case SOCGame.PLACING_FREE_ROAD1:
            if (ourTurn && (! waitingForOurTurn) && (expectPLACING_FREE_ROAD1))
            {
                expectPLACING_FREE_ROAD1 = false;
                waitingForGameState = true;
                counter = 0;
                expectPLACING_FREE_ROAD2 = true;

                // D.ebugPrintln("!!! PUTTING PIECE 1 " + whatWeWantToBuild + " !!!");
                pause(500);
                client.putPiece(game, whatWeWantToBuild);  // either ROAD or SHIP
                pause(1000);
            }
            break;

        case SOCGame.PLACING_FREE_ROAD2:
            if (ourTurn && (! waitingForOurTurn) && (expectPLACING_FREE_ROAD2))
            {
                expectPLACING_FREE_ROAD2 = false;
                waitingForGameState = true;
                counter = 0;
                expectPLAY1 = true;

                SOCPossiblePiece posPiece = buildingPlan.advancePlan();

                if (posPiece.getType() == SOCPossiblePiece.ROAD)
                    whatWeWantToBuild = new SOCRoad(ourPlayerData, posPiece.getCoordinates(), null);
                else
                    whatWeWantToBuild = new SOCShip(ourPlayerData, posPiece.getCoordinates(), null);

                // D.ebugPrintln("posPiece = " + posPiece);
                // D.ebugPrintln("$ POPPED OFF");
                // D.ebugPrintln("!!! PUTTING PIECE 2 " + whatWeWantToBuild + " !!!");
                pause(500);
                client.putPiece(game, whatWeWantToBuild);
                pause(1000);
            }
            break;

        case SOCGame.START1A:
            {
                if ((! waitingForOurTurn) && ourTurn && (! (expectPUTPIECE_FROM_START1A && (counter < 4000))))
                {
                    final int firstSettleNode = openingBuildStrategy.randomSettlement();
                    placeFirstSettlement(firstSettleNode);
                    expectPUTPIECE_FROM_START1A = true;
                    waitingForGameState = true;
                    counter = 0;
                    soc.debug.D.ebugPrintlnINFO("------------random choice---" + firstSettleNode+ "-------------------------");
                }

                expectSTART1A = false;
            }
            break;

        case SOCGame.START1B:
            {
                if ((! waitingForOurTurn) && ourTurn && (! (expectPUTPIECE_FROM_START1B && (counter < 4000))))
                {
                    planAndPlaceInitRoad();

                    expectPUTPIECE_FROM_START1B = true;
                    counter = 0;
                    waitingForGameState = true;
                    waitingForOurTurn = true;  // ignore next player's GameState(START1A) message seen before Turn(nextPN)
                    pause(1500);
                }

                expectSTART1B = false;
            }
            break;

        case SOCGame.START2A:
            {
                if ((! waitingForOurTurn) && ourTurn && (! (expectPUTPIECE_FROM_START2A && (counter < 4000))))
                {
                    final int secondSettleNode = openingBuildStrategy.randomSettlement();
                    placeInitSettlement(secondSettleNode);

                    expectPUTPIECE_FROM_START2A = true;
                    counter = 0;
                    waitingForGameState = true;
                    soc.debug.D.ebugPrintlnINFO("------------------random choice" + secondSettleNode + "-----------------------");
                }

                expectSTART2A = false;
            }
            break;

        case SOCGame.START2B:
            {
                if ((! waitingForOurTurn) && ourTurn && (! (expectPUTPIECE_FROM_START2B && (counter < 4000))))
                {
                    planAndPlaceInitRoad();

                    expectPUTPIECE_FROM_START2B = true;
                    counter = 0;
                    waitingForGameState = true;
                    waitingForOurTurn = true;  // ignore next player's GameState(START2A) message seen before Turn(nextPN)
                    pause(1500);
                }

                expectSTART2B = false;
            }
            break;

        case SOCGame.START3A:
            {
                if ((! waitingForOurTurn) && ourTurn && (! (expectPUTPIECE_FROM_START3A && (counter < 4000))))
                {
                    final int secondSettleNode = openingBuildStrategy.planSecondSettlement();  // TODO planThirdSettlement
                    placeInitSettlement(secondSettleNode);

                    expectPUTPIECE_FROM_START3A = true;
                    counter = 0;
                    waitingForGameState = true;
                }

                expectSTART3A = false;
            }
            break;

        case SOCGame.START3B:
            {
                if ((! waitingForOurTurn) && ourTurn && (! (expectPUTPIECE_FROM_START3B && (counter < 4000))))
                {
                    planAndPlaceInitRoad();

                    expectPUTPIECE_FROM_START3B = true;
                    counter = 0;
                    waitingForGameState = true;
                    waitingForOurTurn = true;  // ignore next player's GameState(START3A) message seen before Turn(nextPN)
                    pause(1500);
                }

                expectSTART3B = false;
            }
            break;

        }
    }
}
