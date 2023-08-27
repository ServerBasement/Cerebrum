package it.ohalee.cerebrum.standalone.basement.redis.message;

import it.ohalee.basementlib.api.redis.messages.BasementMessage;
import lombok.Getter;

import java.util.UUID;

@Getter
public class StartServerMessage extends BasementMessage {

    public static final String TOPIC = "cerebrum-start-server";

    private final UUID uuid;
    private final String ranchName;
    private final String serverName;

    public StartServerMessage() {
        super(TOPIC);
        this.uuid = null;
        this.ranchName = null;
        this.serverName = null;
    }

    public StartServerMessage(UUID uuid, String ranchName, String serverName) {
        super(TOPIC);
        this.uuid = uuid;
        this.ranchName = ranchName;
        this.serverName = serverName;
    }
}
