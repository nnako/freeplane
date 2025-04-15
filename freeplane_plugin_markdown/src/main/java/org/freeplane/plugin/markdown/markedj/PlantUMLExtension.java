package org.freeplane.plugin.markdown.markedj;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import javax.imageio.ImageIO;

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
import org.freeplane.core.resources.ResourceController;

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

    private static String plantUmlInclude = "";
    static {
        URL plantumlConfigUrl = ResourceController.getResourceController().getResource("plantuml-include.txt");
        if (plantumlConfigUrl != null) {
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(plantumlConfigUrl.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            plantUmlInclude = stringBuilder.toString();
        }
    }
    private static final FileFormatOption FILE_FORMAT_OPTION_PNG_NO_METADATA = new FileFormatOption(FileFormat.PNG, false);

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
        StringBuilder plantUmlStringBuilder = new StringBuilder();
        if (plantUmlInclude.isEmpty()) {
            plantUmlStringBuilder.append(plantUmlToken.getText());
        } else {
            try (BufferedReader plantUmlStringReader = new BufferedReader(new StringReader(plantUmlToken.getText()))) {
                String line;
                int lineIndex = 0;
                while ((line = plantUmlStringReader.readLine()) != null) {
                    if (lineIndex == 1) {
                        plantUmlStringBuilder.append(plantUmlInclude).append("\n");
                    }
                    plantUmlStringBuilder.append(line).append("\n");
                    lineIndex++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        SourceStringReader reader = new SourceStringReader(plantUmlStringBuilder.toString());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DiagramDescription diagramDescription;
        boolean useCache = ImageIO.getUseCache();
        try {
        	ImageIO.setUseCache(false);
            diagramDescription = reader.outputImage(outputStream, FILE_FORMAT_OPTION_PNG_NO_METADATA);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
        	ImageIO.setUseCache(useCache);
        }
        if (diagramDescription != null) {
            String base64String = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return String.format("<div class=\"plantuml\">%n<img src=\"data:image/png;base64,%s\">%n</div>%n", base64String);
        } else {
            return String.format("<pre><code>/!\\ Diagram creation failed /!\\%n%n%s</code></pre>", plantUmlStringBuilder);
        }
    }

}
