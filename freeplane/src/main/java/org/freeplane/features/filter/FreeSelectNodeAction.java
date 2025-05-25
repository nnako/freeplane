/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.filter;

import java.awt.event.ActionEvent;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.map.MapView;

public class FreeSelectNodeAction extends AFreeplaneAction {
    private static final long serialVersionUID = 1L;

    public enum Direction {
        LEFT {
            @Override
            void select(MapView mapView) {
                mapView.selectLeft(false);
            }
        },
        CONTINIOUS_LEFT {
            @Override
            void select(MapView mapView) {
                mapView.selectLeft(true);
            }
        },
        UP {
            @Override
            void select(MapView mapView) {
                mapView.selectUp(false);
            }
        },
        CONTINIOUS_UP {
            @Override
            void select(MapView mapView) {
                mapView.selectUp(true);
            }
        },
        RIGHT {
            @Override
            void select(MapView mapView) {
                mapView.selectRight(false);
            }
        },
        CONTINIOUS_RIGHT {
            @Override
            void select(MapView mapView) {
                mapView.selectRight(true);
            }
        },
        DOWN {
            @Override
            void select(MapView mapView) {
                mapView.selectDown(false);
            }
        },
        CONTINIOUS_DOWN {
            @Override
            void select(MapView mapView) {
                mapView.selectDown(true);
            }
        };

        abstract void select(MapView mapView);
    }

    private final Direction direction;

    public FreeSelectNodeAction(final Direction direction) {
        super("FreeSelectNodeAction." + direction.name());
        this.direction = direction;
    }

    public void actionPerformed(final ActionEvent e) {
        final MapView mapView = (MapView) Controller.getCurrentController().getMapViewManager().getMapViewComponent();
        direction.select(mapView);
    }
}
