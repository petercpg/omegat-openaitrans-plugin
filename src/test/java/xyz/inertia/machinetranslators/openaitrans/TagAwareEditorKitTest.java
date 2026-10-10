package xyz.inertia.machinetranslators.openaitrans;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.junit.Test;

import static org.junit.Assert.*;

public class TagAwareEditorKitTest {

    static class TagAwareHTMLEditorKit extends HTMLEditorKit {
        private static final Pattern ENTRY_PATTERN =
                Pattern.compile("(<div id=\"\\d+\">)(.*?)(<div class=\"engine\">)", Pattern.DOTALL);

        private static final Pattern TAG_PATTERN = Pattern.compile("<(/?[a-zA-Z_:][a-zA-Z0-9_.:-]*(?:\\s+[^<>]*)?/?)>");

        private final JEditorPane targetPane;

        public TagAwareHTMLEditorKit(JEditorPane pane, StyleSheet styleSheet) {
            this.targetPane = pane;
            if (styleSheet != null) {
                setStyleSheet(styleSheet);
            }
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
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '&') {
                    out.append("&amp;");
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

    @Test
    public void testTagAwareEditorKit() throws Exception {
        JEditorPane pane = new JEditorPane();
        TagAwareHTMLEditorKit kit = new TagAwareHTMLEditorKit(pane, null);
        pane.setEditorKit(kit);

        String originalResult1 =
                "視組態而定： <guibutton>Set category</guibutton> 圖示 <placeholder type=\"inlinemediaobject\" id=\"0\"/>..";
        String originalResult2 = "另一個範例： <b>粗體文字</b> 以及 <t0/> 標記 & 測試。";

        String rawHtml = "<html>"
                + "<div id=\"0\">" + originalResult1 + "<div class=\"engine\">&lt;AI LLM Translate&gt;</div></div>"
                + "<div id=\"1\">" + originalResult2 + "<div class=\"engine\">&lt;Google Translate&gt;</div></div>"
                + "</html>";

        pane.setText(rawHtml);

        HTMLDocument doc = (HTMLDocument) pane.getDocument();
        Element root = doc.getDefaultRootElement();
        Element div0 = doc.getElement(root, HTML.Attribute.ID, "0");
        assertNotNull("div 0 should exist", div0);
        Element div1 = doc.getElement(root, HTML.Attribute.ID, "1");
        assertNotNull("div 1 should exist", div1);

        String plainText = doc.getText(0, doc.getLength());
        System.out.println("Document plain text:\n" + plainText);

        assertTrue("Should contain <guibutton>", plainText.contains("<guibutton>Set category</guibutton>"));
        assertTrue(
                "Should contain <placeholder",
                plainText.contains("<placeholder type=\"inlinemediaobject\" id=\"0\"/>"));
        assertTrue("Should contain <b>", plainText.contains("<b>粗體文字</b>"));
        assertTrue("Should contain <t0/>", plainText.contains("<t0/>"));
        assertTrue("Should contain &", plainText.contains("& 測試"));

        // Check offset matching for highlightSelected
        int startOffset0 = div0.getStartOffset();
        String textInDiv0 = doc.getText(startOffset0, originalResult1.length());
        assertEquals("Highlight length should match originalResult1 exactly", originalResult1, textInDiv0.trim());
    }
}
