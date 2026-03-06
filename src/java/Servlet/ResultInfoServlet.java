package Servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.ConversionStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet("/result-info")
public class ResultInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) { resp.sendError(400, "token manke."); return; }

        ConversionStore.Meta meta = ConversionStore.get(token);
        if (meta == null) { resp.sendError(404, "Pa jwenn / ekspire."); return; }

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        String json = "{\"filename\":\"" + meta.filename.replace("\"","\\\"") +
                "\",\"contentType\":\"" + meta.contentType.replace("\"","\\\"") + "\"}";
        resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
