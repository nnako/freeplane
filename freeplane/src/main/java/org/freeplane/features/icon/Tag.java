/*
 * Created on 2 Apr 2024
 *
 * author dimitry
 */
package org.freeplane.features.icon;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.freeplane.core.ui.components.HSLColorConverter;
import org.freeplane.core.util.LineComparator;

public class Tag implements Comparable<Tag>{
    public final static Tag EMPTY_TAG = new Tag("", Color.BLACK);
    public final static Tag REMOVED_TAG = new Tag(" removed tag ", Color.BLACK);
    private final String content;
    private Color color;
    private Tag colorChainTag;

    public static Color getDefaultColor(String content) {
        if(content.isEmpty())
           return Color.BLACK;
       long crc = computeCRC32(content);
       return HSLColorConverter.generateColorFromLong(crc);
    }

    public Tag(String content) {
        this(content, getDefaultColor(content));
    }

    public Tag(String content, Color color) {
        this.content = content;
        this.color = color;
    }

    public Tag getColorChainTag() {
        return colorChainTag;
    }

    public void setColorChainTag(Tag colorChainTag) {
        this.colorChainTag = colorChainTag;
    }

    public String getContent() {
        return content;
    }

    @Override
    public int compareTo(Tag o) {
        return LineComparator.compareLinesParsingNumbers(content, o.content);
    }

    public boolean isEmpty() {
       return content.isEmpty();
    }

    public void setColor(Color color) {
        this.color = color;
        if(colorChainTag != null && colorChainTag.getColor() != color)
            colorChainTag.setColor(color);
    }

    public Color getColor() {
        return color;
    }

    public Color getDefaultColor() {
        return getDefaultColor(content);
    }

    public Tag copy() {
        return new Tag(content, color);
    }

    public List<Tag> categoryTags(String tagCategorySeparator) {
        if(tagCategorySeparator.isEmpty() || isEmpty())
            return Collections.singletonList(this);
        final String[] categories = getContent().split(Pattern.quote(tagCategorySeparator));
        if(categories == null || categories.length < 2)
            return Collections.singletonList(this);
        return Stream.of(categories).map(content -> new Tag(content, color))
                .collect(Collectors.toList());
    }

    public Tag withoutCategories(String tagCategorySeparator) {
        if(tagCategorySeparator.isEmpty() || isEmpty())
            return this;
        int shortTagBegin = content.lastIndexOf(tagCategorySeparator);
        if(shortTagBegin < 0)
            return this;
        return new Tag(content.substring(shortTagBegin + tagCategorySeparator.length()), color);
    }


    private static long computeCRC32(String input) {
        CRC32 crc = new CRC32();
        crc.update(input.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tag other = (Tag) obj;
        return Objects.equals(content, other.content);
    }

    @Override
    public String toString() {
        return content;
    }

    public Tag updateSeparator(String initialSeparator, String currentSeparator) {
        if(initialSeparator.equals(currentSeparator) || ! getContent().contains(initialSeparator)) {
            return this;
        }
        else {
            return new Tag(content.replace(initialSeparator, currentSeparator), color);
        }
    }

	public Tag qualifiedTag() {
		return colorChainTag != null
				&& colorChainTag.getContent().length() > getContent().length()
				? colorChainTag
				: this;
	}
}
