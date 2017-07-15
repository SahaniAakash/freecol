/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.LastSale;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when to add or remove a feature.
 */
public class FeatureChangeMessage extends ObjectMessage {

    public static final String TAG = "featureChange";
    private static final String ADD_TAG = "add";
    private static final String ID_TAG = FreeColObject.ID_ATTRIBUTE_TAG;


    /**
     * Create a new {@code FeatureChangeMessage} for the game object
     * and feature.
     *
     * @param fcgo The parent {@code FreeColGameObject} to manipulate.
     * @param fco The {@code FreeColObject} to add or remove.
     * @param add If true the object is added.
     */
    public FeatureChangeMessage(FreeColGameObject fcgo, FreeColObject fco,
                                boolean add) {
        super(TAG, ID_TAG, fcgo.getId(),
              ADD_TAG, String.valueOf(add));

        add1(fco);
    }

    /**
     * Create a new {@code FeatureChangeMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public FeatureChangeMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, ID_TAG, ADD_TAG);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                FreeColObject fco = null;
                if (Ability.TAG.equals(tag)) {
                    fco = xr.readFreeColObject(game, Ability.class);
                } else if (Modifier.TAG.equals(tag)) {
                    fco = xr.readFreeColObject(game, Modifier.class);
                } else if (HistoryEvent.TAG.equals(tag)) {
                    fco = xr.readFreeColObject(game, HistoryEvent.class);
                } else if (LastSale.TAG.equals(tag)) {
                    fco = xr.readFreeColObject(game, LastSale.class);
                } else if (ModelMessage.TAG.equals(tag)) {
                    fco = xr.readFreeColObject(game, ModelMessage.class);
                } else {
                    expected("Feature", tag);
                }
                add1(fco);
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
    }


    /**
     * Get the parent object to add/remove to.
     *
     * @param game The {@code Game} to look in.
     * @return The parent {@code FreeColGameObject}.
     */
    private FreeColGameObject getParent(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ID_TAG));
    }

    /**
     * Get the add/remove state.
     *
     * @return True if the child object should be added to the parent.
     */
    private boolean getAdd() {
        return getBooleanAttribute(ADD_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.OWNED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final FreeColGameObject parent = getParent(game);
        final List<FreeColObject> children = getChildren();
        final boolean add = getAdd();

        if (parent == null) {
            logger.warning("featureChange with null parent.");
            return;
        }
        if (children.isEmpty()) {
            logger.warning("featureChange with no children.");
            return;
        }

        igc(freeColClient).featureChangeHandler(parent, children, add);
    }
}
