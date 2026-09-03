package com.recruitment.server;

import com.recruitment.dao.NotificationDAO;
import com.recruitment.model.Notification;
import com.recruitment.util.JSONUtil;
import com.recruitment.util.ResponseHelper;
import com.recruitment.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP Handler for System Notifications.
 */
public class NotificationHandler implements HttpHandler {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (ResponseHelper.handleCorsPreflight(exchange)) {
            return;
        }

        SessionManager.UserSession session = SessionManager.getSessionFromExchange(exchange);
        if (session == null) {
            ResponseHelper.sendError(exchange, 401, "Authentication required.");
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                List<Notification> list = notificationDAO.getNotificationsByUser(session.getUserId());
                int unreadCount = notificationDAO.getUnreadCount(session.getUserId());
                Map<String, Object> resp = new HashMap<>();
                resp.put("notifications", list);
                resp.put("unreadCount", unreadCount);
                ResponseHelper.sendSuccess(exchange, "Notifications retrieved", resp);

            } else if ("PUT".equals(method)) {
                if (path.endsWith("/read-all")) {
                    notificationDAO.markAllAsRead(session.getUserId());
                    ResponseHelper.sendSuccess(exchange, "All notifications marked as read.");
                } else {
                    String body = ResponseHelper.readBody(exchange);
                    Map<String, Object> data = JSONUtil.parseObject(body);
                    int id = JSONUtil.getInt(data, "notificationId", 0);
                    if (id > 0) {
                        notificationDAO.markAsRead(id, session.getUserId());
                        ResponseHelper.sendSuccess(exchange, "Notification marked as read.");
                    } else {
                        notificationDAO.markAllAsRead(session.getUserId());
                        ResponseHelper.sendSuccess(exchange, "All notifications marked as read.");
                    }
                }
            } else {
                ResponseHelper.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            System.err.println("[NotificationHandler] Error: " + e.getMessage());
            ResponseHelper.sendError(exchange, 500, "Internal error processing notifications.");
        }
    }
}
