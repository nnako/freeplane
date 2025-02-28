package org.freeplane.core.util;

import java.awt.Color;

import org.freeplane.core.io.ITreeWriter;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * Defines a color with some utility methods.
 *
 * @author robert.ladstaetter
 */
public class ColorUtils {
	public static final int NON_TRANSPARENT_ALPHA = 255;
	public static final String BLACK = "#000000";

	public static String colorToString(final Color col) {
		if (col == null) {
			return null;
		}
		return String.format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue());
	}
	public static String colorToRGBPercentString(final Color col) {
		if (col == null) {
			return null;
		}
		int alpha = col.getAlpha();
		if(alpha == 255)
			return String.format("#%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue());
		else if(alpha == 0)
			return TextUtils.getText("transparent");
		else

			return String.format("#%02x%02x%02x, %2d%%", col.getRed(), col.getGreen(), col.getBlue(), alpha * 100 / 255);
	}

	public static String colorToRGBAString(final Color col) {
		if (col == null) {
			return null;
		}
		return String.format("#%02x%02x%02x%02x", col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
	}

	public static Color rgbStringToColor(final String str) {
		if (str == null) {
			return null;
		}
		if (str.length() != 7 || str.charAt(0) != '#') {
			throw new NumberFormatException("wrong color format in " + str + ". Expecting #rrggbb");
		}
		final int r = Integer.parseInt(str.substring(1, 3), 16);
		final int g = Integer.parseInt(str.substring(3, 5), 16);
		final int b = Integer.parseInt(str.substring(5, 7), 16);
		return new Color(r, g, b);
	}

	public static Color stringToColor(final String str) {
		if (str == null || str.equals("none")) {
			return null;
		}
		if(str.length() == 7)
			return rgbStringToColor(str);

		if (str.length() != 9 || str.charAt(0) != '#') {
			throw new NumberFormatException("wrong color format in " + str + ". Expecting #aarrggbb");
		}
		final int r = Integer.parseInt(str.substring(1, 3), 16);
		final int g = Integer.parseInt(str.substring(3, 5), 16);
		final int b = Integer.parseInt(str.substring(5, 7), 16);
		final int a = Integer.parseInt(str.substring(7, 9), 16);
		return new Color(r,g,b,a);
	}

	public static Color stringToColor(final String str, final Color opacityHolder) {
		final Color rgb = stringToColor(str);
		if(opacityHolder == null || isNonTransparent(opacityHolder) || rgb == null)
			return rgb;
		else
			return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), opacityHolder.getAlpha());
	}

	public static boolean isNonTransparent(final Color opacityHolder) {
		final boolean isNonTransparent = opacityHolder.getAlpha() == NON_TRANSPARENT_ALPHA;
		return isNonTransparent;
	}

	static public Color alphaToColor(String value, Color color) {
		return alphaToColor(Integer.parseInt(value), color);
	}

	public static Color alphaToColor(final int opacity, final Color rgbHolder) {
		if(rgbHolder == null)
			return new Color(0, 0, 0, opacity);
        if(rgbHolder.getAlpha() == opacity)
    		return rgbHolder;
    	return new Color(rgbHolder.getRed(), rgbHolder.getGreen(), rgbHolder.getBlue(), opacity);
    }

	public static void setColorAttributes(final XMLElement element, String colorAttribute, String opacityAttribite, final Color color) {
		element.setAttribute(colorAttribute, colorToString(color));
		if(! isNonTransparent(color))
			element.setAttribute(opacityAttribite, Integer.toString(color.getAlpha()));
	}

	public static void addColorAttributes(final ITreeWriter writer, String colorAttribute, String opacityAttribite, final Color color) {
		writer.addAttribute(colorAttribute, colorToString(color));
		if(! isNonTransparent(color))
			writer.addAttribute(opacityAttribite, Integer.toString(color.getAlpha()));
	}

	public static Color makeNonTransparent(Color color){
		return makeNonTransparent(color, Color.WHITE);
	}

	public static Color makeNonTransparent(Color color, Color background) {
		if(color == null)
			return null;
		if(isNonTransparent(color))
			return color;
		final int r1 = color.getRed();
		final int g1 = color.getGreen();
		final int b1 = color.getBlue();
		final int a1 = color.getAlpha();
		final int r2 = background.getRed();
		final int g2 = background.getGreen();
		final int b2 = background.getBlue();
		final int r3 = r2 + (r1-r2)*a1 / NON_TRANSPARENT_ALPHA;
		final int g3 = g2 + (g1-g2)*a1 / NON_TRANSPARENT_ALPHA;
		final int b3 = b2 + (b1-b2)*a1 / NON_TRANSPARENT_ALPHA;
		return new Color(r3, g3, b3);
	}

	public static boolean isDark(Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();

		double brightness = 0.299 * (r * r) + 0.587 * (g * g) + 0.114 * (b * b);

		boolean isDark = brightness < 128*128;
		return isDark;
	}

	public static Color blendColors(Color[] colors) {
        double r = 0, g = 0, b = 0, a = 0;

        for (Color color : colors) {
            double srcA = color.getAlpha() / 255.0;
            double srcR = color.getRed() / 255.0;
            double srcG = color.getGreen() / 255.0;
            double srcB = color.getBlue() / 255.0;

            r = srcR * srcA + r * (1 - srcA);
            g = srcG * srcA + g * (1 - srcA);
            b = srcB * srcA + b * (1 - srcA);
            a = srcA + a * (1 - srcA);

            if (color.getAlpha() == 255) {
                break; // Stop blending since this color is fully opaque
            }
        }

        return new Color((float) r, (float) g, (float) b, (float) a);
    }
	public static Color blendColors(Color top, Color bottom) {
	    double topA = top.getAlpha() / 255.0;
	    double bottomA = bottom.getAlpha() / 255.0;
	    double outA = topA + bottomA * (1 - topA);
	    double outR = 0, outG = 0, outB = 0;
	    if (outA > 0) {
	        outR = (top.getRed() / 255.0 * topA + bottom.getRed() / 255.0 * bottomA * (1 - topA)) / outA;
	        outG = (top.getGreen() / 255.0 * topA + bottom.getGreen() / 255.0 * bottomA * (1 - topA)) / outA;
	        outB = (top.getBlue() / 255.0 * topA + bottom.getBlue() / 255.0 * bottomA * (1 - topA)) / outA;
	    }
	    return new Color((float) outR, (float) outG, (float) outB, (float) outA);
	}
}
