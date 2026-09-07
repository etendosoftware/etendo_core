package com.etendoerp.platform.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import com.etendoerp.platform.fixture.Category;
import com.etendoerp.platform.fixture.Request;

/** Small authenticated validation endpoint; not a production authentication system. */
public final class RequestServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String authorization = request.getHeader("Authorization");
        var properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        String role = matches(authorization, properties.getProperty("platform.validation.token")) ? "R1"
                : matches(authorization, properties.getProperty("platform.validation.readOnlyToken")) ? "R_READ" : null;
        if (role == null) {
            response.setStatus(401);
            response.getWriter().println("Authentication required");
            return;
        }
        if (!request.getMethod().equals("GET") && !request.getMethod().equals("POST")) {
            response.setStatus(405);
            return;
        }
        String title = request.getParameter("title");
        if ((request.getMethod().equals("POST") && title == null)
                || (title != null && !title.matches("[A-Za-z0-9 ._-]{1,100}"))) {
            response.setStatus(400);
            return;
        }
        try {
            OBContext.setOBContext("U1", role, "C1", "O1", "en_US");
            String body;
            if (request.getMethod().equals("POST")) {
                Request entity = new Request();
                entity.setId(UUID.randomUUID().toString().replace("-", ""));
                entity.setNewOBObject(true);
                entity.setTitle(title);
                entity.setActive(true);
                entity.setCategory(OBDal.getInstance().get(Category.class, "GENERAL"));
                OBDal.getInstance().save(entity);
                body = entity.getId() + "\t" + entity.getTitle() + "\n";
            } else {
                var query = OBDal.getInstance().createQuery(Request.class, title == null ? "" : "title=:title");
                if (title != null) query.setNamedParameter("title", title);
                query.setMaxResult(100);
                StringBuilder result = new StringBuilder();
                for (Request entity : query.list()) result.append(entity.getId()).append('\t').append(entity.getTitle()).append('\n');
                body = result.toString();
            }
            OBDal.getInstance().commitAndClose();
            response.setStatus(request.getMethod().equals("POST") ? 201 : 200);
            response.getWriter().print(body);
        } catch (OBSecurityException denied) {
            response.setStatus(403);
            response.getWriter().println("Access denied");
        } catch (RuntimeException failure) {
            getServletContext().log("Request transaction failed", failure);
            response.setStatus(500);
            response.getWriter().println("Request failed");
        } finally {
            try {
                if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose();
            } finally {
                OBContext.setOBContext((OBContext) null);
                SessionHandler.deleteSessionHandler();
                org.openbravo.database.SessionInfo.init();
            }
        }
    }

    private static boolean matches(String authorization, String token) {
        return authorization != null && token != null && MessageDigest.isEqual(
                authorization.getBytes(StandardCharsets.UTF_8), ("Bearer " + token).getBytes(StandardCharsets.UTF_8));
    }
}
