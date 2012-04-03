package ru.justdoo.vk.ws;

import org.apache.log4j.Logger;
import ru.justdoo.vk.auth.Auth;
import ru.justdoo.vk.auth.Connector;

public final class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            final Auth auth = Connector.login("user", "password");
            System.out.println(auth);
        } catch (Exception e) {
            logger.error("login failed", e);
        }
    }
}
