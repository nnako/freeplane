/*
 * Created on 11 Jan 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class AntiAliasingConfigurator {
    private long lastRenderTime = 0;
    private Timer repaintTimer;
    private final int repaintDelay;
    private final int clipAreaThreshold;

    public static final AntiAliasingConfigurator INSTANCE = new AntiAliasingConfigurator(1000/20, 100*100);

    public AntiAliasingConfigurator(int repaintDelay, int clipAreaThreshold) {
        this.repaintDelay = repaintDelay;
        this.clipAreaThreshold = clipAreaThreshold;
    }

    public void configureRenderingHints(JComponent component, Graphics2D g2) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastRenderTime;
        Rectangle clipBounds = g2.getClipBounds();

        if (clipBounds != null && clipBounds.width * clipBounds.height < clipAreaThreshold) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else if (timeDifference < repaintDelay) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            restartRepaintTimer(component, clipBounds);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            stopRepaintTimer();
        }

        lastRenderTime = currentTime;
    }

    private void restartRepaintTimer(JComponent component, Rectangle clipBounds) {
        if (clipBounds == null) {
            return; // No clipping region; nothing to repaint.
        }

        if (repaintTimer != null && repaintTimer.isRunning()) {
            repaintTimer.restart();
        } else {
            repaintTimer = new Timer(repaintDelay, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
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