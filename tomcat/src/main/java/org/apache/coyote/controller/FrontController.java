package org.apache.coyote.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.coyote.http11.HttpRequest;
import org.apache.coyote.http11.HttpResponse;
import org.apache.coyote.http11.HttpStatusCode;
import org.apache.coyote.http11.MimeType;
import org.apache.coyote.util.FileExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrontController {

    private static final Logger log = LoggerFactory.getLogger(FrontController.class);
    private final Map<String, Controller> controllers = new HashMap<>();

    public FrontController() {
        controllers.put("/login", new LoginController());
        controllers.put("/register", new RegisterController());
    }

    public HttpResponse dispatch(HttpRequest request) {
        log(request);
        String path = request.getPath();
        if (FileExtension.isFileExtension(path)) {
            String resourcePath = "static" + path;
            try {
                Path filePath = Paths.get(getClass().getClassLoader().getResource(resourcePath).toURI());
                MimeType mimeType = MimeType.from(FileExtension.from(path));
                byte[] body = Files.readAllBytes(filePath);
                return new HttpResponse(HttpStatusCode.OK, mimeType, body);
            } catch (URISyntaxException | IOException e) {
                return new HttpResponse(HttpStatusCode.OK, "No File Found".getBytes());
            }
        }
        Controller controller = getController(path);
        return controller.run(request);
    }

    private static void log(HttpRequest request) {
        log.info("method = {}, path = {}, url = {}", request.getMethod(), request.getPath(), request.getUrl());
        for (String key : request.getQueryMap().keySet()) {
            log.info("key = {}, value = {}", key, request.getQueryMap().get(key));
        }
        if (request.getBody().isPresent()) {
            log.info(request.getBody().get());
        }
    }


    public Controller getController(String path) {
        Controller controller = controllers.get(path);
        if (controller == null) {
            return new NotFoundController();
        }
        return controllers.get(path);
    }
}
