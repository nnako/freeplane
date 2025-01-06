package org.freeplane.plugin.markdown.markedj;

import io.github.gitbucket.markedj.token.Token;

public class PlantUMLToken implements Token {
    protected static String TYPE = "PlantUMLToken";
    private final String text;

    public PlantUMLToken(String text) {
        this.text = text;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getText() {
        return text;
    }
}
