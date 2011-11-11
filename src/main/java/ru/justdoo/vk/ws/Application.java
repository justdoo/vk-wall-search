package ru.justdoo.vk.ws;

import org.apache.log4j.Logger;
import ru.justdoo.vk.auth.Connector;
import ru.justdoo.vk.MessageException;

public final class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            new Connector().login("user", "password");
        } catch (MessageException e) {
            logger.error("error", e);
        }
    }
}
