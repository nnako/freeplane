/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2011 dimitry
 *
 *  This file author is dimitry
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
package org.freeplane.features.text.mindmapmode;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

/**
 * @author Dimitry Polivaev
 * Aug 23, 2011
 */
public class EventBuffer implements KeyEventDispatcher, FocusListener, InputMethodListener {
	ArrayList<AWTEvent> events = new ArrayList<>(100);
	private Component textComponent;
	boolean isActive = false;
	private AWTEvent firstEvent;
	private AWTEvent dispatchedEvent = null;
	private Component focusOwner;

	public boolean isActive() {
		return isActive;
	}
	EventBuffer(){}
	public Component getTextComponent() {
		return textComponent;
	}

	public void setTextComponent(Component c) {
		if(textComponent != null)
			textComponent.removeFocusListener(this);
		this.textComponent = c;
		if(textComponent != null)
			textComponent.addFocusListener(this);
	}

	public boolean dispatchKeyEvent(final KeyEvent ke) {
		if(ke.equals(dispatchedEvent) || events.isEmpty() && ke.getID() != KeyEvent.KEY_PRESSED){
			return false;
		}
		addAwtEvent(ke);

		// Prevent Freeplane freeze
		if(ke.getKeyCode() == KeyEvent.VK_ESCAPE
				&& ke.getID() == KeyEvent.KEY_RELEASED){
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(EventBuffer.this);
				}
			});
		}
		ke.consume();
		return true;
	}
	private void addAwtEvent(final AWTEvent ke) {
		events.add(ke);
	}

	public void focusGained(final FocusEvent e) {
		textComponent.removeFocusListener(this);
		SwingUtilities.invokeLater(() -> {
			try{
				for (int i = 0; i < events.size(); i++) {
					final AWTEvent event = events.get(i);
					if(event.getSource().equals(textComponent))
						dispatchedEvent = event;
					else if(event instanceof KeyEvent)
					{
						KeyEvent ke = (KeyEvent) event;
						dispatchedEvent = new KeyEvent(textComponent, ke.getID(), ke.getWhen(), ke.getModifiers(), ke.getKeyCode(), ke.getKeyChar(), ke.getKeyLocation());
					}
					else if(event instanceof InputMethodEvent)
					{
						InputMethodEvent ime = (InputMethodEvent) event;
						dispatchedEvent = new InputMethodEvent(textComponent, ime.getID(), ime.getWhen(), ime.getText(),
								ime.getCommittedCharacterCount(), ime.getCaret(), ime.getVisiblePosition());
					}
					e.getComponent().dispatchEvent(dispatchedEvent);
					dispatchedEvent = null;
				}
			}
			finally{
				deactivate();
			}
		});
	}

	public void focusLost(final FocusEvent e) {
	}

	public void deactivate() {
		if(! isActive)
			return;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
		isActive = false;
		if(textComponent != null) {
			textComponent.removeFocusListener(this);
			textComponent = null;
		}
		if(focusOwner != null) {
			focusOwner.removeInputMethodListener(this);
			focusOwner = null;
		}
		events.clear();
		firstEvent = null;
		dispatchedEvent = null;
	}
	public void activate(AWTEvent e) {
		if(!isActive) {
			final KeyboardFocusManager currentKeyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			currentKeyboardFocusManager.addKeyEventDispatcher(this);
			focusOwner = currentKeyboardFocusManager.getFocusOwner();
			focusOwner.addInputMethodListener(this);
			isActive = true;

		if(e instanceof MouseEvent)
			setFirstEvent((MouseEvent)e);
		else if(e != null)
			addAwtEvent(e);
		}
	}
	public void setFirstEvent(AWTEvent e) {
		firstEvent = e;
	}

	public AWTEvent getFirstEvent(){
		if(firstEvent != null)
			return firstEvent;
		if(events.isEmpty())
			return null;
		return events.get(0);
	}

	public MouseEvent getMouseEvent() {
		if(firstEvent instanceof MouseEvent)
			return (MouseEvent) firstEvent;
		else
			return null;
	}
	@Override
	public void inputMethodTextChanged(InputMethodEvent event) {
		addAwtEvent(event);
	}
	@Override
	public void caretPositionChanged(InputMethodEvent event) {
		addAwtEvent(event);
	}
}
