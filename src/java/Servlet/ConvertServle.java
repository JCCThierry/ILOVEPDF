package Servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.URLEncoder;
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

    private static final Path BASE_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "converter");
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

        Part part = request.getPart("file"); // FormData.append("file", ...)
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
        String downloadName = baseName + outExt; // ✅ menm non, nouvo ext

        String token = UUID.randomUUID().toString();
        Path uploadedPath = UPLOAD_DIR.resolve(token + "_" + originalName);
        Path outputPath = OUTPUT_DIR.resolve(token + "_" + downloadName);

        // Save upload
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, uploadedPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Convert
        try (InputStream in = Files.newInputStream(uploadedPath);
             OutputStream out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            if (conv.equals("pdfToWord")) {
                PdfToWordConverter.convert(in, out);
            } else if (conv.equals("wordToPdf")) {
                WordToPdfConverter.convert(in, out);
            } else if (conv.equals("wordToExcel")) {
                WordToExcelConverter.convertDocxToXlsx(in, out);
            } 
        }

        // Download response
        response.setContentType(outContentType);
        response.setHeader("Content-Disposition", contentDisposition(downloadName));
        response.setHeader("X-Content-Type-Options", "nosniff");

        try (InputStream fin = Files.newInputStream(outputPath);
             OutputStream fout = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fin.read(buf)) != -1) fout.write(buf, 0, n);
            fout.flush();
        }
    }

    private static String safeFileName(String submitted) {
        if (submitted == null) return "file";
        String name = Paths.get(submitted).getFileName().toString();
        name = name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        if (name.isEmpty()) name = "file";
        return name;
    }

    private static String removeExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    private static String contentDisposition(String filename) {
        String clean = filename.replace("\"", "");
        String encoded = URLEncoder.encode(clean, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + clean + "\"; filename*=UTF-8''" + encoded;
    }
}
