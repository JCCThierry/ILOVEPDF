package Servlet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.util.List;

public class WordToExcelConverter {

    public static void convertDocxToXlsx(InputStream wordInput, OutputStream excelOutput) throws IOException {

        try (XWPFDocument doc = new XWPFDocument(wordInput);
             XSSFWorkbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Contenu Word");

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);

            int rowIndex = 0;
            int maxCol = 1;

            for (IBodyElement element : doc.getBodyElements()) {

                if (element instanceof XWPFParagraph) {
                    String text = safeText(((XWPFParagraph) element).getText());
                    if (!text.trim().isEmpty()) { // ✅ Java 8
                        Row row = sheet.createRow(rowIndex++);
                        Cell cell = row.createCell(0);
                        cell.setCellValue(text);
                        cell.setCellStyle(wrapStyle);
                    }
                }

                if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;

                    if (rowIndex > 0) rowIndex++;

                    for (XWPFTableRow tRow : table.getRows()) {
                        Row row = sheet.createRow(rowIndex++);
                        List<XWPFTableCell> cells = tRow.getTableCells();
                        maxCol = Math.max(maxCol, cells.size());

                        for (int c = 0; c < cells.size(); c++) {
                            Cell cell = row.createCell(c);
                            String cellText = extractCellText(cells.get(c));
                            cell.setCellValue(cellText);
                            cell.setCellStyle(wrapStyle);
                        }
                    }
                    rowIndex++;
                }
            }

            for (int c = 0; c < Math.min(maxCol, 30); c++) {
                sheet.autoSizeColumn(c);
            }

            workbook.write(excelOutput);
        }
    }

    private static String safeText(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String extractCellText(XWPFTableCell cell) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : cell.getParagraphs()) {
            String t = safeText(p.getText());
            if (!t.trim().isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(t);
            }
        }
        return sb.toString();
    }
}
