/*
 * Created on 11 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class AntiAliasingConfigurator {
    private static final int MILLISECONDS_PER_SECOND = 1000;
	private long lastRenderTime = 0;
	private Timer repaintTimer;
    private final int repaintDelay;
    private final int clipAreaThreshold;
    private boolean isRepaintScheduled;
    private boolean isRepaintInProgress;
	private final JComponent component;
	private Rectangle clipBounds;

    public AntiAliasingConfigurator(JComponent component) {
    	this(component, 100*100, MILLISECONDS_PER_SECOND/25);
    }
    public AntiAliasingConfigurator(JComponent component, int clipAreaThreshold, int repaintDelay) {
        this.component = component;
		this.repaintDelay = repaintDelay;
        this.clipAreaThreshold = clipAreaThreshold;
        isRepaintInProgress = isRepaintScheduled = false;
    }

    public void prepareForPaint(Graphics2D g2) {
    	if(! managesPaint(g2)) {
    		enableAntialias(g2);
    	}
    	else {
            if (timeSinceLastRendering() < repaintDelay || ! isRepaintInProgress) {
            	Rectangle newClipBounds = g2.getClipBounds();
            	clipBounds = clipBounds == null ? newClipBounds : clipBounds.union(newClipBounds);
            	isRepaintScheduled = true;
            	isRepaintInProgress = false;
            	SwingUtilities.invokeLater(this::restartRepaintTimer);
                disableAntialias(g2);
            } else {
            	stopRepaintTimer();
            	clipBounds = null;
            	isRepaintScheduled = isRepaintInProgress = false;
            	enableAntialias(g2);
            }

            lastRenderTime = System.currentTimeMillis();
    	}
    }
	private long timeSinceLastRendering() {
		return System.currentTimeMillis() - lastRenderTime;
	}
	public void disableAntialias(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
	}
	public void enableAntialias(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

    private void restartRepaintTimer() {
        if (clipBounds == null) {
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
						component.repaint(clipBounds);
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
	public void endPaint(Graphics2D g2) {
    	if(managesPaint(g2))
    		lastRenderTime = System.currentTimeMillis();
	}
	private boolean managesPaint(Graphics2D g2) {
    	if(component.isPaintingForPrint() || ! EventQueue.isDispatchThread()) {
    		return false;
    	}
        Rectangle newClipBounds = g2.getClipBounds();
        if (newClipBounds == null || newClipBounds.width * newClipBounds.height < clipAreaThreshold) {
            return false;
        }
        return true;
	}
}