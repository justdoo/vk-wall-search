package ru.justdoo.vk;

import java.util.List;
import org.apache.log4j.Logger;
import ru.justdoo.vk.api.Wall;
import ru.justdoo.vk.basic.App;
import ru.justdoo.vk.basic.Auth;
import ru.justdoo.vk.auth.Connector;
import ru.justdoo.vk.json.Post;
import ru.justdoo.vk.util.Text;

public final class Application {
    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            final App app = new App(2649257, "Gf8HxXp6JIN3IWKFNviN");
            final Auth auth = new Connector(app, "test@gmail.com", "test").login();
            logger.info(auth);
            final List<Post> posts = new Wall(app, auth).posts();
            logger.info("posts:\n" + Text.format(posts));
            logger.info("-");
        } catch (Exception e) {
            logger.error("login failed", e);
        }
    }
}
