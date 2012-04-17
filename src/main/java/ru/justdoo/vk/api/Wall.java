package ru.justdoo.vk.api;

import com.google.gson.Gson;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import ru.justdoo.vk.basic.App;
import ru.justdoo.vk.basic.Auth;
import ru.justdoo.vk.json.Post;
import ru.justdoo.vk.util.Text;
import ru.justdoo.vk.util.VkMethod;

public final class Wall {
    private static final Logger logger = Logger.getLogger(Wall.class);

    private final App app;
    private final Auth auth;

    public Wall(App app, Auth auth) {
        this.app = app;
        this.auth = auth;
    }

    public List<Post> posts() {
        final String text = new VkMethod(app, auth).
                parameter("method", "wall.get").
                parameter("owner_id", "77530936").
                parameter("filter", "owner"). // just todo doesn't work
                parameter("count", "100").
                call();
        final Gson gson = new Gson();
        final Map map = gson.fromJson(text, Map.class);
        logger.info("json:\n" + Text.format(map));
        final List list = (List) map.get("response");
        final List<Post> posts = new LinkedList<Post>();
        for (final Object item : list.subList(1, list.size())) {
            posts.add(new Post((Map) item));
        }
        return posts;
    }
}
