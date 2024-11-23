package org.freeplane.features.filter.condition;

import java.awt.Color;
import java.awt.FontMetrics;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.freeplane.core.ui.components.IconListComponent;
import org.freeplane.core.ui.components.IconRow;
import org.freeplane.core.ui.components.ObjectIcon;
import org.freeplane.core.ui.components.TextIcon;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.condition.ConditionFactory.ConditionOperator;
import org.freeplane.features.filter.condition.ConditionFactory.ConditionOption;
import org.freeplane.n3.nanoxml.XMLElement;


public abstract class ASelectableCondition  implements ICondition{
	public static final float STRING_MIN_MATCH_PROB = 0.7F;
	transient private String description;
	private String userName;
	private static Method EQUALS;
	private static Method HASH;
	static{
		try{
			final ClassLoader classLoader = ASelectableCondition.class.getClassLoader();
			EQUALS = classLoader.loadClass("org.apache.commons.lang.builder.EqualsBuilder").getMethod("reflectionEquals", Object.class, Object.class);
			HASH = classLoader.loadClass("org.apache.commons.lang.builder.HashCodeBuilder").getMethod("reflectionHashCode", Object.class);
		}
		catch(Exception e){

		}
	}

    static protected Color operatorBackgroundColor() {
        Color optionBackgroundColor = UITools.isLightLookAndFeelInstalled() ? Color.BLACK.brighter() : Color.WHITE.darker();
        return optionBackgroundColor;
    }

    public ASelectableCondition() {
		super();
	}

	@Override
    public int hashCode() {
		if(HASH == null){
			return super.hashCode();
		}
		try {
	        return (Integer) HASH.invoke(null, this);
        }
        catch (Exception e) {
	        e.printStackTrace();
	        return super.hashCode();
        }
    }

	@Override
	public boolean equals(final Object obj) {
		if(EQUALS == null){
			return super.equals(obj);
		}
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
				@Override
				public Boolean run()
			            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
					return (Boolean) EQUALS.invoke(null, ASelectableCondition.this, obj);
				}
			}).booleanValue();
        }
        catch (Exception e) {
	        e.printStackTrace();
	        return super.equals(obj);
        }
    }
	protected abstract String createDescription();

	protected List<Icon> createRenderedIcons(final String attribute, final ConditionFactory.ConditionOperator simpleCondition, final String value,
            final boolean matchCase, final boolean matchApproximately, final boolean ignoreDiacritics, FontMetrics  fontMetrics) {
        return createRenderedIcons(attribute, simpleCondition, new TextIcon(value, fontMetrics),
                matchCase, matchApproximately, ignoreDiacritics, fontMetrics);
    }

    protected List<Icon> createRenderedIcons(final String condition, FontMetrics fontMetrics) {
        return createRenderedIcons(condition, ConditionOperator.EMPTY, "", false, false, false, fontMetrics);
    }

    protected List<Icon> createRenderedIcons(final String attribute, final ConditionFactory.ConditionOperator conditionOperator, final Icon valueIcon,
            final boolean matchCase, final boolean matchApproximately, final boolean ignoreDiacritics, FontMetrics  fontMetrics) {
        List<Icon> icons = new ArrayList<>();
        if(! attribute.isEmpty()) {
            icons.add(textIcon(attribute, fontMetrics));
        }
        String operator = conditionOperator.getOperator();
        Color optionBackgroundColor = operatorBackgroundColor();
        boolean isOperatorBlank = operator.isEmpty() || operator.equals(" ");
        Icon operatorIcon = textIcon(operator, fontMetrics,
                icon -> {
                    if(! isOperatorBlank)
                        icon.setIconBackgroundColor(optionBackgroundColor);
                });
        Icon openingValueDelimiterIcon = textIcon(conditionOperator.getOpeningValueDelimiter(), fontMetrics);
        Icon closingValueDelimiterIcon = textIcon(conditionOperator.getClosingValueDelimiter(), fontMetrics);
        TextIcon gapIcon = textIcon(" ", fontMetrics);
        if(! operator.isEmpty()) {
            icons.add(gapIcon);
            if(! isOperatorBlank) {
                icons.add(operatorIcon);
                icons.add(gapIcon);
            }
        }
        icons.add(openingValueDelimiterIcon);
        icons.add(valueIcon);
        icons.add(closingValueDelimiterIcon);
        if(matchCase) {
            icons.add(gapIcon);
            icons.add(textIcon(ConditionOption.FILTER_MATCH_CASE.getDisplayedOption(), fontMetrics,
                    icon -> icon.setIconBackgroundColor(optionBackgroundColor)));
        }
        if(ignoreDiacritics) {
            icons.add(gapIcon);
            icons.add(textIcon(ConditionOption.FILTER_IGNORE_DIACRITICS.getDisplayedOption(), fontMetrics,
                    icon -> icon.setIconBackgroundColor(optionBackgroundColor)));
        }
        if(matchApproximately) {
            icons.add(gapIcon);
            icons.add(textIcon(ConditionOption.FILTER_MATCH_APPROX.getDisplayedOption(), fontMetrics,
                    icon -> icon.setIconBackgroundColor(optionBackgroundColor)));
        }
        return icons;
    }

    protected TextIcon textIcon(final String text,
            FontMetrics fontMetrics) {
        return textIcon(text, fontMetrics, x -> {/**/});
    }
    protected TextIcon textIcon(final String text,
            FontMetrics fontMetrics, Consumer<TextIcon> config) {
        TextIcon icon = new TextIcon(text, fontMetrics);
        config.accept(icon);
        return icon;
    }

	final public IconListComponent getListCellRendererComponent(FontMetrics fontMetrics) {
	    IconListComponent renderer = createGraphicComponent(fontMetrics);
		renderer.setToolTipText(toString());
		return renderer;
	}

	protected IconListComponent createGraphicComponent(FontMetrics fontMetrics) {
		List<Icon> icons = createRenderedIcons(fontMetrics);
		List<Icon> iconsWithName;
		if(userName != null){
		    iconsWithName = new ArrayList<Icon>(icons.size() + 1);
		    iconsWithName.add(new TextIcon(userName + " : ", fontMetrics));
		    iconsWithName.addAll(icons);
			return createIconListComponent(iconsWithName);
		}
		return createIconListComponent(icons);
	}

    protected IconListComponent createIconListComponent(List<Icon> icons) {
        IconRow icon = new IconRow(icons);
        ObjectIcon o = new ObjectIcon(this, icon);
        List<ObjectIcon> singletonList = Collections.singletonList(o);
        return new IconListComponent(singletonList, toString());
    }

	abstract protected List<Icon> createRenderedIcons(FontMetrics  fontMetrics);

	protected List<Icon> createRenderedIconsFromDescription(FontMetrics  fontMetrics) {
	    return Collections.singletonList(new TextIcon(toString(), fontMetrics));
    }

    protected String createDescription(final String attribute, final String simpleCondition, final String value,
            final boolean matchCase, final boolean matchApproximately,
            final boolean ignoreDiacritics) {
        final String description = createSimpleDescription(attribute, simpleCondition, value)
                + (matchCase && value != null ? ", " + TextUtils.getText(ConditionFactory.FILTER_MATCH_CASE) : "")
                + (matchApproximately && value != null ? ", " + TextUtils.getText(ConditionFactory.FILTER_MATCH_APPROX) : ""
                        + (ignoreDiacritics && value != null ? ", " + TextUtils.getText(ConditionFactory.FILTER_IGNORE_DIACRITICS) : ""));
        return description;
    }

    protected String createDescription(final String attribute, final String simpleCondition, final String value) {
        return createSimpleDescription(attribute, simpleCondition, value);
    }

    private String createSimpleDescription(final String attribute, final String simpleCondition,
            final String value) {
        final String description = attribute + " " + simpleCondition + (value != null ? " \"" + value + "\"" : "");
        return description;
    }

	@Override
    final public String toString() {
    	if (description == null) {
    		description = createDescription();
    	}
    	return description;
    }

	public void toXml(final XMLElement element) {
		final XMLElement child = new XMLElement();
		child.setName(getName());
		if(userName != null){
			child.setAttribute("user_name", userName);
		}
		fillXML(child);
		element.addChild(child);
	}

	protected void fillXML(XMLElement element){}

	abstract protected String getName();


	public void setUserName(String userName) {
		if(userName == this.userName || userName != null && userName.equals(this.userName))
			return;
	    this.userName = userName;
    }


	public String getUserName() {
	    return userName;
    }


    protected List<Icon> createSmallRendererIcons(FontMetrics  fontMetrics) {
        if(userName == null){
            return createRenderedIcons(fontMetrics);
        }
        return Collections.singletonList(new TextIcon('"' + userName + '"', fontMetrics));
    }

    protected String createSmallDescription() {
        if(userName == null){
            return createDescription();
        }
        return '"' + userName + '"';
    }

    public boolean canBePersisted() {
        return true;
    }

    public ASelectableCondition removeCondition(ASelectableCondition removedCondition) {
        return removedCondition == this ? null : this;
    }
}
