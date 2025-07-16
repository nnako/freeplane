/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2012 Freeplane team and others
 *
 *  this file is created by Dimitry Polivaev in 2012.
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
package org.freeplane.core.ui.components.html;

import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import org.freeplane.core.util.HtmlUtils;

public class ScaledHTML extends BasicHTML{
	/**  Set the document-wide run direction so GlyphView and TextLayout
	 *   apply the Unicode BiDi algorithm with the right base level.  */

	private static void applyComponentOrientation(Document doc,
			ComponentOrientation o) {
		// (1) global flag – required for correct run order
		doc.putProperty(TextAttribute.RUN_DIRECTION,
				o.isLeftToRight()
				? TextAttribute.RUN_DIRECTION_LTR
						: TextAttribute.RUN_DIRECTION_RTL);
	}
	/**
	 * Create an html renderer for the given component and
	 * string of html.
	 */
	static Renderer createHTMLView(JLabel c, String html) {
		ScaledEditorKit kit = SynchronousScaledEditorKit.create();
		Document doc = kit.createDefaultDocument(c);
		applyComponentOrientation(doc, c.getComponentOrientation());
		Object base = c.getClientProperty(documentBaseKey);
        if (base instanceof URL) {
            ((HTMLDocument)doc).setBase((URL)base);
        }
        Reader r = new StringReader(html);
        try {
            kit.read(r, doc, 0);
        } catch (Throwable e) {
        }


        ViewFactory f = kit.getViewFactory();
        View hview = f.create(doc.getDefaultRootElement());
        Renderer v = new Renderer(c, f, hview);
        return v;
    }

    public static void resetPainter() {
        GlyphPainterMetricResetter.resetPainter();
    }

    public static void updateRendererOnPropertyChange(JLabel lbl, String propertyName) {
        String text = lbl.getText();
        GlyphPainterMetricResetter.resetPainter();
        try {
            if("foreground" == propertyName)
                ScaledHTML.updateRendererOnForegroundChange(lbl, text);
            else
                ScaledHTML.updateRenderer(lbl, text);
        }
        finally{
            GlyphPainterMetricResetter.resetPainter();
        }
        View v = (View) lbl.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            lbl.putClientProperty("preferredWidth", v.getPreferredSpan(View.X_AXIS));
        }
    }

    private static void updateRendererOnForegroundChange(JLabel c, String text) {
        Renderer oldRenderer = (Renderer) c.getClientProperty(BasicHTML.propertyKey);
        updateRenderer(c, text);
        if(oldRenderer !=  null) {
            Object newRenderer = c.getClientProperty(BasicHTML.propertyKey);
            if(newRenderer instanceof Renderer)
                ((Renderer) newRenderer).setSize(oldRenderer);
        }
    }

     public static void updateRenderer(JLabel c, String text) {
    	View value = null;
    	try{
    	View oldValue = (View)c.getClientProperty(propertyKey);
    	if (isHTMLString(text)) {
    		value = ScaledHTML.createHTMLView(c, text);
    	}
    	else if (c.getClientProperty(StyleSheet.class) != null) {
    		value = ScaledHTML.createHTMLView(c, HtmlUtils.plainToHTML(text));
    	}
    	if (value != oldValue && oldValue != null) {
    		for (int i = 0; i < oldValue.getViewCount(); i++) {
    			oldValue.getView(i).setParent(null);
    		}
    	}
    	}
    	finally{
    		c.putClientProperty(BasicHTML.propertyKey, value);
    	}
    }


    /**
     * Overrides to the default stylesheet.  Should consider
     * just creating a completely fresh stylesheet.
     */
    static final String styleChanges =
    "p { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0 }" +
    "body { margin-top: 0; margin-bottom: 0; margin-left: 0; margin-right: 0 }";

	/**
     * Root text view that acts as an HTML renderer.
     */
    public static class Renderer extends View {

    	private static final int NOT_INITIALIZED = -1;

    	private int width;
    	private int height;
    	private View view;
    	private ViewFactory factory;
    	private JComponent host;
    	private boolean setSizeRunning;
    	private float initialWidth = NOT_INITIALIZED;
    	private float initialHeight = NOT_INITIALIZED;


		Renderer(JComponent c, ViewFactory f, View v) {
    		super(null);
    		setSizeRunning = true;
    		host = c;
    		factory = f;
    		view = v;
    		view.setParent(this);
    		// initially layout to the preferred size
    	}

        private void setSize(Renderer oldRenderer) {
            initialWidth = oldRenderer.initialWidth;
            if(initialWidth != NOT_INITIALIZED) {
                initialHeight = oldRenderer.initialHeight;
                setSize(oldRenderer.width, oldRenderer.height);
            }
        }

        @Override
		public AttributeSet getAttributes() {
	    return null;
	}

         @Override
		public float getPreferredSpan(int axis) {
        	 initialize();

        	 if (axis == X_AXIS) {
        		 // width currently laid out to
        		 return width;
        	 }
        	 return view.getPreferredSpan(axis);
         }

         private void initialize() {
        	 if(initialWidth == NOT_INITIALIZED) {
        		 initialWidth = view.getPreferredSpan(X_AXIS);
        		 initialHeight = view.getPreferredSpan(Y_AXIS);
        		 setSize(initialWidth, initialHeight);
        	 }
         }

       @Override
       public float getMinimumSpan(int axis) {
    	   initialize();
    	   return view.getMinimumSpan(axis);
        }

        @Override
		public float getMaximumSpan(int axis) {
	    return Integer.MAX_VALUE;
        }

        @Override
		public void preferenceChanged(View child, boolean width, boolean height) {
        	if(! setSizeRunning){
        		host.revalidate();
        		host.repaint();
        	}
        }

        @Override
		public float getAlignment(int axis) {
	    return view.getAlignment(axis);
        }

        @Override
		public void paint(Graphics g, Shape allocation) {
	    Rectangle alloc = allocation.getBounds();
	    view.setSize(alloc.width, alloc.height);
	    view.paint(g, allocation);
        }

         @Override
		public void setParent(View parent) {
            throw new Error("Can't set parent on root view");
        }

        @Override
		public int getViewCount() {
            return 1;
        }
        @Override
		public View getView(int n) {
            return view;
        }
        @Override
		public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
	    return view.modelToView(pos, a, b);
        }

	@Override
	public Shape modelToView(int p0, Position.Bias b0, int p1,
				 Position.Bias b1, Shape a) throws BadLocationException {
	    return view.modelToView(p0, b0, p1, b1, a);
	}

        @Override
		public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
	    return view.viewToModel(x, y, a, bias);
        }

        @Override
		public Document getDocument() {
            return view.getDocument();
        }

        @Override
		public int getStartOffset() {
	    return view.getStartOffset();
        }

        @Override
		public int getEndOffset() {
	    return view.getEndOffset();
        }

        @Override
		public Element getElement() {
	    return view.getElement();
        }

        @Override
		public void setSize(float width, float height) {
        	setSizeRunning = true;
        	try {
        	this.width = (int) width;
        	this.height = (int) height;
        	view.setSize(width, height);
        	}
        	finally {
        		setSizeRunning = false;
			}
        }

        public void resetWidth() {
        	initialize();
        	setSize(initialWidth, initialHeight);
        }

        @Override
		public Container getContainer() {
            return host;
        }

        @Override
		public ViewFactory getViewFactory() {
	    return factory;
        }

		public float getPreferredWidth() {
			initialize();
			return initialWidth;
		}

		public int getWidth() {
			initialize();
			return width;
		}

		public void setWidth(int width) {
			initialize();
			setSize(width, initialHeight);
		}
    }

 }
