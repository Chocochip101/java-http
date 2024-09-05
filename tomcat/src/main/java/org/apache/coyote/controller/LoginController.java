package org.apache.coyote.controller;

import com.techcourse.db.InMemoryUserRepository;
import com.techcourse.model.User;
import java.util.HashMap;
import java.util.Map;
import org.apache.catalina.Manager;
import org.apache.coyote.Session;
import org.apache.coyote.SessionManager;
import org.apache.coyote.http11.HttpCookie;
import org.apache.coyote.http11.HttpHeader;
import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;
import org.apache.coyote.http11.HttpStatusCode;
import org.apache.coyote.http11.MimeType;

public class LoginController implements Controller {

    private static final String ACCOUNT_KEY = "account";
    private static final String PASSWORD_KEY = "password";

    private final InMemoryUserRepository userRepository;

    public LoginController() {
        userRepository = new InMemoryUserRepository();
    }

    @Override
    public HttpResponse run(HttpRequest request) {
        if (request.getBody().isEmpty()) {
            return redirectLoginPage();
        }
        String body = request.getBody().get();
        Map<String, String> parsedBody = parseBody(body);

        String account = parsedBody.get(ACCOUNT_KEY);
        String password = parsedBody.get(PASSWORD_KEY);

        User user = userRepository.findByAccount(account)
                .orElseThrow();

        if (user.checkPassword(password)) {
            HttpHeader header = new HttpHeader();
            Manager manager = SessionManager.getInstance();

            if (!isSessionExists(request)) {
                Session session = Session.createRandomSession();
                manager.add(session);
                session.setAttribute("user", user.getAccount());
                header.setCookie(HttpCookie.ofJSessionId(session.getId()));
            }
            return redirectDefaultPage(header);
        }
        return redirectUnauthorizedPage();
    }

    private HttpResponse redirectLoginPage() {
        HttpHeader header = new HttpHeader();
        header.setLocation("/login.html");
        header.setContentType(MimeType.HTML);
        return new HttpResponse(HttpStatusCode.FOUND, header);
    }

    private Map<String, String> parseBody(String query) {
        Map<String, String> result = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = keyValue[0];
            String value = keyValue[1];
            result.put(key, value);
        }
        return result;
    }

    private boolean isSessionExists(HttpRequest request) {
        HttpHeader requestHeaders = request.getHeaders();
        return requestHeaders.existsSession();
    }

    private HttpResponse redirectDefaultPage(HttpHeader header) {
        header.setLocation("/index.html");
        header.setContentType(MimeType.HTML);
        return new HttpResponse(HttpStatusCode.FOUND, header);
    }

    private HttpResponse redirectUnauthorizedPage() {
        HttpHeader header = new HttpHeader();
        header.setLocation("/401.html");
        header.setContentType(MimeType.HTML);
        return new HttpResponse(HttpStatusCode.FOUND, header);
    }
}
