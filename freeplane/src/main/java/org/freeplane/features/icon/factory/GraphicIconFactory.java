package org.freeplane.features.icon.factory;

import java.awt.Component;
import java.awt.Font;
import java.net.URL;
import java.util.WeakHashMap;

import javax.swing.Icon;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.svgicons.FreeplaneIconFactory;
import org.freeplane.features.icon.UIIcon;

class GraphicIconFactory implements IconFactory {
	private static final String SVG_EXT = ".svg";
	private static final String SVG_EXT_WITH_QUERY = SVG_EXT + ResourceController.USE_ACCENT_COLOR_QUERY;
	private static final String DEFAULT_IMAGE_PATH = "/images/";
	static final IconFactory FACTORY = new GraphicIconFactory();
	private static final Icon ICON_NOT_FOUND = FACTORY.getIcon(ResourceController.getResourceController()
	    .getResource(DEFAULT_IMAGE_PATH + "IconNotFound.svg"));


	private final WeakValueCache<String, Icon> ICON_CACHE = new WeakValueCache<String, Icon>();
	private final WeakHashMap<Icon, URL> ICON_URLS = new WeakHashMap<Icon, URL>();



	private GraphicIconFactory() {};

	@Override
	public Icon getIcon(final UIIcon uiIcon) {
		return getIcon(uiIcon.getUrl(), DEFAULT_UI_ICON_HEIGTH);
	}

	@Override
	public Icon getIcon(final URL url) {
		return getIcon(url, DEFAULT_UI_ICON_HEIGTH);
	}

	private String createCacheKey(final URL url, final int heightPixels) {
		return url.toString() + "#" + heightPixels;
	}

	@Override
	public Icon getIcon(UIIcon uiIcon, Quantity<LengthUnit> iconHeight) {
		return getIcon(uiIcon.getUrl(), iconHeight);
	}

	@Override
	public Icon getIcon(final URL url, Quantity<LengthUnit> iconHeight) {
		Icon result = ICON_NOT_FOUND;
		if (url != null) {
			final int heightPixels = iconHeight.toBaseUnitsRounded();
			final String cacheKey = createCacheKey(url, heightPixels);
			if (ICON_CACHE.containsKey(cacheKey)) {
				result = ICON_CACHE.get(cacheKey);
			}
			else {
				String path = url.getPath();
				if (path.endsWith(SVG_EXT) || path.endsWith(SVG_EXT_WITH_QUERY)) {
					result = FreeplaneIconFactory.createSVGIcon(url, heightPixels);
				}
				else {
					result = FreeplaneIconFactory.createIconPrivileged(url);
				}
				ICON_CACHE.put(cacheKey, result);
				registerIcon(result, url);
			}
		}
		return result;
	}



	@Override
	public void registerIcon(Icon icon, URL url) {
		ICON_URLS.put(icon, url);

	}

	@Override
	public boolean canScaleIcon(final Icon icon) {
		return ICON_URLS.containsKey(icon);
	}

	@Override
	public Icon getScaledIcon(final Icon icon, Quantity<LengthUnit> iconHeight) {
		if (iconHeight.toBaseUnitsRounded() == icon.getIconHeight())
			return icon;
		final URL iconUrl = ICON_URLS.get(icon);
		if (iconUrl != null)
			return getIcon(iconUrl, iconHeight);
		else
			throw new IllegalArgumentException("unknown icon");
	}

	@Override
	public  Icon getScaledIcon(final Icon icon, final Component component) {
		if(!canScaleIcon(icon))
			return icon;
		final Font font = component.getFont();
		final int fontHeight = component.getFontMetrics(font).getHeight();
		final Quantity<LengthUnit> iconHeight = new Quantity<LengthUnit>(fontHeight, LengthUnit.px);
		Icon scaledIcon = getScaledIcon(icon, iconHeight);
		return scaledIcon;
	}


}