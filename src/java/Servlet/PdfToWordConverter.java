package Servlet;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;
import java.util.regex.Pattern;

public class PdfToWordConverter {

    private static final Pattern ASPOSE_EVAL =
            Pattern.compile("(?i)\\s*Evaluation Only\\.?\\s*Created with Aspose\\.PDF\\..*");

    public static void convert(InputStream pdfInput, OutputStream wordOutput) throws IOException {

        byte[] pdfBytes = toBytes(pdfInput);

        try (PDDocument pdf = Loader.loadPDF(pdfBytes);
             XWPFDocument docx = new XWPFDocument()) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(pdf);
            text = removeAsposeEvaluationLine(text);

            String[] lines = text.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i] == null ? "" : lines[i].trim();

                XWPFParagraph p = docx.createParagraph();
                if (!line.isEmpty()) {
                    writeLongText(p, line, 1000);
                }
            }

            docx.write(wordOutput);
        }
    }

    private static byte[] toBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    private static String removeAsposeEvaluationLine(String text) {
        if (text == null) return "";
        String[] lines = text.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i] == null ? "" : lines[i];
            if (!ASPOSE_EVAL.matcher(line).matches()) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static void writeLongText(XWPFParagraph p, String text, int chunkSize) {
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            p.createRun().setText(text.substring(i, end));
        }
    }
}
