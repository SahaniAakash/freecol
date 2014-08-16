/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;


/**
 * Mission for idling in a settlement.
 */
public class IdleAtSettlementMission extends Mission {

    private static final Logger logger = Logger.getLogger(IdleAtSettlementMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI idler";


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public IdleAtSettlementMission(AIMain aiMain, AIUnit aiUnit,
                                   LogBuilder lb) {
        super(aiMain, aiUnit, null, lb);
    }

    /**
     * Creates a new <code>IdleAtSettlementMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public IdleAtSettlementMission(AIMain aiMain, AIUnit aiUnit,
                                   FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
        uninitialized = getAIUnit() == null;
    }


    // Implement Mission
    //   Inherit dispose, getTransportDestination

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseTransportPriority() {
        return MINIMUM_TRANSPORT_PRIORITY;
    }

    /**
     * {@inheritDoc}
     */
    public Location getTarget() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setTarget(Location target) {}

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        final Unit unit = getAIUnit().getUnit();
        if (unit.isInEurope()) return unit.getLocation();

        PathNode path = unit.findOurNearestOtherSettlement();
        return (path == null) ? null : upLoc(path.getLastNode().getLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOneTime() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        return invalidAIUnitReason(getAIUnit());
    }

    /**
     * {@inheritDoc}
     */
    public Mission doMission(LogBuilder lb) {
        lb.add(tag);
        String reason = invalidReason();
        if (reason != null) {
            lbBroken(lb, reason);
            return null;
        }

        // Wait if not on the map.
        final Unit unit = getUnit();
        if (!unit.hasTile()) {
            lbAt(lb, unit);
            return this;
        }

        // If our tile contains a settlement, idle.  No log, this is normal.
        Settlement settlement = unit.getTile().getSettlement();
        if (settlement != null) {
            lb.add(", idling at ", settlement, ".");
            return this;
        }

        Location target = findTarget();
        if (target != null) {
            Unit.MoveType mt = travelToTarget(target, null, lb);
            switch (mt) {
            case MOVE:
                break;
            case MOVE_ILLEGAL:
            case MOVE_NO_MOVES: case MOVE_NO_REPAIR: case MOVE_NO_TILE:
                return this;
            default:
                lbMove(lb, unit, mt);
                return this;
            }

        } else { // Just make a random moves if no target can be found.
            moveRandomlyTurn(tag);
            lbAt(lb, unit);
        }
        return this;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "idleAtSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "idleAtSettlementMission";
    }
}
