package xyz.inertia.machinetranslators.openaitrans;

import org.omegat.core.Core;
import org.omegat.gui.exttrans.MachineTranslateTextArea;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom HTMLEditorKit for OmegaT's MachineTranslateTextArea.
 *
 * Intercepts HTML rendering of MT results to properly display inline XML/HTML tags
 * (such as &lt;guibutton&gt;, &lt;placeholder .../&gt;, &lt;b0&gt;, etc.) with syntax
 * highlighting instead of letting Swing's HTML parser silently drop unrecognized tags.
 *
 * This keeps the underlying MachineTranslationInfo.result completely intact so that
 * Ctrl+M insertion and OmegaT tag validation work flawlessly without any corrupted markup.
 */
public class TagAwareHTMLEditorKit extends HTMLEditorKit {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagAwareHTMLEditorKit.class);

    private static final Pattern ENTRY_PATTERN =
            Pattern.compile("(<div id=\"\\d+\">)(.*?)(<div class=\"engine\">)", Pattern.DOTALL);

    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?[a-zA-Z_:][a-zA-Z0-9_.:-]*(?:\\s+[^<>]*)?/?)>");

    private static final Pattern ENTITY_PATTERN = Pattern.compile("^&(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);");

    private final JEditorPane targetPane;

    public TagAwareHTMLEditorKit(JEditorPane pane, StyleSheet styleSheet) {
        this.targetPane = pane;
        if (styleSheet != null) {
            setStyleSheet(styleSheet);
        }
    }

    /**
     * Installs TagAwareHTMLEditorKit on OmegaT's MachineTranslateTextArea if not already installed.
     */
    public static void install() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                MachineTranslateTextArea pane = Core.getMachineTranslatePane();
                if (pane != null && !(pane.getEditorKit() instanceof TagAwareHTMLEditorKit)) {
                    EditorKit oldKit = pane.getEditorKit();
                    StyleSheet ss = null;
                    if (oldKit instanceof HTMLEditorKit) {
                        ss = ((HTMLEditorKit) oldKit).getStyleSheet();
                    }
                    TagAwareHTMLEditorKit newKit = new TagAwareHTMLEditorKit(pane, ss);
                    pane.setEditorKit(newKit);
                    LOGGER.info("TagAwareHTMLEditorKit installed successfully on MachineTranslateTextArea");
                }
            } catch (Throwable t) {
                LOGGER.debug("Could not install TagAwareHTMLEditorKit: {}", t.getMessage());
            }
        });
    }

    @Override
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[2048];
        int n;
        while ((n = in.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        String raw = sb.toString();
        String processed = processHtml(raw);
        super.read(new StringReader(processed), doc, pos);
    }

    private String processHtml(String html) {
        if (!html.contains("<div id=\"") || !html.contains("<div class=\"engine\">")) {
            return html;
        }

        boolean isDark = false;
        if (targetPane != null) {
            Color bg = targetPane.getBackground();
            if (bg != null) {
                double luma = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
                isDark = luma < 128;
            }
        }

        String tagColor = isDark ? "#58a6ff" : "#0969da";
        String placeholderColor = isDark ? "#bc8cff" : "#8250df";

        Matcher matcher = ENTRY_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String content = matcher.group(2);
            String suffix = matcher.group(3);

            String formattedContent = formatTranslationContent(content, tagColor, placeholderColor);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + formattedContent + suffix));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String formatTranslationContent(String content, String tagColor, String placeholderColor) {
        Matcher tagMatcher = TAG_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        int lastEnd = 0;
        while (tagMatcher.find()) {
            String textBetween = content.substring(lastEnd, tagMatcher.start());
            sb.append(escapeHtmlText(textBetween));

            String fullTag = tagMatcher.group(0);
            String innerTag = fullTag.substring(1, fullTag.length() - 1);
            boolean isPlaceholder = innerTag.toLowerCase().startsWith("placeholder")
                    || innerTag.toLowerCase().startsWith("/placeholder");
            String color = isPlaceholder ? placeholderColor : tagColor;

            sb.append("<font color=\"")
                    .append(color)
                    .append("\"><b>&lt;")
                    .append(escapeXmlInside(innerTag))
                    .append("&gt;</b></font>");

            lastEnd = tagMatcher.end();
        }
        if (lastEnd < content.length()) {
            sb.append(escapeHtmlText(content.substring(lastEnd)));
        }
        return sb.toString();
    }

    private static String escapeHtmlText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '&') {
                // Check if already an HTML entity
                Matcher m = ENTITY_PATTERN.matcher(text.substring(i));
                if (m.find()) {
                    String entity = m.group(0);
                    out.append(entity);
                    i += entity.length() - 1;
                } else {
                    out.append("&amp;");
                }
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String escapeXmlInside(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&') {
                out.append("&amp;");
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '"') {
                out.append("&quot;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
