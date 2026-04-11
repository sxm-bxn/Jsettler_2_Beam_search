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
}