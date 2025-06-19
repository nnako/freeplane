/*
 * Created on 30 Jan 2024
 *
 * author dimitry
 */
package org.freeplane.plugin.script;

import java.util.Date;
import java.util.function.Supplier;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.MenuUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.format.FormatController;
import org.freeplane.features.format.ScannerController;
import org.freeplane.plugin.script.proxy.Convertible;
import org.freeplane.plugin.script.proxy.ScriptUtils;

/**
 * Provides static imports for Freeplane scripting utilities to enable easy access to common functionality
 * when compiling Groovy source code outside of Freeplane's script environment.
 * 
 * <p>This class is particularly useful when creating utility scripts or add-on classes that need to be
 * compiled as JAR files. In regular Freeplane scripts, these utilities are available as global variables,
 * but when compiling outside Freeplane, you need to import them explicitly.</p>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>
 * import static org.freeplane.plugin.script.GroovyStaticImports.*
 * 
 * // Now you can use:
 * logger.info("Hello from my utility script")
 * ui.informationMessage("This is a message")
 * String plainText = htmlUtils.htmlToPlain("&lt;b&gt;Bold text&lt;/b&gt;")
 * </pre>
 * 
 * <p>The following utilities are made available through static imports:</p>
 * <ul>
 * <li>{@link #logger} - for logging messages (see {@link LogUtils})</li>
 * <li>{@link #ui} - for GUI operations and dialogs (see {@link UITools})</li>
 * <li>{@link #htmlUtils} - for HTML/XML processing (see {@link HtmlUtils})</li>
 * <li>{@link #textUtils} - for text processing and translations (see {@link TextUtils})</li>
 * <li>{@link #menuUtils} - for menu operations (see {@link MenuUtils})</li>
 * <li>{@link #scriptUtils} - for script-specific utilities (see {@link ScriptUtils})</li>
 * <li>{@link #config} - for accessing Freeplane configuration (see {@link org.freeplane.plugin.script.FreeplaneScriptBaseClass.ConfigProperties})</li>
 * </ul>
 * 
 * <p>Additionally, this class provides utility methods for common operations like null checking,
 * number rounding, text parsing, and formatting.</p>
 * 
 * @see org.freeplane.plugin.script.FreeplaneScriptBaseClass
 * @since 1.12.x (created on 30 Jan 2024)
 */
public class GroovyStaticImports {
    /** 
     * Utilities for logging messages to Freeplane's log file. Use for debugging and error reporting.
     * @see LogUtils
     */
    public final static LogUtils logger = new LogUtils();
    
    /** 
     * Utilities for GUI operations including dialogs, message boxes, and UI components access.
     * @see UITools
     */
    public final static UITools ui = new UITools();
    
    /** 
     * Utilities for HTML/XML processing including conversion between HTML and plain text.
     * @see HtmlUtils
     */
    public final static HtmlUtils htmlUtils = HtmlUtils.getInstance();
    
    /** 
     * Utilities for text processing, translations, formatting, and string operations.
     * @see TextUtils
     */
    public final static TextUtils textUtils = new TextUtils();
    
    /** 
     * Utilities for menu operations and menu item execution.
     * @see MenuUtils
     */
    public final static MenuUtils menuUtils = new MenuUtils();
    
    /** 
     * Utilities for script-specific operations, particularly useful in utility scripts and add-ons.
     * @see ScriptUtils
     */
    public final static ScriptUtils scriptUtils = new ScriptUtils();
    
    /** 
     * Accessor for Freeplane's configuration properties. Provides access to all configuration settings.
     * <p>Note: In utility scripts and add-on classes, this static instance is the recommended way to access
     * configuration, as the global {@code config} variable is not available when compiling outside Freeplane.</p>
     */
    public final static FreeplaneScriptBaseClass.ConfigProperties config = new FreeplaneScriptBaseClass.ConfigProperties();
    
    /**
     * Executes the given closure while ignoring any cyclic dependencies in formulas.
     * If there are cyclic dependencies, formulas are skipped without warnings or exceptions.
     * 
     * @param <T> the return type of the closure
     * @param closure the operation to execute
     * @return the result of the closure execution
     */
    public static <T> T ignoreCycles(final Supplier<T> closure) {
        return ScriptUtils.ignoreCycles(closure);
    }

	/** returns valueIfNull if value is null and value otherwise. */
	public static Object ifNull(Object value, Object valueIfNull) {
		return value == null ? valueIfNull : value;
	}

	/** rounds a number to integral type. */
    public static Long round(final Double d) {
            if (d == null)
                    return null;
            return Math.round(d);
    }

    /** round to the given number of decimal places: <code>round(0.1234, 2) &rarr; 0.12</code> */
    public static Double round(final Double d, final int precision) {
            if (d == null)
                    return d;
            double factor = 1;
            for (int i = 0; i < precision; i++) {
                    factor *= 10.;
            }
            return Math.round(d * factor) / factor;
    }

    /** parses text to the proper data type, if possible, setting format to the standard. Parsing is configured via
     * config file scanner.xml
     * <pre>
     * assert parse('2012-11-30') instanceof Date
     * assert parse('1.22') instanceof Number
     * // if parsing fails the original string is returned
     * assert parse('2012XX11-30') == '2012XX11-30'
     *
     * def d = parse('2012-10-30')
     * c.statusInfo = "${d} is ${new Date() - d} days ago"
     * </pre> */
    public static Object parse(final String text) {
        return ScannerController.getController().parse(text);
    }

    /** uses formatString to return a FormattedObject.
     * <p><em>Note:</em> If you want to format the node core better use the format node attribute instead:
     * <pre>
     * node.object = new Date()
     * node.format = 'dd/MM/yy'
     * </pre>
     * @return {@link IFormattedObject} if object is formattable and the unchanged object otherwise. */
    public static Object format(final Object object, final String formatString) {
        return FormatController.format(object, formatString);
    }

    /** Applies default date-time format for dates or default number format for numbers. All other objects are left unchanged.
     * @return {@link IFormattedObject} if object is formattable and the unchanged object otherwise. */
    public static Object format(final Object object) {
        return FormatController.formatUsingDefault(object);
    }

    /** Applies default date format (instead of standard date-time) format on the given date.
     * @return {@link IFormattedObject} if object is formattable and the unchanged object otherwise. */
    public static Object formatDate(final Date date) {
        final String format = FormatController.getController().getDefaultDateFormat().toPattern();
        return FormatController.format(date, format);
    }

    /** formats according to the internal standard, that is the conversion will be reversible
     * for types that are handled special by the scripting api namely Dates and Numbers.
     * @see Convertible#toString(Object) */
    public static String toString(final Object o) {
        return Convertible.toString(o);
    }
}
