package Servlet;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.util.*;

public class WordToPdfConverter {

    public static void convert(InputStream wordInput, OutputStream pdfOutput) throws IOException {

        try (XWPFDocument doc = new XWPFDocument(wordInput);
             PDDocument pdf = new PDDocument()) {

            PDFont font = loadFont(pdf);

            float fontSize = 12f;
            float leading = 1.4f * fontSize;
            float margin = 50f;

            PageState state = newPage(pdf, font, fontSize, margin);
            float maxWidth = state.page.getMediaBox().getWidth() - (2 * margin);

            // BODY
            state = writeBodyElements(doc.getBodyElements(), pdf, state, font, fontSize, leading, margin, maxWidth);

            // HEADERS
            for (XWPFHeader h : doc.getHeaderList()) {
                state = writeBodyElements(h.getBodyElements(), pdf, state, font, fontSize, leading, margin, maxWidth);
            }

            // FOOTERS
            for (XWPFFooter f : doc.getFooterList()) {
                state = writeBodyElements(f.getBodyElements(), pdf, state, font, fontSize, leading, margin, maxWidth);
            }

            state.content.endText();
            state.content.close();

            pdf.save(pdfOutput);
            pdfOutput.flush();
        }
    }

    private static PageState writeBodyElements(List<IBodyElement> elements,
                                               PDDocument pdf,
                                               PageState state,
                                               PDFont font, float fontSize,
                                               float leading, float margin,
                                               float maxWidth) throws IOException {

        for (IBodyElement element : elements) {

            if (element instanceof XWPFParagraph) {
                String text = paragraphText((XWPFParagraph) element);

                if (text.trim().isEmpty()) {
                    state = ensureSpace(pdf, state, leading, font, fontSize, margin);
                    state.content.newLineAtOffset(0, -leading);
                    state.y -= leading;
                    continue;
                }

                for (String line : wrap(text, font, fontSize, maxWidth)) {
                    line = cleanForShowText(line);
                    if (line.isEmpty()) line = " ";

                    state = ensureSpace(pdf, state, leading, font, fontSize, margin);
                    state.content.showText(line);
                    state.content.newLineAtOffset(0, -leading);
                    state.y -= leading;
                }

                // espace après paragraphe
                state = ensureSpace(pdf, state, leading, font, fontSize, margin);
                state.content.newLineAtOffset(0, -leading * 0.4f);
                state.y -= (leading * 0.4f);
            }

            if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;

                // espace avant tableau
                state = ensureSpace(pdf, state, leading, font, fontSize, margin);
                state.content.newLineAtOffset(0, -leading * 0.6f);
                state.y -= (leading * 0.6f);

                for (XWPFTableRow row : table.getRows()) {
                    List<String> cellTexts = new ArrayList<>();

                    for (XWPFTableCell cell : row.getTableCells()) {
                        cellTexts.add(tableCellText(cell));
                    }

                    String rowText = join(cellTexts, " | ");

                    for (String line : wrap(rowText, font, fontSize, maxWidth)) {
                        line = cleanForShowText(line);
                        if (line.isEmpty()) line = " ";

                        state = ensureSpace(pdf, state, leading, font, fontSize, margin);
                        state.content.showText(line);
                        state.content.newLineAtOffset(0, -leading);
                        state.y -= leading;
                    }
                }

                // espace après tableau
                state = ensureSpace(pdf, state, leading, font, fontSize, margin);
                state.content.newLineAtOffset(0, -leading * 0.6f);
                state.y -= (leading * 0.6f);
            }
        }
        return state;
    }

    // -------- Text extraction (pi solid pase getText)
    private static String paragraphText(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun r : p.getRuns()) {
            String t = r.text();
            if (t != null) sb.append(t);
        }
        return clean(sb.toString());
    }

    private static String tableCellText(XWPFTableCell cell) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : cell.getParagraphs()) {
            String t = paragraphText(p);
            if (!t.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(t);
            }
        }
        return clean(sb.toString()).replace("\n", " ").replace("\r", " ").trim();
    }

    // -------- Font (mete font la nan resources)
    private static PDFont loadFont(PDDocument pdf) throws IOException {
        InputStream fontStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("fonts/DejaVuSans.ttf");

        if (fontStream != null) {
            try {
                return PDType0Font.load(pdf, fontStream, true); // embed
            } finally {
                fontStream.close();
            }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private static String join(List<String> items, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static class PageState {
        PDPage page;
        PDPageContentStream content;
        float y;
    }

    private static PageState newPage(PDDocument pdf, PDFont font, float fontSize, float margin) throws IOException {
        PageState st = new PageState();
        st.page = new PDPage(PDRectangle.LETTER);
        pdf.addPage(st.page);

        st.content = new PDPageContentStream(pdf, st.page);
        st.content.setFont(font, fontSize);
        st.content.beginText();

        float startY = st.page.getMediaBox().getHeight() - margin;
        st.y = startY;
        st.content.newLineAtOffset(margin, startY);
        return st;
    }

    private static PageState ensureSpace(PDDocument pdf, PageState st, float leading,
                                        PDFont font, float fontSize, float margin) throws IOException {
        if (st.y - leading <= margin) {
            st.content.endText();
            st.content.close();
            return newPage(pdf, font, fontSize, margin);
        }
        return st;
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace("\t", " ")
                .replace("\u0000", "")
                .replaceAll("\\p{Cntrl}", " ")
                .trim();
    }

    private static String cleanForShowText(String s) {
        if (s == null) return "";
        return s.replace("\r", " ")
                .replace("\n", " ")
                .replace("\u0000", "")
                .replaceAll("\\p{Cntrl}", " ")
                .trim();
    }

    private static List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = (line.length() == 0) ? w : (line + " " + w);

            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) lines.add(line.toString());

                if (textWidth(w, font, fontSize) > maxWidth) {
                    lines.addAll(breakLongWord(w, font, fontSize, maxWidth));
                    line.setLength(0);
                } else {
                    line.setLength(0);
                    line.append(w);
                }
            }
        }

        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static List<String> breakLongWord(String word, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            String candidate = cur.toString() + word.charAt(i);
            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                cur.append(word.charAt(i));
            } else {
                if (cur.length() > 0) parts.add(cur.toString());
                cur.setLength(0);
                cur.append(word.charAt(i));
            }
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts;
    }

    private static float textWidth(String text, PDFont font, float fontSize) throws IOException {
        return (font.getStringWidth(text) / 1000f) * fontSize;
    }
}
