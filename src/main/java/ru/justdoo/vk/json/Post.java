package ru.justdoo.vk.json;

import java.util.Map;

public class Post {
    private final Map map;
    private final long id;
    private final long fromId;
    private final long toId;
    private final long date;
    private final String text;
    private final long copyOwnerId;
    private final long copyPostId;

    public Post(Map map) {
        this.map = map;
        id = ((Number) map.get("id")).longValue();
        fromId = ((Number) map.get("from_id")).longValue();
        toId = ((Number) map.get("to_id")).longValue();
        date = ((Number) map.get("date")).longValue();
        text = (String) map.get("text");
        copyOwnerId = map.containsKey("copy_owner_id") ? ((Number) map.get("copy_owner_id")).longValue() : 0L;
        copyPostId= map.containsKey("copy_post_id") ? ((Number) map.get("copy_post_id")).longValue() : 0L;
    }

    public long getId() {
        return id;
    }

    public long getFromId() {
        return fromId;
    }

    public long getToId() {
        return toId;
    }

    public long getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public long getCopyOwnerId() {
        return copyOwnerId;
    }

    public long getCopyPostId() {
        return copyPostId;
    }

    public String toString() {
        return "post (" +
                "id=" + id + ", " +
                "from_id=" + fromId + ", " +
                "to_id=" + toId + ", " +
                "date=" + date + ", " +
                "copy_owner_id=" + copyOwnerId + ", " +
                "copy_post_id=" + copyPostId+ ", " +
                "text='" + text + "'" +
                ")";
    }
}
