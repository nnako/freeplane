/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
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
package org.freeplane.core.resources;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.Icon;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.ActionAcceleratorManager;
import org.freeplane.core.ui.TimePeriodUnits;
import org.freeplane.core.ui.svgicons.FreeplaneIconFactory;
import org.freeplane.core.util.ColorUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.factory.IconFactory;
import org.freeplane.features.mode.AController.IActionOnChange;
import org.freeplane.features.mode.Controller;

/**
 * @author Dimitry Polivaev
 */
public abstract class ResourceController {
	public static final String USE_ACCENT_COLOR = "useAccentColor=true";
	public static final String USE_ACCENT_COLOR_QUERY = "?" + USE_ACCENT_COLOR;
	public static final String FREEPLANE_PROPERTIES = "/freeplane.properties";
	public static final String LOCAL_PROPERTIES = "LocalProperties.";
	public static final String RESOURCE_DRAW_RECTANGLE_FOR_SELECTION = "standarddrawrectangleforselection";
	// some plugins have their own file for registration of defaults

	static public ResourceController getResourceController() {
		return Controller.getCurrentController().getResourceController();
	}

	final private List<IFreeplanePropertyListener> propertyChangeListeners = new Vector<IFreeplanePropertyListener>();
	static private ActionAcceleratorManager acceleratorManager;
	private ResourceBundles resources;
	public static final String FREEPLANE_RESOURCE_URL_PROTOCOL = "freeplaneresource";
	public static final String OBJECT_TYPE = "ObjectType";

    private final Locale systemLocale;

    public ResourceController() {
		super();
	    systemLocale = Locale.getDefault();
	}

	public void addLanguageResources(final String language, final URL url) {
		resources.addResources(language, url);
	}

	public void addLanguageResources(final String language, final Map<String, String> resources) {
		this.resources.addResources(language, resources);
	}

    public void putUserResourceString(String key, String value) {
        resources.putUserResourceString(key, value);
    }

    public void putResourceString(String key, String value) {
        resources.putResourceString(key, value);
    }

	public void addPropertyChangeListener(final IFreeplanePropertyListener listener) {
		propertyChangeListeners.add(listener);
	}

	/**
	 * @param listener
	 *            The new listener. All currently available properties are sent
	 *            to the listener after registration. Here, the oldValue
	 *            parameter is set to null.
	 */
	public void addPropertyChangeListenerAndPropagate(final IFreeplanePropertyListener listener) {
		addPropertyChangeListener(listener);
		for (final Entry<Object, Object> entry : getProperties().entrySet()) {
			final String key = (String) entry.getKey();
			listener.propertyChanged(key, (String) entry.getValue(), null);
		}
	}

	protected void loadAnotherLanguage() {
		resources.loadAnotherLanguage(getProperty(ResourceBundles.RESOURCE_LANGUAGE));
	}

	public void firePropertyChanged(final String property, final String value, final String oldValue) {
		if (oldValue == null || !oldValue.equals(value)) {
			for (final IFreeplanePropertyListener listener : getPropertyChangeListeners()) {
				listener.propertyChanged(property, value, oldValue);
			}
		}
	}

	public boolean getBooleanProperty(final String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

	public boolean getBooleanProperty(final String key, final boolean defaultValue) {
		final String value = getProperty(key, null);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnumProperty(String propertyName, Enum<T> defaultValue) {
		try {
			final String propertyValue = getProperty(propertyName);
			if(propertyValue != null)
			    defaultValue = Enum.valueOf(defaultValue.getClass(), propertyValue.toUpperCase(Locale.ENGLISH));
		}
		catch (Exception e) {
			LogUtils.severe(e);
		}
		return (T) defaultValue;
	}

	/**
	 * @param key
	 * @return
	 */
	public String getDefaultProperty(final String key) {
		return null;
	}

	/** register defaults in freeplane.properties respectively defaults.properties instead! */
	public double getDoubleProperty(final String key, final double defaultValue) {
		try {
			return Double.parseDouble(ResourceController.getResourceController().getProperty(key));
		}
		catch (final Exception e) {
			return defaultValue;
		}
	}

	/**
	 * @return
	 */
	abstract public String getFreeplaneUserDirectory();

	/** register defaults in freeplane.properties respectively defaults.properties instead! */
	public int getIntProperty(final String key, final int defaultValue) {
		try {
			return Integer.parseInt(getProperty(key));
		}
		catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public int getLengthProperty(String name) {
		final Quantity<LengthUnit> quantity = getLengthQuantityProperty(name);
		return quantity.toBaseUnitsRounded();
	}

	public Quantity<LengthUnit> getLengthQuantityProperty(String name) {
		final String property = getProperty(name);
		final Quantity<LengthUnit> quantity = Quantity.fromString(property, LengthUnit.px);
		return quantity;
	}

	public int getTimeProperty(String name) {
		final Quantity<TimePeriodUnits> quantity = getTimeQuantityProperty(name);
		return quantity.toBaseUnitsRounded();
	}

	public Quantity<TimePeriodUnits> getTimeQuantityProperty(String name) {
		final String property = getProperty(name);
		final Quantity<TimePeriodUnits> quantity = Quantity.fromString(property, TimePeriodUnits.ms);
		return quantity;
	}

	public int getIntProperty(String key) {
		return Integer.parseInt(getProperty(key));
	}

	public double getDoubleProperty(String key) {
		return Double.parseDouble(getProperty(key));
	}

	/** register defaults in freeplane.properties respectively defaults.properties instead. */
	public long getLongProperty(final String key, final long defaultValue) {
		try {
			return Long.parseLong(getProperty(key));
		}
		catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}

	public Color getColorProperty(String name) {
		String string = getProperty(name);
		return ColorUtils.stringToColor(string);
	}

    public File getFile(String key) {
        String value = getProperty(key);
        String freeplaneUserDirectory = ResourceController.getResourceController().getFreeplaneUserDirectory();
        value = TextUtils.replaceAtBegin(value, "{freeplaneuserdir}", freeplaneUserDirectory);
        File file = new File(value);
        return file;
    }

	abstract public Properties getProperties();

	abstract public String getProperty(final String key);

	/** register defaults in freeplane.properties respectively defaults.properties instead! */
	public String getProperty(final String key, final String value) {
		return getProperties().getProperty(key, value);
	}

	public boolean containsProperty(String key) {
		return getProperties().containsKey(key);
	}

	public Collection<IFreeplanePropertyListener> getPropertyChangeListeners() {
		return Collections.unmodifiableCollection(propertyChangeListeners);
	}

	public URL getResource(final String resourcePath) {
		return getClass().getResource(resourcePath);
	}

	public InputStream getResourceStream(final String resFileName) throws IOException {
		final URL resUrl = getResource(resFileName);
		if (resUrl == null) {
			LogUtils.severe("Can't find " + resFileName + " as resource.");
			throw new IllegalArgumentException("Can't find " + resFileName + " as resource.");
		}
		return new BufferedInputStream(resUrl.openStream());
	}

	public String getResourceBaseDir() {
		return "";
	}

	public String getInstallationBaseDir() {
		return "";
	}

	/** Returns the ResourceBundle with the current language */
	public ResourceBundle getResources() {
		if (resources == null) {
			resources = new ResourceBundles(getProperty(ResourceBundles.RESOURCE_LANGUAGE));
		}
		return resources;
	}

	public String getLanguageCode() {
		return resources.getLanguageCode();
	}

	public String getDefaultLanguageCode() {
		return resources.getDefaultLanguageCode();
	}

	public String getText(final String key, final String resource) {
		return ((ResourceBundles) getResources()).getResourceString(key, resource);
	}

	protected void init() {
	}

	public void removePropertyChangeListener(final IFreeplanePropertyListener listener) {
		propertyChangeListeners.remove(listener);
	}

	abstract public void saveProperties();

	abstract public void setDefaultProperty(final String key, final String value);

	public void setProperty(final String property, final boolean value) {
		setProperty(property, Boolean.toString(value));
	}

	public void setProperty(String name, int value) {
		setProperty(name, Integer.toString(value));
	}

    public void setProperty(String name, long value) {
        setProperty(name, Long.toString(value));
    }

    public void setProperty(String name, double value) {
        setProperty(name, Double.toString(value));
    }

	abstract public void setProperty(final String property, final String value);

	/** adds properties from url to properties. Existing properties in resultProps will be overridden.
	 * @return false if anything went wrong. */
	public static boolean loadProperties(Properties resultProps, final URL url) {
		try (InputStream in = new BufferedInputStream(url.openStream()) ){
			resultProps.load(in);
			LogUtils.info("Loaded properties from " + url);
			return true;
		}
		catch (final Exception ex) {
			System.err.println("Could not load properties from " + url);
		}
		return false;
	}

	/** will add properties from propertiesUrl if they don't exist yet. */
	public void addDefaults(URL propertiesUrl) {
		Properties props = new Properties();
		loadProperties(props, propertiesUrl);
		addDefaults(props);
	}

	/** use generic to make it useable with Properties. KT and VT must be of type String. */
	public <KT, VT> void addDefaults(Map<KT, VT> defaultProperties) {
		for (Entry<KT, VT> entry : defaultProperties.entrySet()) {
			setDefaultProperty((String) entry.getKey(), (String) entry.getValue());
		}
	}

	public boolean isApplet() {
		return false;
	}

	public void removePropertyChangeListener(final Class<? extends IActionOnChange> clazz,
	                                         final AFreeplaneAction action) {
		final Iterator<IFreeplanePropertyListener> iterator = propertyChangeListeners.iterator();
		while (iterator.hasNext()) {
			final IFreeplanePropertyListener next = iterator.next();
			if (next instanceof IActionOnChange && ((IActionOnChange) next).getAction() == action) {
				iterator.remove();
				return;
			}
		}
	}

	public ActionAcceleratorManager getAcceleratorManager() {
		if (acceleratorManager == null)
			acceleratorManager = new ActionAcceleratorManager();
		return acceleratorManager;
	}

	private Map<String, Icon> iconCache = new HashMap<String, Icon>();

    public Icon getIcon(final String iconKey) {
        return getIcon(iconKey, false);
    }

    public Icon getOptionalIcon(final String iconKey) {
        return getIcon(iconKey, true);
    }

    private Icon getIcon(final String iconKey, boolean isOptional) {
        Icon icon = iconCache.get(iconKey);
		if (icon == null) {
			final String iconResource = iconKey.startsWith("/") ? iconKey : getProperty(iconKey, null);
			icon = loadIcon(iconResource, isOptional);
			if (icon != null)
				iconCache.put(iconKey, icon);
		}
		return icon;
    }

	public URL getIconResource(String resourcePath) {
		if (resourcePath != null) {
			final String urlPath;
			boolean usesAccentColor = resourcePath.endsWith(USE_ACCENT_COLOR_QUERY);
			if(usesAccentColor)
				urlPath = resourcePath.substring(0, resourcePath.length() - USE_ACCENT_COLOR_QUERY.length());
			else
				urlPath = resourcePath;
			URL url = getResource(urlPath);
			if (url != null) {
				return usesAccentColor
						? withAccentColorQuery(url)
						: url;
			}
		}
		return null;
	}


	private Icon loadIcon(final String resourcePath, boolean isOptional) {
		if(resourcePath == null)
			return null;
		URL url = getIconResource(resourcePath);
		if (url != null) {
			return IconFactory.getInstance().getIcon(url, IconFactory.DEFAULT_UI_ICON_HEIGHT);
		}
		else if(! isOptional){
			LogUtils.severe("can not load icon '" + resourcePath + "'");
		}
		return null;
	}

	private URL withAccentColorQuery(URL url){
		try {
			return new URL(url.toString() + USE_ACCENT_COLOR_QUERY);
		} catch (MalformedURLException e) {
			LogUtils.severe(e);
			return url;
		}
	}

	public Icon getImageIcon(String iconKey) {
		return FreeplaneIconFactory.toImageIcon(getIcon(iconKey));
	}

	public Locale getSystemLocale() {
        return systemLocale;
    }

    public String[] getArrayProperty(String key, String separator) {
        String joinedValues = getProperty(key);
        final String[] groupNames = joinedValues != null
                ? joinedValues.split(separator)  : new String[] {};
        return groupNames;
    }

    abstract public Properties getSecuredProperties();

    abstract public void secureProperty(String key);

    public String loadString(String path) {
    	try(Scanner s = new Scanner(getResourceStream(path))){
    		s.useDelimiter("\\A");
    		return s.hasNext() ? s.next() : "";
    	} catch (IOException e) {
    		LogUtils.severe(e);
    		return "";
    	}
    }
}
