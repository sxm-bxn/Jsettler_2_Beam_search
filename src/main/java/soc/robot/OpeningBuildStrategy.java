/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file copyright (C) 2008 Eli McGowan <http://sourceforge.net/users/emcgowan>
 * Portions of this file copyright (C) 2003-2004 Robert S. Thomas
 * Portions of this file copyright (C) 2008 Christopher McNeil <http://sourceforge.net/users/cmcneil>
 * Portions of this file copyright (C) 2009-2013,2017-2025 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2012 Paul Bilnoski <paul@bilnoski.net>
 * Portions of this file Copyright (C) 2017 Ruud Poutsma <rtimon@gmail.com>
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
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.robot;

import java.util.*;

import javax.swing.text.StyledEditorKit.BoldAction;

import org.javatuples.Pair;
import java.lang.reflect.Array;

// import java.util.Arrays;
// import java.util.Comparator;
// import java.util.Enumeration;
// import java.util.Hashtable;
// import java.util.Iterator;
// import java.util.List;
// import java.util.Map;
// import java.util.Random;
// import java.util.Set;
// import java.util.TreeMap;

// import org.apache.log4j.Logger;

import soc.disableDebug.D;
import soc.game.*;
import soc.util.CutoffExceededException;

/**
 * This class is a temporary class put in place to slowly pull tasks out of SOCRobotBrain
 * and start replacing them with classes that implement strategy interfaces. (Strategy Pattern)
 * @author Eli McGowan
 *
 */
public class OpeningBuildStrategy {

    /** Our game */
    protected final SOCGame game;

    /** Our {@link SOCRobotBrain}'s player */
    protected final SOCPlayer ourPlayerData;

    /**
     * Our {@link SOCBuildingSpeedEstimate} factory, from {@link #ourPlayerData}'s brain passed into constructor.
     * @since 2.5.00
     */
    protected final SOCBuildingSpeedEstimateFactory bseFactory;

    /** debug logging */
    // private transient Logger log = Logger.getLogger(this.getClass().getName());
    protected transient D log = new D();

    /**
     * used in planning where to put our first and second settlements
     */
    protected int firstSettlement;

    /**
     * used in planning where to put our first and second settlements
     */
    protected int secondSettlement;

    /**
     * Coordinate of a future settlement 2 nodes away from settlementNode
     * (from {@link #firstSettlement} or {@link #secondSettlement}).
     * Valid after calling {@link #planInitRoad()}.
     * @since 2.0.00
     */
    protected int plannedRoadDestinationNode;

    /**
     * Cached resource estimates for the board;
     * <tt>resourceEstimates</tt>[{@link SOCBoard#CLAY_HEX}] == the clay rarity,
     * as an integer percentage 0-100 of dice rolls.
     * Initialized in {@link #estimateResourceRarity()}.
     */
    protected int[] resourceEstimates;

    /**
     * Create an OpeningBuildStrategy for a {@link SOCRobotBrain}'s or {@link SOCRobotDM}'s player.
     * @param ga  Our game
     * @param pl  Our player data in {@code ga}
     * @param br  Robot brain for {@code pl} if available, or null
     * @throws IllegalArgumentException if {@code pl} is null
     */
    public OpeningBuildStrategy(SOCGame ga, SOCPlayer pl, SOCRobotBrain br)
        throws IllegalArgumentException
    {
        if (pl == null)
            throw new IllegalArgumentException();

        game = ga;
        ourPlayerData = pl;
        bseFactory = (br != null)
            ? br.getEstimatorFactory()
            : new SOCBuildingSpeedEstimateFactory(null);
    }

      

    /**
     * Callback from {@link SOCRobotBrain#cancelWrongPiecePlacement(soc.message.SOCCancelBuildRequest)}
     * In case this OBS wants to take any other action to prevent re-sending the cancelled piece.
     * Game state will still be the state in which this piece's placement was attempted:
     * {@link SOCGame#START1A}, {@link SOCGame#START2B}, etc.
     * Any overriders should call {@code super.cancelWrongPiecePlacement(..)}.
     *<P>
     * In versions before 2.5.00, if an initial road was cancelled, bot would call
     * {@link SOCPlayer#clearPotentialSettlement(int) ourPlayerData.clearPotentialSettlement(nodeCoord)}
     * on the future-planned settlement node we were aiming for,
     * stored in this OBS's {@code plannedRoadDestinationNode} field.
     * That action might not apply to every third-party bot's OBS.
     *
     * @param cancelPiece  Playing piece type and coordinate which were rejected by server; not null
     * @since 2.5.00
     */
    public void cancelWrongPiecePlacement(final SOCPlayingPiece cancelPiece)
    {
        if (cancelPiece instanceof SOCRoutePiece)
        {
            // needed for planInitRoad() calculations
            if (plannedRoadDestinationNode > 0)
                ourPlayerData.clearPotentialSettlement(plannedRoadDestinationNode);
        }
    }

    /**
     * Figure out where to place the first settlement.
     * @return {@link #firstSettlement}, or 0 if no potential settlements for our player
     * @see #planSecondSettlement()
     */
    public int planInitialSettlements()
    {
        log.debug("--- planInitialSettlements ---");

        int speed;
        boolean allTheWay;
        firstSettlement = 0;
        secondSettlement = 0;

        int bestSpeed = 4 * SOCBuildingSpeedEstimate.DEFAULT_ROLL_LIMIT;
        SOCBoard board = game.getBoard();
        SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        int probTotal;
        int bestProbTotal;
        boolean[] ports = new boolean[SOCBoard.WOOD_PORT + 1];
        SOCBuildingSpeedEstimate estimate = bseFactory.getEstimator();
        final int[] prob = SOCNumberProbabilities.INT_VALUES;

        bestProbTotal = 0;

        final int[] ourPotentialSettlements = ourPlayerData.getPotentialSettlements_arr();
        if (ourPotentialSettlements == null)
            return 0;  // Should not occur

        for (int i = 0; i < ourPotentialSettlements.length; ++i)
        {
            final int firstNode = ourPotentialSettlements[i];
            // assert: ourPlayerData.isPotentialSettlement(firstNode)

            final Integer firstNodeInt = Integer.valueOf(firstNode);

            //
            // this is just for testing purposes
            //
            log.debug("FIRST NODE -----------");
            log.debug("firstNode = " + board.nodeCoordToString(firstNode));

            StringBuffer sb = new StringBuffer();
            sb.append("numbers:[");

            playerNumbers.clear();
            probTotal = playerNumbers.updateNumbersAndProbability
                (firstNode, board, prob, sb);

            sb.append("]");
            log.debug(sb.toString());
            sb = new StringBuffer();
            sb.append("ports: ");

            for (int portType = SOCBoard.MISC_PORT;
                     portType <= SOCBoard.WOOD_PORT; portType++)
            {
                ports[portType] = (board.getPortCoordinates(portType).contains(firstNodeInt));

                sb.append(ports[portType] + "  ");
            }

            log.debug(sb.toString());
            log.debug("probTotal = " + probTotal);
            estimate.recalculateEstimates(playerNumbers);
            speed = 0;
            allTheWay = false;

            try
            {
                speed += estimate.calculateRollsAndRsrcFast(SOCResourceSet.EMPTY_SET, SOCSettlement.COST, 300, ports).getRolls();
                speed += estimate.calculateRollsAndRsrcFast(SOCResourceSet.EMPTY_SET, SOCCity.COST, 300, ports).getRolls();
                speed += estimate.calculateRollsAndRsrcFast(SOCResourceSet.EMPTY_SET, SOCDevCard.COST, 300, ports).getRolls();
                speed += estimate.calculateRollsAndRsrcFast(SOCResourceSet.EMPTY_SET, SOCRoad.COST, 300, ports).getRolls();
            }
            catch (CutoffExceededException e) {}

            if (D.ebugOn)
            {
                final int[] rolls = estimate.getEstimatesFromNothingFast(ports, 300);
                sb = new StringBuffer();
                sb.append(" road: " + rolls[SOCBuildingSpeedEstimate.ROAD]);
                sb.append(" stlmt: " + rolls[SOCBuildingSpeedEstimate.SETTLEMENT]);
                sb.append(" city: " + rolls[SOCBuildingSpeedEstimate.CITY]);
                sb.append(" card: " + rolls[SOCBuildingSpeedEstimate.CARD]);
                log.debug(sb.toString());
                log.debug("speed = " + speed);
            }

            //
            // end test
            //

            //
            // calculate pairs of first and second settlement together
            //

            for (int j = 1 + i; j < ourPotentialSettlements.length; ++j)
            {
                final int secondNode = ourPotentialSettlements[j];
                // assert: ourPlayerData.isPotentialSettlement(secondNode)

                if (board.isNodeAdjacentToNode(secondNode, firstNode))
                    continue;  // <-- too close to firstNode to build --

                log.debug("firstNode = " + board.nodeCoordToString(firstNode));
                log.debug("secondNode = " + board.nodeCoordToString(secondNode));

                /**
                 * get the numbers for these settlements
                 */
                sb = new StringBuffer();
                sb.append("numbers:[");

                playerNumbers.clear();
                probTotal = playerNumbers.updateNumbersAndProbability
                    (firstNode, board, prob, sb);

                sb.append("] [");

                probTotal += playerNumbers.updateNumbersAndProbability
                    (secondNode, board, prob, sb);

                sb.append("]");
                log.debug(sb.toString());

                /**
                 * see if the settlements are on any ports
                 */
                //sb = new StringBuffer();
                //sb.append("ports: ");

                Arrays.fill(ports, false);
                int portType = board.getPortTypeFromNodeCoord(firstNode);
                if (portType != -1)
                    ports[portType] = true;
                portType = board.getPortTypeFromNodeCoord(secondNode);
                if (portType != -1)
                    ports[portType] = true;

                //log.debug(sb.toString());
                log.debug("probTotal = " + probTotal);

                /**
                 * estimate the building speed for this pair
                 */
                estimate.recalculateEstimates(playerNumbers);
                speed = 0;
                allTheWay = false;

                try
                {
                    speed += estimate.calculateRollsAndRsrcFast
                        (SOCResourceSet.EMPTY_SET, SOCSettlement.COST, bestSpeed, ports).getRolls();

                    if (speed < bestSpeed)
                    {
                        speed += estimate.calculateRollsAndRsrcFast
                            (SOCResourceSet.EMPTY_SET, SOCCity.COST, bestSpeed, ports).getRolls();

                        if (speed < bestSpeed)
                        {
                            speed += estimate.calculateRollsAndRsrcFast
                                (SOCResourceSet.EMPTY_SET, SOCDevCard.COST, bestSpeed, ports).getRolls();

                            if (speed < bestSpeed)
                            {
                                speed += estimate.calculateRollsAndRsrcFast
                                    (SOCResourceSet.EMPTY_SET, SOCRoad.COST, bestSpeed, ports).getRolls();
                                allTheWay = true;
                            }
                        }
                    }

                    // because of addition, speed might be as much as (bestSpeed - 1) + bestSpeed
                }
                catch (CutoffExceededException e)
                {
                    speed = bestSpeed;
                }

                if (D.ebugOn)
                {
                    final int[] rolls = estimate.getEstimatesFromNothingFast(ports, bestSpeed);
                    sb = new StringBuffer();
                    sb.append(" road: " + rolls[SOCBuildingSpeedEstimate.ROAD]);
                    sb.append(" stlmt: " + rolls[SOCBuildingSpeedEstimate.SETTLEMENT]);
                    sb.append(" city: " + rolls[SOCBuildingSpeedEstimate.CITY]);
                    sb.append(" card: " + rolls[SOCBuildingSpeedEstimate.CARD]);
                    log.debug(sb.toString());
                    log.debug("allTheWay = " + allTheWay);
                    log.debug("speed = " + speed);
                }

                /**
                 * keep the settlements with the best speed
                 */
                if (speed < bestSpeed)
                {
                    firstSettlement = firstNode;
                    secondSettlement = secondNode;
                    bestSpeed = speed;
                    bestProbTotal = probTotal;
                    log.debug("bestSpeed = " + bestSpeed);
                    log.debug("bestProbTotal = " + bestProbTotal);
                }
                else if ((speed == bestSpeed) && allTheWay)
                {
                    if (probTotal > bestProbTotal)
                    {
                        log.debug("Equal speed, better prob");
                        firstSettlement = firstNode;
                        secondSettlement = secondNode;
                        bestSpeed = speed;
                        bestProbTotal = probTotal;
                        log.debug("firstSettlement = " + Integer.toHexString(firstSettlement));
                        log.debug("secondSettlement = " + Integer.toHexString(secondSettlement));
                        log.debug("bestSpeed = " + bestSpeed);
                        log.debug("bestProbTotal = " + bestProbTotal);
                    }
                }

            }  // for (j past i in ourPotentialSettlements[])

        }  // for (i in ourPotentialSettlements[])

        /**
         * choose which settlement to place first
         */
        playerNumbers.clear();
        playerNumbers.updateNumbers(firstSettlement, board);

        final Integer firstSettlementInt = Integer.valueOf(firstSettlement);

        for (int portType = SOCBoard.MISC_PORT; portType <= SOCBoard.WOOD_PORT;
                 portType++)
        {
            ports[portType] = (board.getPortCoordinates(portType).contains(firstSettlementInt));
        }

        estimate.recalculateEstimates(playerNumbers);

        int firstSpeed = 0;
        final int cutoff = 100;

        firstSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCSettlement.COST, cutoff, ports);
        firstSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCCity.COST, cutoff, ports);
        firstSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCDevCard.COST, cutoff, ports);
        firstSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCRoad.COST, cutoff, ports);

        playerNumbers.clear();
        playerNumbers.updateNumbers(secondSettlement, board);

        final Integer secondSettlementInt = Integer.valueOf(secondSettlement);

        for (int portType = SOCBoard.MISC_PORT; portType <= SOCBoard.WOOD_PORT;
                 portType++)
        {
            ports[portType] = (board.getPortCoordinates(portType).contains(secondSettlementInt));
        }

        estimate.recalculateEstimates(playerNumbers);

        int secondSpeed = 0;

        secondSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCSettlement.COST, bestSpeed, ports);
        secondSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCCity.COST, bestSpeed, ports);
        secondSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCDevCard.COST, bestSpeed, ports);
        secondSpeed += estimate.calculateRollsFast(SOCResourceSet.EMPTY_SET, SOCRoad.COST, bestSpeed, ports);

        if (firstSpeed > secondSpeed)
        {
            int tmp = firstSettlement;
            firstSettlement = secondSettlement;
            secondSettlement = tmp;
        }

        log.debug
            (board.nodeCoordToString(firstSettlement) + ":" + firstSpeed + ", "
             + board.nodeCoordToString(secondSettlement) + ":" + secondSpeed);

        return firstSettlement;
    }

    /**
     * figure out where to place the second settlement
     * @return {@link #secondSettlement}, or -1 if none
     * @see #planInitialSettlements()
     */
    public int planSecondSettlement()
    {
        log.debug("--- planSecondSettlement");

        int bestSpeed = 4 * SOCBuildingSpeedEstimate.DEFAULT_ROLL_LIMIT;
        final SOCBoard board = game.getBoard();
        SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        boolean[] ports = new boolean[SOCBoard.WOOD_PORT + 1];
        SOCBuildingSpeedEstimate estimate = bseFactory.getEstimator();
        int probTotal;
        int bestProbTotal;
        final int[] prob = SOCNumberProbabilities.INT_VALUES;
        final int firstNode = firstSettlement;

        bestProbTotal = 0;
        secondSettlement = -1;

        final int[] ourPotentialSettlements = ourPlayerData.getPotentialSettlements_arr();
        if (ourPotentialSettlements == null)
            return -1;  // Should not occur

        for (int i = 0; i < ourPotentialSettlements.length; ++i)
        {
            final int secondNode = ourPotentialSettlements[i];
            // assert: ourPlayerData.isPotentialSettlement(secondNode)

            if (board.isNodeAdjacentToNode(secondNode, firstNode))
                continue;  // <-- too close to firstNode to build --

            /**
             * get the numbers for these settlements
             */
            StringBuffer sb = new StringBuffer();
            sb.append("numbers: ");
            playerNumbers.clear();
            probTotal = playerNumbers.updateNumbersAndProbability
                (firstNode, board, prob, sb);
            probTotal += playerNumbers.updateNumbersAndProbability
                (secondNode, board, prob, sb);

            /**
             * see if the settlements are on any ports
             */
            //sb.append("ports: ");

            Arrays.fill(ports, false);
            int portType = board.getPortTypeFromNodeCoord(firstNode);
            if (portType != -1)
                ports[portType] = true;
            portType = board.getPortTypeFromNodeCoord(secondNode);
            if (portType != -1)
                ports[portType] = true;

            //log.debug(sb.toString());
            log.debug("probTotal = " + probTotal);

            /**
             * estimate the building speed for this pair
             */
            estimate.recalculateEstimates(playerNumbers);

            int speed = 0;

            try
            {
                speed += estimate.calculateRollsAndRsrcFast
                    (SOCResourceSet.EMPTY_SET, SOCSettlement.COST, bestSpeed, ports).getRolls();

                if (speed < bestSpeed)
                {
                    speed += estimate.calculateRollsAndRsrcFast
                        (SOCResourceSet.EMPTY_SET, SOCCity.COST, bestSpeed, ports).getRolls();

                    if (speed < bestSpeed)
                    {
                        speed += estimate.calculateRollsAndRsrcFast
                            (SOCResourceSet.EMPTY_SET, SOCDevCard.COST, bestSpeed, ports).getRolls();

                        if (speed < bestSpeed)
                        {
                            speed += estimate.calculateRollsAndRsrcFast
                                (SOCResourceSet.EMPTY_SET, SOCRoad.COST, bestSpeed, ports).getRolls();
                        }
                    }
                }

                // because of addition, speed might be as much as (bestSpeed - 1) + bestSpeed
            }
            catch (CutoffExceededException e)
            {
                speed = bestSpeed;
            }

            log.debug(Integer.toHexString(firstNode) + ", " + Integer.toHexString(secondNode) + ":" + speed);

            /**
             * keep the settlements with the best speed
             */
            if ((speed < bestSpeed) || (secondSettlement < 0))
            {
                firstSettlement = firstNode;
                secondSettlement = secondNode;
                bestSpeed = speed;
                bestProbTotal = probTotal;
                log.debug("firstSettlement = " + Integer.toHexString(firstSettlement));
                log.debug("secondSettlement = " + Integer.toHexString(secondSettlement));

                int[] rolls = estimate.getEstimatesFromNothingFast(ports);
                sb = new StringBuffer();
                sb.append("road: " + rolls[SOCBuildingSpeedEstimate.ROAD]);
                sb.append(" stlmt: " + rolls[SOCBuildingSpeedEstimate.SETTLEMENT]);
                sb.append(" city: " + rolls[SOCBuildingSpeedEstimate.CITY]);
                sb.append(" card: " + rolls[SOCBuildingSpeedEstimate.CARD]);
                log.debug(sb.toString());
                log.debug("bestSpeed = " + bestSpeed);
            }
            else if (speed == bestSpeed)
            {
                if (probTotal > bestProbTotal)
                {
                    firstSettlement = firstNode;
                    secondSettlement = secondNode;
                    bestSpeed = speed;
                    bestProbTotal = probTotal;
                    log.debug("firstSettlement = " + Integer.toHexString(firstSettlement));
                    log.debug("secondSettlement = " + Integer.toHexString(secondSettlement));

                    int[] rolls = estimate.getEstimatesFromNothingFast(ports);
                    sb = new StringBuffer();
                    sb.append("road: " + rolls[SOCBuildingSpeedEstimate.ROAD]);
                    sb.append(" stlmt: " + rolls[SOCBuildingSpeedEstimate.SETTLEMENT]);
                    sb.append(" city: " + rolls[SOCBuildingSpeedEstimate.CITY]);
                    sb.append(" card: " + rolls[SOCBuildingSpeedEstimate.CARD]);
                    log.debug(sb.toString());
                    log.debug("bestSpeed = " + bestSpeed);
                }
            }

        }  // for (i in ourPotentialSettlements[])

        return secondSettlement;
    }

    /**
     * Plan and place a road attached to our most recently placed initial settlement
     * {@link SOCPlayer#getLastSettlementCoord()},
     * in game states {@link SOCGame#START1B START1B}, {@link SOCGame#START2B START2B}, {@link SOCGame#START3B START3B}.
     *<P>
     * Road choice is based on the best nearby potential settlements, and doesn't
     * directly check {@link SOCPlayer#isPotentialRoad(int) ourPlayerData.isPotentialRoad(edgeCoord)}.
     *<P>
     * If server rejects our road choice, bot will call {@link #cancelWrongPiecePlacement(SOCPlayingPiece)}
     * in case the OBS wants to take action to prevent re-choosing the same wrong choice again.
     *
     * @return road edge adjacent to initial settlement node {@link SOCPlayer#getLastSettlementCoord()}
     */
    public int planInitRoad()
    {
        // TODO handle ships here, especially in scenarios with fog
        // or SVP for reaching other islands (_SC_SANY, _SC_SEAC).
        // See also SOCRobotDM.planRoadBuildingTwoRoads

        final int settlementNode = ourPlayerData.getLastSettlementCoord();

        /**
         * Score the nearby nodes to build road towards: Key = coord Integer; value = Integer score towards "best" node.
         */
        Hashtable<Integer,Integer> twoAway = new Hashtable<Integer,Integer>();

        log.debug("--- placeInitRoad");

        /**
         * look at all of the nodes that are 2 away from the
         * last settlement, and pick the best one
         */
        final SOCBoard board = game.getBoard();

        for (int facing = 1; facing <= 6; ++facing)
        {
            // each of 6 directions: NE, E, SE, SW, W, NW
            int tmp = board.getAdjacentNodeToNode2Away(settlementNode, facing);
            if ((tmp != -9) && ourPlayerData.canPlaceSettlement(tmp))
                twoAway.put(Integer.valueOf(tmp), Integer.valueOf(0));
        }

        scoreNodesForSettlements(twoAway, 3, 5, 10);

        log.debug("Init Road for " + ourPlayerData.getName());

        /**
         * create a dummy player to calculate possible places to build
         * taking into account where other players will build before
         * we can.
         */
        SOCPlayer dummy = new SOCPlayer(ourPlayerData.getPlayerNumber(), game);

        if ((game.getGameState() == SOCGame.START1B)
            || (game.isGameOptionSet(SOCGameOptionSet.K_SC_3IP) && (game.getGameState() == SOCGame.START2B)))
        {
            /**
             * do a look ahead so we don't build toward a place
             * where someone else will build first.
             */
            final int numberOfBuilds = numberOfEnemyBuilds();
            log.debug("Other players will build " + numberOfBuilds + " settlements before I get to build again.");

            if (numberOfBuilds > 0)
            {
                /**
                 * rule out where other players are going to build
                 */
                Hashtable<Integer,Integer> allNodes = new Hashtable<Integer,Integer>();

                {
                    Iterator<Integer> psi = ourPlayerData.getPotentialSettlements().iterator();
                    while (psi.hasNext())
                        allNodes.put(psi.next(), Integer.valueOf(0));
                    // log.debug("-- potential settlement at " + Integer.toHexString(next));
                }

                /**
                 * favor spots with the most high numbers
                 */
                bestSpotForNumbers(allNodes, null, 100);

                /**
                 * favor spots near good ports
                 */
                /**
                 * check 3:1 ports
                 */
                List<Integer> miscPortNodes = board.getPortCoordinates(SOCBoard.MISC_PORT);
                bestSpot2AwayFromANodeSet(allNodes, miscPortNodes, 5);

                /**
                 * check out good 2:1 ports
                 */
                final int[] resourceEstis = estimateResourceRarity();
                for (int portType = SOCBoard.CLAY_PORT;
                         portType <= SOCBoard.WOOD_PORT; portType++)
                {
                    /**
                     * if the chances of rolling a number on the resource is better than 1/3,
                     * then it's worth looking at the port
                     */
                    if (resourceEstis[portType] > 33)
                    {
                        List<Integer> portNodes = board.getPortCoordinates(portType);
                        final int portWeight = (resourceEstis[portType] * 10) / 56;
                        bestSpot2AwayFromANodeSet(allNodes, portNodes, portWeight);
                    }
                }

                /*
                 * create a list of potential settlements that takes into account
                 * where other players will build
                 */
                List<Integer> psList = new ArrayList<Integer>();

                psList.addAll(ourPlayerData.getPotentialSettlements());
                // log.debug("- potential settlement at " + Integer.toHexString(j));

                dummy.setPotentialAndLegalSettlements(psList, false, null);

                for (int builds = 0; builds < numberOfBuilds; builds++)
                {
                    BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0, 0);
                    Enumeration<Integer> nodesEnum = allNodes.keys();

                    while (nodesEnum.hasMoreElements())
                    {
                        final Integer nodeCoord = nodesEnum.nextElement();
                        final int score = allNodes.get(nodeCoord).intValue();
                        log.debug("NODE = " + Integer.toHexString(nodeCoord.intValue()) + " SCORE = " + score);

                        if (bestNodePair.getScore() < score)
                        {
                            bestNodePair.setScore(score);
                            bestNodePair.setNode(nodeCoord.intValue());
                        }
                    }

                    /**
                     * pretend that someone has built a settlement on the best spot
                     */
                    dummy.updatePotentials(new SOCSettlement(ourPlayerData, bestNodePair.getNode(), null));

                    /**
                     * remove this spot from the list of best spots
                     */
                    allNodes.remove(Integer.valueOf(bestNodePair.getNode()));
                }
            }
        }

        /**
         * Find the best scoring node
         */
        BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0, 0);
        Enumeration<Integer> cenum = twoAway.keys();

        while (cenum.hasMoreElements())
        {
            final Integer coordInt = cenum.nextElement();
            final int coord = coordInt.intValue();
            final int score = twoAway.get(coordInt).intValue();

            log.debug("Considering " + Integer.toHexString(coord) + " with a score of " + score);

            if (dummy.canPlaceSettlement(coord))
            {
                if (bestNodePair.getScore() < score)
                {
                    bestNodePair.setScore(score);
                    bestNodePair.setNode(coord);
                }
            }
            else
            {
                log.debug("Someone is bound to ruin that spot.");
            }
        }

        // Reminder: settlementNode == ourPlayerData.getLastSettlementCoord()
        plannedRoadDestinationNode = bestNodePair.getNode();  // coordinate of future settlement
                                                         // 2 nodes away from settlementNode
        final int roadEdge   // will be adjacent to settlementNode
            = board.getAdjacentEdgeToNode2Away
              (settlementNode, plannedRoadDestinationNode);

        dummy.destroyPlayer();

        return roadEdge;
    }

    /**
     * Used in {@link RANRobotBrain}
     * to select a random settlement in the initial stage
     * @param getPotentialSettlements_arr the current array of available spots
     * @return A coordinate for intial settlement
     */
    public int randomSettlement()
    {
        final int[] ourPotentialSettlements = ourPlayerData.getPotentialSettlements_arr();
        if (ourPotentialSettlements == null)
            return 0;  // Should not occur
        Random r = new Random();
        int RANSettlement = r.nextInt(ourPotentialSettlements.length);
        final int settlementDecsion = ourPotentialSettlements[RANSettlement];
        return settlementDecsion;
    }

    /**
     * counter that tracks number of settlements placed
     * 
    */
    
    // public int SOCPlacementCounter(){
    //     SOCBoard board = game.getBoard();
    //     final int[] ourPotentialSettlements = ourPlayerData.getPotentialSettlements_arr();

    //     if (ourPotentialSettlements.length == 54){
    //         int turnCounter = 1; 
    //     } 
    //     turnCounter = turnCounter + 1;
    //     return
    // }




    public static <K, V extends Comparable<V> > TreeMap<K, V>
    valueSort(final TreeMap<K, V> map)
    {
        // Static Method with return type Map and
        // extending comparator class which compares values
        // associated with two keys
        Comparator<K> valueComparator = new Comparator<K>()
        {
            
            public int compare(K k1, K k2)
            {
                int comp = map.get(k1).compareTo(map.get(k2));

                if (comp == 0)
                     return 1;

                else
                     return (-comp);
            }
        };
    
        // SortedMap created using the comparator
        TreeMap<K, V> sorted = new TreeMap<K, V>(valueComparator);

        sorted.putAll(map);

        return sorted;
    }
    /**
     * Using a fast algorithm to create an ordered map based on Probtotals
     *    
     * @return sortedMapI <integer coordinate, integer ProbTotal
     */
    
    public TreeMap<Integer, Integer> fastProbabiltySearch()
    {
        log.debug("--- Beam search placement ---");
        // final int width = BSBRobotBrain.width; 
        // final int depth = BSBRobotBrain.depth;

        SOCBoard board = game.getBoard();
        SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        int probTotal;
        // int bestProbTotal;
        final int[] prob = SOCNumberProbabilities.INT_VALUES;


        final int[] ourPotentialSettlements = ourPlayerData.getPotentialSettlements_arr();
        // NavigableMap<String, Integer> ProbabiltyTreeS = new TreeMap<>();
        TreeMap<Integer, Integer> ProbabiltyTreeI = new TreeMap<>();
        for (int i = 0; i < ourPotentialSettlements.length; ++i)
            {
                final int selectionNode = ourPotentialSettlements[i];
                
                playerNumbers.clear();
                probTotal = playerNumbers.updateNumbersAndProbability(selectionNode, board, prob, null);
                // ProbabiltyTreeS.put(board.nodeCoordToString(selectionNode),probTotal);
                ProbabiltyTreeI.put(selectionNode,probTotal);
            } 
        // Map<String, Integer> sortedMap = valueSort(ProbabiltyTreeS);

        // // Get a set of the entries on the sorted map
        // Set<Map.Entry<String, Integer>> set = sortedMap.entrySet();

        // // Get an iterator
        // Iterator<Map.Entry<String, Integer>> i = set.iterator();

        // while (i.hasNext())
        // {
        //     Map.Entry<String, Integer> mp = (Map.Entry<String, Integer>)i.next();

        //     soc.debug.D.ebugPrintlnINFO(mp.getKey() + ": "+ (String.valueOf(mp.getValue())));   
        // }        
        TreeMap<Integer, Integer> sortedMapI = valueSort(ProbabiltyTreeI);
        
        // Get a set of the entries on the sorted map
        Set<Map.Entry<Integer, Integer>> setI = sortedMapI.entrySet();

        // Get an iterator
        Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();

        while (iI.hasNext())
        {
            Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

            soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.valueOf(mpI.getValue())));   
        }
      return sortedMapI;
    }    
    /**
     * Taking w nodes from ordered map
     * Checking against legal spots and taking extra if probabilites match
     * @param width The top w nodes kept at each level    
     * @return subKeyList <integer> coordinates for further exploration 
     */
    
    public List<Integer> nodesForAnalysis(TreeMap<Integer, Integer> sortedMap, List<Integer> plannedSettlementList, int BSBTurn, int currentTurn)
    {
        int width = BSBRobotBrain.width - 1; 
        
        final SOCBoard board = game.getBoard();
        boolean listCheck = false;
        // int listSize[] = new int[branch]; 
        
        
        // SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        List<Integer> keyList = new ArrayList<>(sortedMap.keySet());
        soc.debug.D.ebugPrintlnINFO("keyList size = "+ keyList.size()+ " -- keyList[width] = " + keyList.get(width));
        Map<Integer, Integer> subMap = new TreeMap<>(sortedMap.headMap(keyList.get(width)));
        List<Integer> subKeyList = new ArrayList<>(subMap.keySet());
        while (listCheck = false){
            for (int i = 0; i < subKeyList.size(); i++){
                
                for (int j = 0; j < plannedSettlementList.size(); j++){
                    if (board.isNodeAdjacentToNode(subKeyList.get(i), plannedSettlementList.get(j))){
                        subKeyList.remove(i);
                        break;                        
                    }
                }
            }

            if (subKeyList.size() < width){
                int width_add = width - subKeyList.size();
                TreeMap<Integer ,Integer> addWidthMap = new TreeMap<>(sortedMap.subMap(width + 1, width_add + 1));
                List<Integer> addWidthKeyList = new ArrayList<>(addWidthMap.keySet()); 
                for (int k = 0; k < addWidthKeyList.size(); k++){
                    subKeyList.add(k);
                }
            }
            else {
                listCheck = true;
            }
        }
        return (subKeyList);
    }   

     public List<List<Integer>> nodesForAnalysis3(TreeMap<Integer, Integer> sortedMap, List<List<Integer>> plannedSettlementList){
        
        int width = BSBRobotBrain.width; 
        int branch = BSBRobotBrain.branch;
        int branchNodeSize = plannedSettlementList.size()/branch;
        soc.debug.D.ebugPrintlnINFO("--nodesForAnalysis3-started--");
        final SOCBoard board = game.getBoard();
        boolean foundNode = true;

        
        
        List<List<Integer>> branchNodesList = new ArrayList<>();

        for (int x = 0; x < plannedSettlementList.size(); x++){
            List<Integer> keyList = new ArrayList<>();
            // iterate over full list 
            int y = 0;
            keyList.addAll(sortedMap.keySet());
            
            // soc.debug.D.ebugPrintlnINFO(branchSettlementList.toString());
            for (int i = 0; i < keyList.size(); i++ ){
                if (y == width){
                    break;
                } // if width of list reach complete branch 
                List<Integer> branchSettlementList = new ArrayList<>();
                branchSettlementList.addAll(plannedSettlementList.get(x)); // add a branch to search over still possible spots
                Integer branchSettlementSize = branchSettlementList.size(); // number of nodes in branch 
                

                
                for (int j = 0; j < branchSettlementSize; j++){ 
                    foundNode = true;
                    soc.debug.D.ebugPrintlnINFO(keyList.get(i).toString()+"---"+"---"+branchSettlementList.get(j).toString());
                    if (board.isNodeAdjacentToNode(keyList.get(i), branchSettlementList.get(j)) || keyList.get(i) == branchSettlementList.get(j)){
                        // if new node is too close to any in current branch remove 

                        soc.debug.D.ebugPrintlnINFO("--NodeRemoved--"+(keyList.get(i).toString())+"----"+(i)+"-"+(x)+"-----------");
                        keyList.remove(i) ;
                        
                        foundNode = false;
                        break;                  
                    }
                }

                if (foundNode == true){
                    // else add to list as taking in sorted order 

                    branchSettlementList.add(keyList.get(i));   // add new node to current branch 
                    branchNodesList.add(branchSettlementList);  // add full new branch to final list 
                    soc.debug.D.ebugPrintlnINFO("--BranchNodeList--"+(branchNodesList.get(branchNodesList.size()-1)).toString()+"----"+(i)+"-"+(x)+"-----------");
                    soc.debug.D.ebugPrintlnINFO("--BranchNode--"+(keyList.get(i).toString())+"----"+(i)+"-"+(x)+"-----------");
                    
                    y = y + 1;
                }
            }          
        }

        for (int i = 0; i < branchNodesList.size(); i++ ){
            soc.debug.D.ebugPrintlnINFO(branchNodesList.get(i).toString());
        }
        return branchNodesList;
    }


     public List<List<Integer>> fullNodeAnalysis(TreeMap<Integer, Integer> sortedMap, List<List<Integer>> plannedSettlementList){
        
        int width = BSBRobotBrain.width; 
        int branch = BSBRobotBrain.branch;
        int branchNodeSize = plannedSettlementList.size()/branch;
        soc.debug.D.ebugPrintlnINFO("--FullNodeAnalysis-started--");
        final SOCBoard board = game.getBoard();
        boolean foundNode = true;
        TreeMap<Integer,TreeMap<Integer, Integer>> fullBranchScoreMap = new TreeMap<>();
        TreeMap<Integer,List< Integer>> branchCoords = new TreeMap<>();
        
        List<List<Integer>> branchNodesList = new ArrayList<>();

        for (int x = 0; x < plannedSettlementList.size(); x++){
            List<Integer> keyList = new ArrayList<>();
            // iterate over full list 
            int y = 0;
            keyList.addAll(sortedMap.keySet());
            List<Integer> branchList = new ArrayList<>();
            // soc.debug.D.ebugPrintlnINFO(branchSettlementList.toString());
            for (int i = 0; i < keyList.size(); i++ ){
                List<Integer> branchSettlementList = new ArrayList<>();
                
                branchSettlementList.addAll(plannedSettlementList.get(x)); // add a branch to search over still possible spots
                Integer branchSettlementSize = branchSettlementList.size(); // number of nodes in branch 
                for (int j = 0; j < branchSettlementSize; j++){ 
                    foundNode = true;
                    soc.debug.D.ebugPrintlnINFO(keyList.get(i).toString()+"---"+"---"+branchSettlementList.get(j).toString());
                    if (board.isNodeAdjacentToNode(keyList.get(i), branchSettlementList.get(j)) || keyList.get(i) == branchSettlementList.get(j)){
                        // if new node is too close to any in current branch remove 

                        soc.debug.D.ebugPrintlnINFO("--NodeRemoved--"+(keyList.get(i).toString())+"----"+(i)+"-"+(x)+"-----------");
                        keyList.remove(i) ;
                        
                        foundNode = false;
                        break;                  
                    }
                }

                if (foundNode == true){
                    // else add to list as taken in sorted order 
 
                    branchList.add(keyList.get(i));  // add node to branch list  
                    soc.debug.D.ebugPrintlnINFO("--BranchNode--"+(keyList.get(i).toString())+"----"+(i)+"-"+(x)+"-----------");
                    y = y + 1;       
                }

                if (y == width){
                    TreeMap<Integer, Integer> branchScoreMap = new TreeMap<>();

                    for (int j = 0; j < branchList.size(); j++){
                        int currentBranchScore = realSingleNodeEvaluation(branchList.get(j));
                        branchScoreMap.put(branchList.get(j), currentBranchScore);
                    }

                    TreeMap<Integer, Integer> coordScoreSort = valueSort(branchScoreMap);

                    fullBranchScoreMap.put(x,coordScoreSort);
                    branchCoords.put(x, branchSettlementList);
                            // Get a set of the entries on the sorted map
                    Set<Map.Entry<Integer, Integer>> setI = coordScoreSort.entrySet();

                    // Get an iterator
                    Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();
                    soc.debug.D.ebugPrintlnINFO("---coordscoremap----");
                    while (iI.hasNext())
                    {
                        Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

                        soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.valueOf(mpI.getValue())));   
                    }
                    break;
                } // if width of list reach complete branch 
            }          
        }
        boolean mapped = false;
        while (branchNodesList.size() < branch){
            TreeMap<Integer, Integer> scoreTree = new TreeMap<>();
            
            if (mapped == false){
                for (int x = 0; x <  plannedSettlementList.size(); x++ ){
                    TreeMap<Integer, Integer> innerMap = fullBranchScoreMap.get(x);
                    Set<Map.Entry<Integer, Integer>> setI = innerMap.entrySet();

                    // Get an iterator
                    Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();
                    soc.debug.D.ebugPrintlnINFO("---innermap----");
                    while (iI.hasNext())
                    {
                        Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

                        soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.valueOf(mpI.getValue())));   
                    }
                    
                    // List<Integer> scoreKeyList = new ArrayList<>(innerMap.keySet());
                    // List<Integer> branchKeyList = new ArrayList<>(branchCoords.keySet());
                    Integer mCord = branchCoords.get(x).get(0);
                    Integer oCord = innerMap.firstEntry().getKey();
                    Integer mScore = realSingleNodeEvaluation(mCord);
                    Integer oScore = realSingleNodeEvaluation(oCord);
                    Integer scoreDifference = mScore - oScore;
                    soc.debug.D.ebugPrintlnINFO("my coor = "+ mCord.toString() + ":" +  mScore.toString());
                    soc.debug.D.ebugPrintlnINFO("their coor = "+ oCord.toString() + ":" + oScore.toString());
                    scoreTree.put(x,(scoreDifference));
                    mapped = true;
                }                
            }
            for (int x = 0; x <  plannedSettlementList.size(); x++ ){
                TreeMap<Integer, Integer> innerMap = fullBranchScoreMap.get(x);
                Set<Map.Entry<Integer, Integer>> setI = innerMap.entrySet();

                // Get an iterator
                Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();
                soc.debug.D.ebugPrintlnINFO("---innermap----");
                while (iI.hasNext())
                {
                    Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

                    soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.valueOf(mpI.getValue())));   
                }
                
                // List<Integer> scoreKeyList = new ArrayList<>(innerMap.keySet());
                // List<Integer> branchKeyList = new ArrayList<>(branchCoords.keySet());
                Integer mCord = branchCoords.get(x).get(0);
                Integer oCord = innerMap.firstEntry().getKey();
                Integer mScore = realSingleNodeEvaluation(mCord);
                Integer oScore = realSingleNodeEvaluation(oCord);
                Integer scoreDifference = mScore - oScore;
                soc.debug.D.ebugPrintlnINFO("my coor = "+ mCord.toString() + ":" +  mScore.toString());
                soc.debug.D.ebugPrintlnINFO("their coor = "+ oCord.toString() + ":" + oScore.toString());
                scoreTree.put(x,(scoreDifference));
            }
            TreeMap<Integer, Integer> branchScoreSort = valueSort(scoreTree);
            // List<Integer> keyList = new ArrayList<>(branchScoreSort.keySet());
            Integer bestBranch = branchScoreSort.firstEntry().getKey();
            List<Integer> finalBranchList = new ArrayList<>();
            finalBranchList.addAll(branchCoords.get(bestBranch));
            TreeMap<Integer, Integer> innerMap = new TreeMap<>(fullBranchScoreMap.get(bestBranch));

            Set<Map.Entry<Integer, Integer>> setI = innerMap.entrySet();

            // Get an iterator
            Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();
            soc.debug.D.ebugPrintlnINFO("---out of mapping innermap----");
            while (iI.hasNext())
            {
                Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

                soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.valueOf(mpI.getValue())));   
            }

            Integer oCord = innerMap.firstEntry().getKey();
            soc.debug.D.ebugPrintlnINFO("---best branch---" + bestBranch.toString());
            soc.debug.D.ebugPrintlnINFO("---coord added---" + oCord.toString()+ "coord value" + innerMap.get(oCord));
            finalBranchList.add(oCord);    
            Integer Removed = innerMap.remove(oCord);
            soc.debug.D.ebugPrintlnINFO(Removed.toString());
            branchNodesList.add(finalBranchList);
        }
        
        return branchNodesList;
    }

    public TreeMap<Integer, Integer> singleNodeExploration(List<Integer> nodeList){

        SOCBoard board = game.getBoard();
        SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        final int[] prob = SOCNumberProbabilities.INT_VALUES;
        TreeMap<Integer, Integer> coordScoreMap = new TreeMap<>();

        int[] boardResourceQuantity = estimateResourceRarity(); // 0-Brick, 1-Ore, 2-Sheep, 3-Wheat, 4-Wood 
        TreeMap<Integer,Integer> scarcityScore = new TreeMap<>();
        scarcityScore.put(0,(boardResourceQuantity[0])/2);
        scarcityScore.put(1,(boardResourceQuantity[1])/3);
        scarcityScore.put(2,(boardResourceQuantity[2])/2);
        scarcityScore.put(3,(boardResourceQuantity[3])/4);
        scarcityScore.put(4,(boardResourceQuantity[4])/2);
        
        for (int i = 0; i < nodeList.size();i++){
            int currentNode = nodeList.get(i);
            int probTotal = playerNumbers.updateNumbersAndProbability(currentNode, board, prob, null);
            Integer coordScore = 0;
            coordScore = coordScore + probTotal;

            List<Integer> hexes = board.getAdjacentHexesToNode(currentNode);
            List<Integer> hexType = new ArrayList<>();
            List<Integer> hexNumber = new ArrayList<>();
            Set<Integer> typeSet = new HashSet <Integer>();
            TreeMap<Integer,Pair<Integer,Integer>> fullHexMap = new TreeMap<>();
            int[] resourceScore = {0,0,0,0,0};
            for (int j = 0; j < hexes.size(); j++){
                hexType.add(board.getHexTypeFromCoord(hexes.get(j)));
                hexNumber.add(board.getHexNumFromCoord(hexes.get(j)));
                fullHexMap.put(hexes.get(j), new Pair<Integer,Integer> (hexType.get(j),hexNumber.get(j)));
                typeSet.add(j);
                if (hexType.get(j) == 0){
                    resourceScore[0] = resourceScore[0] + hexNumber.get(j);
                }
                else if (hexType.get(j) == 1){
                    resourceScore[1] = resourceScore[1] + hexNumber.get(j);
                }
                else if (hexType.get(j) == 2){
                    resourceScore[2] = resourceScore[2] + hexNumber.get(j);
                }
                else if (hexType.get(j) == 3){
                    resourceScore[3] = resourceScore[3] + hexNumber.get(j);
                }
                else if (hexType.get(j) == 4){
                    resourceScore[4] = resourceScore[4] + hexNumber.get(j);
                }
            }
            coordScore = coordScore + 3*typeSet.size();
            for (int j = 0; j < hexes.size(); j++){
                if (scarcityScore.get(j)/2.5 < (int)Array.get(resourceScore ,j)){
                    coordScore = coordScore + 6;
                }
            coordScoreMap.put(nodeList.get(i), coordScore);
            }
        
        } 
        TreeMap<Integer, Integer> coordScoreSort = valueSort(coordScoreMap);
        return coordScoreSort;
    }

    

        public  Integer realSingleNodeEvaluation(Integer node){

        SOCBoard board = game.getBoard();
        SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        final int[] prob = SOCNumberProbabilities.INT_VALUES;
        // TreeMap<Integer, Integer> coordScoreMap = new TreeMap<>();



        int[] boardResourceQuantity = estimateResourceRarity(); // 0-Brick, 1-Ore, 2-Sheep, 3-Wheat, 4-Wood 
        TreeMap<Integer,Integer> scarcityScore = new TreeMap<>();
        scarcityScore.put(0,(boardResourceQuantity[0])/2);
        scarcityScore.put(1,(boardResourceQuantity[1])/3);
        scarcityScore.put(2,(boardResourceQuantity[2])/2);
        scarcityScore.put(3,(boardResourceQuantity[3])/4);
        scarcityScore.put(4,(boardResourceQuantity[4])/2);
        
    
        int probTotal = playerNumbers.updateNumbersAndProbability(node, board, prob, null);
        Integer coordScore = 0;
        coordScore = coordScore + probTotal;


        List<Integer> hexes = board.getAdjacentHexesToNode(node);
        List<Integer> hexType = new ArrayList<>();
        List<Integer> hexNumber = new ArrayList<>();
        Set<Integer> typeSet = new HashSet <Integer>();
        TreeMap<Integer,Pair<Integer,Integer>> fullHexMap = new TreeMap<>();
        int[] resourceScore = {0,0,0,0,0};
        for (int j = 0; j < hexes.size(); j++){
            hexType.add(board.getHexTypeFromCoord(hexes.get(j)));
            hexNumber.add(board.getHexNumFromCoord(hexes.get(j)));
            fullHexMap.put(hexes.get(j), new Pair<Integer,Integer> (hexType.get(j),hexNumber.get(j)));
            typeSet.add(j);
            if (hexType.get(j) == 0){
                resourceScore[0] = resourceScore[0] + hexNumber.get(j);
            }
            else if (hexType.get(j) == 1){
                resourceScore[1] = resourceScore[1] + hexNumber.get(j);
            }
            else if (hexType.get(j) == 2){
                resourceScore[2] = resourceScore[2] + hexNumber.get(j);
            }
            else if (hexType.get(j) == 3){
                resourceScore[3] = resourceScore[3] + hexNumber.get(j);
            }
            else if (hexType.get(j) == 4){
                resourceScore[4] = resourceScore[4] + hexNumber.get(j);
            }
        }
        coordScore = coordScore + 3*typeSet.size();
        for (int j = 0; j < hexes.size(); j++){
            if (scarcityScore.get(j)/2.5 < (int)Array.get(resourceScore ,j)){
                coordScore = coordScore + 6;
            }
        }

        Pair<Integer,Integer> coordScorePair = new Pair<Integer,Integer>(node, coordScore);
        return coordScore;
    }


    public List<List<Integer>> multiNodeScoreFunction (List<List<Integer>> branchNodes, int BSBturn){

        int branch = BSBRobotBrain.branch - 1;

        SOCBoard board = game.getBoard();
        SOCPlayerNumbers playerNumbers = new SOCPlayerNumbers(board);
        final int[] prob = SOCNumberProbabilities.INT_VALUES;
        TreeMap<Integer,Integer> BSBVersusScore = new TreeMap<Integer,Integer>();
        TreeMap<Integer,List<Integer>> BSBVersusBranch = new TreeMap<Integer,List<Integer>>();
        Integer opponentBranchScore = 0;
        int BSBNode = 0;
        int BSBNodeScore = 0;

        for (Integer i = 0; i < branchNodes.size(); i++){
            List<Integer> currentBranch = new ArrayList<>();
            Integer currentBranchValue = 0; 
            currentBranch.addAll(branchNodes.get(i));
            for (int j = 0; j < currentBranch.size(); j++){
                currentBranchValue = realSingleNodeEvaluation(currentBranch.get(j));
                if (j + 1 == BSBturn){
                    BSBNode = currentBranch.get(j);
                    BSBNodeScore = currentBranchValue;
                }
                if (j == currentBranch.size()){
                    opponentBranchScore = (currentBranchValue);
                }
            }

            

            
            BSBNodeScore = BSBNodeScore - opponentBranchScore;
            BSBVersusScore.put(i, BSBNodeScore);
            BSBVersusBranch.put(i, currentBranch);
            soc.debug.D.ebugPrintlnINFO(i.toString());
            soc.debug.D.ebugPrintlnINFO(currentBranch.toString());
        }
        TreeMap<Integer,Integer> BSBFinalScoreMap = valueSort(BSBVersusScore);

        List<Integer> keyList = new ArrayList<>(BSBFinalScoreMap.keySet());
        // soc.debug.D.ebugPrintlnINFO("keyList size = "+ keyList.size()+ " -- keyList[width] = " + keyList.get(width));




        Map<Integer, Integer> subMap = new TreeMap<>(BSBFinalScoreMap.headMap(keyList.get(branch)));
        List<Integer> subKeyList = new ArrayList<>(subMap.keySet());

        List<List<Integer>> BSBFinalBranch = new ArrayList<List<Integer>>();

        for (int i = 0; i < subKeyList.size(); i ++){
            BSBFinalBranch.add(BSBVersusBranch.get(subKeyList.get(i)));
        }


        // Get a set of the entries on the sorted map
        Set<Map.Entry<Integer, Integer>> setI = BSBFinalScoreMap.entrySet();

        // Get an iterator
        Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();

        while (iI.hasNext())
        {
            Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

            soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.valueOf(mpI.getValue()))); 

        }
        // Integer maxKey = keyList.get(0);
        // soc.debug.D.ebugPrintlnINFO(maxKey.toString());

        soc.debug.D.ebugPrintlnINFO(BSBFinalBranch.toString());

        return BSBFinalBranch;
    }

    public List<List <Integer>> branchSelector(TreeMap<Integer, Integer> sortedTreeMap){
        int branch = BSBRobotBrain.branch; 
        List<Integer> keyList = new ArrayList<>(sortedTreeMap.keySet());
        Map<Integer, Integer> subMap = new TreeMap<>(sortedTreeMap.headMap(keyList.get(branch)));
        List<Integer> subKeyList = new ArrayList<>(subMap.keySet()); 


        List<List<Integer>> subKeyListList = new ArrayList<>(); 
        for (int i = 0; i < branch; i++){ 
            List<Integer> innerList = new ArrayList<>();
            innerList.add((subKeyList.get(i)));
            
            subKeyListList.add(innerList);
        }

        return subKeyListList;
        
    }


    public int BeamSearchFull(int BSBTurn){
        int BBTurn = BSBTurn;
        final SOCBoard board = game.getBoard();
        List<List<Integer>> emptyList = new ArrayList<>(); 
        TreeMap<Integer,Integer> sortedBoardMap = fastProbabiltySearch();

        List<Integer> topNodesList = nodesForAnalysis(sortedBoardMap, null, BSBTurn, BBTurn);
        // List<List<Integer>> topNodesList2 = nodesForAnalysis3(sortedBoardMap, emptyList );
        // for (int x = 0; x < topNodesList.size(); x++ ){
        //     soc.debug.D.ebugPrintlnINFO(board.nodeCoordToString(topNodesList.get(x)));
        // }
        
        TreeMap<Integer, Integer> SortedNodeScoreMap = singleNodeExploration(topNodesList);

       
        
        // Get a set of the entries on the sorted map
        // Set<Map.Entry<Integer, Integer>> setI = SortedNodeScoreMap.entrySet();

        // // Get an iterator
        // Iterator<Map.Entry<Integer, Integer>> iI = setI.iterator();
        // while (iI.hasNext())
        // {
        //     Map.Entry<Integer, Integer> mpI = (Map.Entry<Integer, Integer>)iI.next();

        //     soc.debug.D.ebugPrintlnINFO(mpI.getKey() + ": "+ (String.
        BBTurn = BSBTurn + 1;

        List<List<Integer>> nodesForBranch = branchSelector(SortedNodeScoreMap);

        List<List<Integer>> opponentResponseLists = nodesForAnalysis3(sortedBoardMap, nodesForBranch);

        List<List<Integer>> newBranchList = fullNodeAnalysis(sortedBoardMap, nodesForBranch);

        soc.debug.D.ebugPrintlnINFO("new branch list"+ newBranchList.toString());
        
        // multiNodeScoreFunction(opponentResponseLists, 1);

        
        return BBTurn;
    }

    /**
     * Given a set of nodes, run a bunch of metrics across them
     * to find which one is best for building a
     * settlement.
     *
     * @param nodes          a hashtable of nodes; the scores in the table will be modified.
     *                            Key = coord Integer; value = score Integer.
     * @param numberWeight   the weight given to nodes on good numbers
     * @param miscPortWeight the weight given to nodes on 3:1 ports
     * @param portWeight     the weight given to nodes on good 2:1 ports
     */
    protected void scoreNodesForSettlements
        (final Hashtable<Integer,Integer> nodes, final int numberWeight, final int miscPortWeight, final int portWeight)
    {
        /**
         * favor spots with the most high numbers
         */
        bestSpotForNumbers(nodes, ourPlayerData, numberWeight);

        /**
         * Favor spots on good ports:
         */
        /**
         * check if this is on a 3:1 ports, only if we don't have one
         */
        if (! ourPlayerData.getPortFlag(SOCBoard.MISC_PORT))
        {
            List<Integer> miscPortNodes = game.getBoard().getPortCoordinates(SOCBoard.MISC_PORT);
            bestSpotInANodeSet(nodes, miscPortNodes, miscPortWeight);
        }

        /**
         * check out good 2:1 ports that we don't have
         */
        final int[] resourceEstis = estimateResourceRarity();

        for (int portType = SOCBoard.CLAY_PORT; portType <= SOCBoard.WOOD_PORT;
                portType++)
        {
            /**
             * if the chances of rolling a number on the resource is better than 1/3,
             * then it's worth looking at the port
             */
            if ((resourceEstis[portType] > 33) && (! ourPlayerData.getPortFlag(portType)))
            {
                List<Integer> portNodes = game.getBoard().getPortCoordinates(portType);
                int estimatedPortWeight = (resourceEstis[portType] * portWeight) / 56;
                bestSpotInANodeSet(nodes, portNodes, estimatedPortWeight);
            }
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  A list of nodes that
     * we want to be on is also taken as an argument.
     * Here are the rules for scoring:
     * If a node is in the desired set of nodes it gets 100.
     * Otherwise it gets 0.
     *
     * @param nodesIn   the table of nodes to evaluate: Hashtable&lt;Integer,Integer&gt .
     *                    Contents will be modified by the scoring.
     * @param goodNodes the set of desired nodes
     * @param weight    the score multiplier
     */
    protected void bestSpotInANodeSet
        (final Hashtable<Integer,Integer> nodesIn, final List<Integer> goodNodes, final int weight)
    {
        Enumeration<Integer> nodesInEnum = nodesIn.keys();

        while (nodesInEnum.hasMoreElements())
        {
            final Integer nodeCoord = nodesInEnum.nextElement();
            final int node = nodeCoord.intValue();
            int score = 0;
            final int oldScore = nodesIn.get(nodeCoord).intValue();

            for (final int goodNode : goodNodes)
            {
                if (node == goodNode)
                {
                    score = 100;
                    break;
                }
            }

            /**
             * multiply by weight
             */
            score *= weight;

            nodesIn.put(nodeCoord, Integer.valueOf(oldScore + score));

            //log.debug("BSIANS -- put node "+Integer.toHexString(node)+" with old score "+oldScore+" + new score "+score);
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  A list of nodes that
     * we want to be near is also taken as an argument.
     * Here are the rules for scoring:
     * If a node is two away from a node in the desired set of nodes it gets 100.
     * Otherwise it gets 0.
     *
     * @param nodesIn   the table of nodes to evaluate: Hashtable&lt;Integer,Integer&gt; .
     *                     Contents will be modified by the scoring.
     * @param goodNodes the set of desired nodes
     * @param weight    the score multiplier
     */
    protected void bestSpot2AwayFromANodeSet
        (final Hashtable<Integer,Integer> nodesIn, final List<Integer> goodNodes, final int weight)
    {
        final SOCBoard board = game.getBoard();

        Enumeration<Integer> nodesInEnum = nodesIn.keys();

        while (nodesInEnum.hasMoreElements())
        {
            final Integer nodeCoord = nodesInEnum.nextElement();
            final int node = nodeCoord.intValue();
            int score = 0;
            final int oldScore = nodesIn.get(nodeCoord).intValue();

            for (final int goodNodeTarget : goodNodes)
            {
                if (node == goodNodeTarget)
                {
                    break;
                }
                else if (board.isNode2AwayFromNode(node, goodNodeTarget))
                {
                    score = 100;
                }
            }

            /**
             * multiply by weight
             */
            score *= weight;

            nodesIn.put(nodeCoord, Integer.valueOf(oldScore + score));

            //log.debug("BS2AFANS -- put node "+Integer.toHexString(node)+" with old score "+oldScore+" + new score "+score);
        }
    }

    /**
     * Estimate the rarity of each resource, given this board's resource locations vs dice numbers.
     * Useful for initial settlement placement and free-resource choice (when no other info available).
     * This is based on the board and doesn't change when pieces are placed.
     * Cached after the first call, as {@link #resourceEstimates}.
     *<P>
     * Calls each hex's {@link SOCBoard#getHexTypeFromCoord(int)}, ignores all hex types besides
     * the usual {@link SOCBoard#CLAY_HEX} through {@link SOCBoard#WOOD_HEX} and {@link SOCBoardLarge#GOLD_HEX}.
     *
     * @return an array of rarity numbers, where
     *         estimates[SOCBoard.CLAY_HEX] == the clay rarity,
     *         as an integer percentage 0-100 of dice rolls.
     */
    public int[] estimateResourceRarity()
    {
        if (resourceEstimates == null)
        {
            final SOCBoard board = game.getBoard();
            final int[] numberWeights = SOCNumberProbabilities.INT_VALUES;

            resourceEstimates = new int[SOCResourceConstants.UNKNOWN];  // uses 1 to 5 (CLAY to WOOD)
            resourceEstimates[0] = 0;

            // look at each hex
            final int bef = board.getBoardEncodingFormat();
            if (bef == SOCBoard.BOARD_ENCODING_6PLAYER ||
                bef == SOCBoard.BOARD_ENCODING_ORIGINAL)
            {
                // v1 or v2 encoding
                final int L = board.getNumberLayout().length;
                for (int i = 0; i < L; i++)
                {
                    final int hexNumber = board.getNumberOnHexFromNumber(i);
                    if (hexNumber > 0)
                        resourceEstimates[board.getHexTypeFromNumber(i)] += numberWeights[hexNumber];
                }
            } else {
                // v3 encoding
                final int[] hcoord = board.getLandHexCoords();
                if (hcoord != null)
                {
                    final int L = hcoord.length;
                    for (int i = 0; i < L; i++)
                    {
                        final int hexNumber = board.getNumberOnHexFromCoord(hcoord[i]);
                        if (hexNumber == 0)
                            continue;

                        final int htype = board.getHexTypeFromCoord(hcoord[i]);
                        if (htype == SOCBoardLarge.GOLD_HEX)
                        {
                            // Count gold as all resource types
                            for (int ht = SOCBoard.CLAY_HEX; ht <= SOCBoard.WOOD_HEX; ++ht)
                                resourceEstimates[ht] += numberWeights[hexNumber];
                        }
                        else if ((htype >= 0) && (htype <= SOCBoard.WOOD_HEX))
                        {
                            resourceEstimates[htype] += numberWeights[hexNumber];
                        }
                    }
                }
            }
        }

        //D.ebugPrint("Resource Estimates = ");
        //for (int i = 1; i < 6; i++)
        //{
            //D.ebugPrint(i+":"+resourceEstimates[i]+" ");
        //}

        //log.debug();

        return resourceEstimates;
    }

    /**
     * Calculate the number of builds before our next turn during init placement.
     *
     */
    protected int numberOfEnemyBuilds()
    {
        int numberOfBuilds = 0;
        int pNum = game.getCurrentPlayerNumber();

        /**
         * This is the clockwise direction
         */
        if ((game.getGameState() == SOCGame.START1A) || (game.getGameState() == SOCGame.START1B))
        {
            do
            {
                /**
                 * look at the next player
                 */
                pNum++;

                if (pNum >= game.maxPlayers)
                {
                    pNum = 0;
                }

                if ((pNum != game.getFirstPlayer()) && ! game.isSeatVacant (pNum))
                {
                    numberOfBuilds++;
                }
            }
            while (pNum != game.getFirstPlayer());
        }

        /**
         * This is the counter-clockwise direction
         */
        do
        {
            /**
             * look at the next player
             */
            pNum--;

            if (pNum < 0)
            {
                pNum = game.maxPlayers - 1;
            }

            if ((pNum != game.getCurrentPlayerNumber()) && ! game.isSeatVacant (pNum))
            {
                numberOfBuilds++;
            }
        }
        while (pNum != game.getCurrentPlayerNumber());

        return numberOfBuilds;
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  Nodes touching hexes
     * with better numbers get better scores.  Also numbers
     * that the player isn't touching yet are better than ones
     * that the player is already touching.
     *
     * @param nodes    the "table" of nodes to add scores to: key = a node on land; value = its Integer score.
     *                   Score values will be updated here.
     * @param player   the player that we are doing the rating for, or <tt>null</tt>;
     *                   will give a bonus to numbers the player isn't already touching
     * @param weight   a number that is multiplied by the score
     */
    protected void bestSpotForNumbers(Hashtable<Integer,Integer> nodes, SOCPlayer player, int weight)
    {
        final int[] numRating = SOCNumberProbabilities.INT_VALUES;
        final SOCPlayerNumbers playerNumbers = (player != null) ? player.getNumbers() : null;
        final SOCBoard board = game.getBoard();

        // 80 is highest practical score (40 if player == null)
        final int maxScore = (player != null) ? 80 : 40;

        int oldScore;
        Enumeration<Integer> nodesEnum = nodes.keys();

        while (nodesEnum.hasMoreElements())
        {
            final Integer nodeInt = nodesEnum.nextElement();

            //log.debug("BSN - looking at node "+Integer.toHexString(node.intValue()));
            oldScore = nodes.get(nodeInt).intValue();

            int score = 0;

            for (final int hex : board.getAdjacentHexesToNode(nodeInt.intValue()))
            {
                final int number = board.getNumberOnHexFromCoord(hex);
                score += numRating[number];

                if ((number != 0) && (playerNumbers != null) && ! playerNumbers.hasNumber(number))
                {
                    /**
                     * add a bonus for numbers that the player doesn't already have
                     */

                    //log.debug("ADDING BONUS FOR NOT HAVING "+number);
                    score += numRating[number];
                }

                //log.debug(" -- -- Adding "+numRating[board.getNumberOnHexFromCoord(hex)]);
            }

            /*
             * normalize score and multiply by weight
             * 80 is highest practical score (40 if player == null)
             * lowest score is 0
             */
            final int nScore = ((score * 100) / maxScore) * weight;
            final Integer finalScore = Integer.valueOf(nScore + oldScore);
            nodes.put(nodeInt, finalScore);

            //log.debug("BSN -- put node "+Integer.toHexString(node.intValue())+" with old score "+oldScore+" + new score "+nScore);
        }
    }

}
