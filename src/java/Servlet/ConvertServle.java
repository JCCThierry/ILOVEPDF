package Servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import model.ConversionStore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@WebServlet("/convert")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 80L * 1024 * 1024,
        maxRequestSize = 90L * 1024 * 1024
)
public class ConvertServle extends HttpServlet {

    private static final Path BASE_DIR   = Paths.get(System.getProperty("java.io.tmpdir"), "converter");
    private static final Path UPLOAD_DIR = BASE_DIR.resolve("uploads");
    private static final Path OUTPUT_DIR = BASE_DIR.resolve("outputs");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Files.createDirectories(UPLOAD_DIR);
        Files.createDirectories(OUTPUT_DIR);

        String conv = request.getParameter("conversionType");
        if (conv == null || conv.trim().isEmpty()) {
            response.sendError(400, "conversionType manke.");
            return;
        }

        Part part = request.getPart("file");
        if (part == null || part.getSize() == 0) {
            response.sendError(400, "Pa gen fichye (file).");
            return;
        }

        String originalName = safeFileName(part.getSubmittedFileName());
        String lower = originalName.toLowerCase();

        // Validate extension vs conv
        if ((conv.equals("pdfToWord") || conv.equals("pdfToExcel")) && !lower.endsWith(".pdf")) {
            response.sendError(400, "Pou " + conv + " fòk se PDF.");
            return;
        }
        if ((conv.equals("wordToPdf") || conv.equals("wordToExcel")) &&
                !(lower.endsWith(".doc") || lower.endsWith(".docx"))) {
            response.sendError(400, "Pou " + conv + " fòk se Word (.doc/.docx).");
            return;
        }

        // Determine output extension + contentType
        String outExt;
        String outContentType;

        switch (conv) {
            case "pdfToWord":
                outExt = ".docx";
                outContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                break;
            case "wordToPdf":
                outExt = ".pdf";
                outContentType = "application/pdf";
                break;
            case "wordToExcel":
            case "pdfToExcel":
                outExt = ".xlsx";
                outContentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            default:
                response.sendError(400, "conversionType pa valide: " + conv);
                return;
        }

        String baseName = removeExtension(originalName);
        String downloadName = baseName + outExt;

        String token = UUID.randomUUID().toString().replace("-", "");
        Path uploadedPath = UPLOAD_DIR.resolve(token + "_" + originalName);
        Path outputPath   = OUTPUT_DIR.resolve(token + "_" + downloadName);

        // Save upload
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, uploadedPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Convert
        try (InputStream in = Files.newInputStream(uploadedPath);
             OutputStream out = Files.newOutputStream(outputPath,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            if (conv.equals("pdfToWord")) {
                PdfToWordConverter.convert(in, out);
            } else if (conv.equals("wordToPdf")) {
                WordToPdfConverter.convert(in, out);
            } else if (conv.equals("wordToExcel")) {
                WordToExcelConverter.convertDocxToXlsx(in, out);
            } else if (conv.equals("pdfToExcel")) {
                // PdfToExcelConverter.convert(in, out);
                throw new ServletException("pdfToExcel pa implemante nan converter la.");
            }
        } finally {
            // optional: delete upload file to save space
            try { Files.deleteIfExists(uploadedPath); } catch (Exception ignored) {}
        }

        // ✅ Store result for token-based download/preview
        ConversionStore.put(new ConversionStore.Meta(token, downloadName, outContentType, outputPath));

        // ✅ Return JSON (NO attachment)
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        String json = "{\"success\":true,\"token\":\"" + token + "\",\"filename\":\"" + jsonEscape(downloadName) + "\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    private static String safeFileName(String submitted) {
        if (submitted == null) return "file";
        String name = Paths.get(submitted).getFileName().toString();
        name = name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        return name.isEmpty() ? "file" : name;
    }

    private static String removeExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
