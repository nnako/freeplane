/*
 * Created on 11 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.core.ui;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;

public class AntiAliasingConfigurator {
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private long lastRenderTime;
    private Timer repaintTimer;
    private final int repaintDelay;
    private boolean isRepaintScheduled;
    private boolean isRepaintInProgress;
    private final JComponent component;
    private Rectangle repaintedClipBounds;
    private Rectangle lastPaintedClipBounds;
    private Dimension lastPaintedComponentSize;
    private static boolean isAntialiasingEnabled = true;
    private static Object hintAntialiasCurves = RenderingHints.VALUE_ANTIALIAS_ON;
    private static Object hintAntialiasText = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
    private static Thread currentPaintingThread = null;

    static {
        ResourceController.getResourceController().addPropertyChangeListenerAndPropagate(new IFreeplanePropertyListener() {

            @Override
            public void propertyChanged(String propertyName, String newValue, String oldValue) {
                if (propertyName.equals("antialias")) {
                    changeAntialias(newValue);
                }
            }
        });
    }
    private static void changeAntialias(String antialiasOption) {
        if (antialiasOption.equals("antialias_none")) {
            isAntialiasingEnabled = false;
            hintAntialiasCurves = RenderingHints.VALUE_ANTIALIAS_OFF;
            hintAntialiasText = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        } else {
            isAntialiasingEnabled = true;
            if (antialiasOption.equals("antialias_edges")) {
                hintAntialiasCurves = RenderingHints.VALUE_ANTIALIAS_ON;
                hintAntialiasText = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
            }
            else if (antialiasOption.equals("antialias_all")) {
                hintAntialiasCurves = RenderingHints.VALUE_ANTIALIAS_ON;
                hintAntialiasText = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
            }
        }
    }
    private static void disableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    private static void enableAntialiasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public static void setAntialiasing(Graphics2D g2) {
        if(! isManagedPaintingInProgress()) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hintAntialiasCurves);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, hintAntialiasText);
        }
    }

    private static boolean isManagedPaintingInProgress() {
        return currentPaintingThread != null && currentPaintingThread == Thread.currentThread();
    }

    public AntiAliasingConfigurator(JComponent component) {
        this(component, MILLISECONDS_PER_SECOND/25);
    }
    public AntiAliasingConfigurator(JComponent component, int repaintDelay) {
        this.component = component;
        this.repaintDelay = repaintDelay;
        isRepaintInProgress = isRepaintScheduled = false;
        lastRenderTime = 0;
    }

    public void withAntialias(Graphics2D g2, Runnable painter) {
        if(isManagedPaintingInProgress()) {
            painter.run();
            return;
        }
        if(! managesPaint(g2)) {
            enableAntialiasing(g2);
            painter.run();
            return;
        }
        if(!isAntialiasingEnabled) {
            disableAntialiasing(g2);
            painter.run();
            return;
        }
        Rectangle newClipBounds = g2.getClipBounds();
        Dimension newComponentSize = component.getSize();
        if ((timeSinceLastRendering() < repaintDelay || ! isRepaintInProgress)
                && ! newClipBounds.equals(lastPaintedClipBounds)
                && newComponentSize.equals(lastPaintedComponentSize)
                ) {
            repaintedClipBounds = repaintedClipBounds == null ? newClipBounds : repaintedClipBounds.union(newClipBounds);
            isRepaintScheduled = true;
            isRepaintInProgress = false;
            lastPaintedClipBounds = null;
            lastPaintedComponentSize = null;
            SwingUtilities.invokeLater(this::restartRepaintTimer);
            disableAntialiasing(g2);
        } else {
            repaintedClipBounds = null;
            isRepaintScheduled = isRepaintInProgress = false;
            lastPaintedClipBounds = newClipBounds;
            lastPaintedComponentSize = newComponentSize;
            stopRepaintTimer();
            setAntialiasing(g2);
        }
        try {
            startManagedPainting();
            painter.run();
        }
        finally {
            endManagedPainting();
            lastRenderTime = System.currentTimeMillis();
        }

    }

    private void startManagedPainting() {
        currentPaintingThread = Thread.currentThread();
    }
    private void endManagedPainting() {
        currentPaintingThread = null;
    }

    private long timeSinceLastRendering() {
        return System.currentTimeMillis() - lastRenderTime;
    }

    private void restartRepaintTimer() {
        if (repaintedClipBounds == null) {
            return;
        }

        if (repaintTimer != null && repaintTimer.isRunning()) {
            repaintTimer.restart();
        } else {
            repaintTimer = new Timer(repaintDelay, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(isRepaintScheduled && timeSinceLastRendering() >= repaintDelay) {
                        isRepaintInProgress = true;
                        component.repaint(repaintedClipBounds);
                    }
                }
            });
            repaintTimer.setRepeats(false);
            repaintTimer.start();
        }
    }

    private void stopRepaintTimer() {
        if (repaintTimer != null && repaintTimer.isRunning()) {
            repaintTimer.stop();
        }
    }
    private boolean managesPaint(Graphics2D g2) {
        if(component.isPaintingForPrint() || ! EventQueue.isDispatchThread()) {
            return false;
        }
        return true;
    }
}
