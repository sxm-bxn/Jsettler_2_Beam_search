/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * Portions of this file Copyright (C) 2007-2025 Jeremy D Monin <jeremy@nand.net>
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
package soc.message;

/**
 * This message from the client syncs the 4 extra numeric values from client game to server game.
 * Used by robots and test code to send calculated values (like settlement scores) to the server
 * so they can be persisted in the database.
 *<P>
 * Format: EXTRANUM sep gameName sep num1 sep num2 sep num3 sep num4
 *
 * @since 2.7.00
 */
public class SOCExtraNum extends SOCMessage
    implements SOCMessageForGame
{
    private static final long serialVersionUID = 2700L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The 4 extra numeric values
     */
    private int[] nums;

    /**
     * Create an ExtraNum message.
     *
     * @param ga  the name of the game
     * @param n1  extra number 1
     * @param n2  extra number 2
     * @param n3  extra number 3
     * @param n4  extra number 4
     */
    public SOCExtraNum(String ga, int n1, int n2, int n3, int n4)
    {
        messageType = EXTRANUM;
        game = ga;
        nums = new int[] {n1, n2, n3, n4};
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the 4 extra numbers as an array
     */
    public int[] getNums()
    {
        return nums;
    }

    /**
     * EXTRANUM sep gameName sep num1 sep num2 sep num3 sep num4
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, nums[0], nums[1], nums[2], nums[3]);
    }

    /**
     * EXTRANUM sep gameName sep num1 sep num2 sep num3 sep num4
     *
     * @param ga  the name of the game
     * @param n1  extra number 1
     * @param n2  extra number 2
     * @param n3  extra number 3
     * @param n4  extra number 4
     * @return the command string
     */
    public static String toCmd(String ga, int n1, int n2, int n3, int n4)
    {
        return "EXTRANUM" + sep + ga + sep + n1 + sep + n2 + sep + n3 + sep + n4;
    }

    /**
     * Parse the command String into an ExtraNum message
     *
     * @param s   the String to parse, format: gameName sep num1 sep num2 sep num3 sep num4
     * @return    an ExtraNum message, or null if the data is garbled
     */
    public static SOCExtraNum parseDataStr(String s)
    {
        String ga;
        int n1, n2, n3, n4;

        try
        {
            String[] sa = s.split(sep);
            ga = sa[0];
            n1 = Integer.parseInt(sa[1]);
            n2 = Integer.parseInt(sa[2]);
            n3 = Integer.parseInt(sa[3]);
            n4 = Integer.parseInt(sa[4]);
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCExtraNum(ga, n1, n2, n3, n4);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "EXTRANUM:game=" + game + "|nums=[" + nums[0] + "," + nums[1] + "," + nums[2] + "," + nums[3] + "]";
    }
}
