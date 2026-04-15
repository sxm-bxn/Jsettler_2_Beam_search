/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2025 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012 Paul Bilnoski <paul@bilnoski.net>
 * Portions of this file Copyright (C) 2017 Ruud Poutsma <rtimon@gmail.com>
 * Portions of this file Copyright (C) 2017-2018 Strategic Conversation (STAC Project) https://www.irit.fr/STAC/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *

 **/

package soc.robot;
import java.util.*;

// import soc.baseclient.SOCDisplaylessPlayerClient;
import soc.disableDebug.D;

// import soc.game.SOCBoard;
// import soc.game.SOCBoardLarge;
// import soc.game.SOCCity;
// import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
// import soc.game.SOCGameOptionSet;
// import soc.game.SOCInventory;
// import soc.game.SOCInventoryItem;
// import soc.game.SOCPlayer;
// import soc.game.SOCPlayerNumbers;
// import soc.game.SOCPlayingPiece;
// import soc.game.SOCResourceConstants;
// import soc.game.SOCResourceSet;
// import soc.game.SOCRoad;
// import soc.game.SOCRoutePiece;
// import soc.game.SOCSettlement;
// import soc.game.SOCShip;
// import soc.game.SOCSpecialItem;
// import soc.game.SOCTradeOffer;
import soc.game.SOCRoad;
import soc.game.SOCShip;
// import soc.message.SOCAcceptOffer;
// import soc.message.SOCBankTrade;
// import soc.message.SOCBotGameDataCheck;
// import soc.message.SOCCancelBuildRequest;
// import soc.message.SOCChoosePlayer;
// import soc.message.SOCChoosePlayerRequest;
// import soc.message.SOCClearOffer;
// import soc.message.SOCDeclinePlayerRequest;
// import soc.message.SOCDevCardAction;
// import soc.message.SOCDiceResult;
// import soc.message.SOCDiceResultResources;
// import soc.message.SOCDiscard;
// import soc.message.SOCDiscardRequest;
// import soc.message.SOCGameState;
// import soc.message.SOCGameStats;
// import soc.message.SOCInventoryItemAction;
// import soc.message.SOCMakeOffer;
import soc.message.SOCMessage;
// import soc.message.SOCMovePiece;
// import soc.message.SOCMoveRobber;
// import soc.message.SOCPlayerElement;
// import soc.message.SOCPlayerElement.PEType;
// import soc.message.SOCPlayerElements;
// import soc.message.SOCPutPiece;
// import soc.message.SOCRejectOffer;
// import soc.message.SOCResourceCount;
// import soc.message.SOCRobberyResult;
// import soc.message.SOCSetSpecialItem;
// import soc.message.SOCSimpleAction;
// import soc.message.SOCSimpleRequest;
// import soc.message.SOCSitDown;  // for javadoc
// import soc.message.SOCStartGame;
// import soc.message.SOCTimingPing;  // for javadoc
// import soc.message.SOCTurn;
// import soc.message.SOCUndoPutPiece;

import soc.util.CappedQueue;
// import soc.util.DebugRecorder;
import soc.util.SOCRobotParameters;

// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.List;
// import java.util.Random;
// import java.util.Vector;



public class BSBRobotBrain extends SOCRobotBrain{

    public BSBRobotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue<SOCMessage> mq) {
        super(rc, params, ga, mq);
        D.ebugPrintlnINFO();
    }

    public static int width = 5;
    public static int depth = 5;
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
                    final int firstSettleNode = openingBuildStrategy.planInitialSettlements();
                    final TreeMap<Integer,Integer> coordProbMap = (openingBuildStrategy.fastProbabiltySearch());
                    final List<Integer> breadthSearchList = openingBuildStrategy.nodesForAnalysis(coordProbMap, null);
                    placeFirstSettlement(firstSettleNode);
                    expectPUTPIECE_FROM_START1A = true;
                    waitingForGameState = true;
                    counter = 0;
                    soc.debug.D.ebugPrintlnINFO("------------normal choice---" + firstSettleNode+ "-------------------------");
                    soc.debug.D.ebugPrintlnINFO(Arrays.toString(breadthSearchList.toArray()) );
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