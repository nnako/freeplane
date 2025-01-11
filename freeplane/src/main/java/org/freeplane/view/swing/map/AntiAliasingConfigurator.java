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
    private final int repaintThreshold;
    private final int repaintDelay;
    private final int clipAreaThreshold;
    private boolean isRepaintInProgress;
	private final JComponent component;
	private Rectangle clipBounds;

    public AntiAliasingConfigurator(JComponent component) {
    	this(component, 100*100, 200, MILLISECONDS_PER_SECOND/25);
    }
    public AntiAliasingConfigurator(JComponent component, int clipAreaThreshold, int repaintThreshold, int repaintDelay) {
        this.component = component;
        this.repaintThreshold = repaintThreshold;
		this.repaintDelay = repaintDelay;
        this.clipAreaThreshold = clipAreaThreshold;
        isRepaintInProgress = false;
    }

    public void configureRenderingHints(Graphics2D g2) {
    	if(! EventQueue.isDispatchThread()) {
    		enableAntialias(g2);
    		return;
    	}
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastRenderTime;
        Rectangle newClipBounds = g2.getClipBounds();
        if (newClipBounds != null && newClipBounds.width * newClipBounds.height < clipAreaThreshold) {
            enableAntialias(g2);
        } else if (timeDifference < repaintThreshold) {
            if (newClipBounds != null) {
				clipBounds = clipBounds == null ? newClipBounds
					: clipBounds.union(newClipBounds).intersection(component.getBounds());
				isRepaintInProgress = true;
				SwingUtilities.invokeLater(this::restartRepaintTimer);
			}
            disableAntialias(g2);
        } else {
        	stopRepaintTimer();
        	clipBounds = null;
        	isRepaintInProgress = false;
        	enableAntialias(g2);
        }

        lastRenderTime = currentTime;
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
            return; // No clipping region; nothing to repaint.
        }

        if (repaintTimer != null && repaintTimer.isRunning()) {
            repaintTimer.restart();
        } else {
            repaintTimer = new Timer(repaintDelay, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                	if(isRepaintInProgress)
                		component.repaint(clipBounds);
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
}