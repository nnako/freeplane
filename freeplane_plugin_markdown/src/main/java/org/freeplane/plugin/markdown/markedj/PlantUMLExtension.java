package org.freeplane.plugin.markdown.markedj;

import io.github.gitbucket.markedj.Lexer;
import io.github.gitbucket.markedj.Parser;
import io.github.gitbucket.markedj.extension.Extension;
import io.github.gitbucket.markedj.extension.TokenConsumer;
import io.github.gitbucket.markedj.rule.FindFirstRule;
import io.github.gitbucket.markedj.rule.Rule;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.DiagramDescription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class PlantUMLExtension implements Extension {
    public static String[] EXPRESSIONS = new String[]{
            "(?s)(?m)\\A@startuml\\n.+?@enduml$",
            "(?s)(?m)\\A@startgantt\\n.+?@endgantt$",
            "(?s)(?m)\\A@startchronology\\n.+?@endchronology$",
            "(?s)(?m)\\A@startsalt\\n.+?@endsalt$",
            "(?s)(?m)\\A@startjson\\n.+?@endjson$",
            "(?s)(?m)\\A@startyaml\\n.+?@endyaml$",
            "(?s)(?m)\\A@startebnf\\n.+?@endebnf$",
            "(?s)(?m)\\A@startregex\\n.+?@endregex$",
            "(?s)(?m)\\A@startwbs\\n.+?@endwbs$",
            "(?s)(?m)\\A@startmindmap\\n.+?@endmindmap$",
    };

    private static final List<Rule> RULES = new LinkedList<>();

    static {
        for (String expr : EXPRESSIONS) {
            RULES.add(new FindFirstRule(expr));
        }
    }

    private static final String PRAGMA_LAYOUT_SMETANA = "!pragma layout smetana";
    private static final FileFormatOption FILE_FORMAT_OPTION_PNG = new FileFormatOption(FileFormat.PNG);

    @Override
    public LexResult lex(String src, Lexer.LexerContext context, TokenConsumer consumer) {
        boolean resultMatches = false;
        for (Rule rule : RULES) {
            List<String> cap = rule.exec(src);
            if (!cap.isEmpty()) {
                String plantUmlCode = cap.get(0);
                context.pushToken(new PlantUMLToken(plantUmlCode));
                src = src.substring(plantUmlCode.length());
                resultMatches = true;
                break;
            }
        }
        return new LexResult(src, resultMatches);
    }

    @Override
    public boolean handlesToken(String tokenType) {
        return PlantUMLToken.TYPE.equals(tokenType);
    }

    @Override
    public String parse(Parser.ParserContext context, Function<Parser.ParserContext, String> tok) {
        PlantUMLToken plantUmlToken = (PlantUMLToken) context.currentToken();
        String plantUmlCode = plantUmlToken.getText();
        final List<String> plantumlList = Arrays.asList(plantUmlCode.split("\n"));
        boolean smetanaFound = plantumlList.stream().map(String::trim).anyMatch(PRAGMA_LAYOUT_SMETANA::equals);
        if (!smetanaFound) {
            LinkedList<String> list = new LinkedList<>(plantumlList);
            list.add(1, PRAGMA_LAYOUT_SMETANA);
            plantUmlCode = String.join("\n", list);
        }
        SourceStringReader reader = new SourceStringReader(plantUmlCode);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DiagramDescription diagramDescription;
        try {
            diagramDescription = reader.outputImage(outputStream, FILE_FORMAT_OPTION_PNG);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (diagramDescription != null) {
            String base64String = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return String.format("<div class=\"plantuml\"><img src=\"data:image/png;base64,%s\"></div>%n", base64String);
        } else {
            return String.format("<pre><code>/!\\ Diagram creation failed /!\\%n%n%s</code></pre>", plantUmlCode);
        }
    }

}
