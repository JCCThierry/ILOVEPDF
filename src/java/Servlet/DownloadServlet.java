package Servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.ConversionStore;
import java.io.*;
import java.nio.file.Files;

@WebServlet("/download")
public class DownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) { resp.sendError(400, "token manke."); return; }

        ConversionStore.Meta meta = ConversionStore.get(token);
        if (meta == null) { resp.sendError(404, "Fichye a pa jwenn oswa li ekspire."); return; }

        resp.setContentType(meta.contentType);
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + meta.filename.replace("\"","") + "\"");
        resp.setHeader("X-Content-Type-Options", "nosniff");

        try (InputStream in = Files.newInputStream(meta.outputPath);
             OutputStream out = resp.getOutputStream()) {
            in.transferTo(out);
        }
    }
}
