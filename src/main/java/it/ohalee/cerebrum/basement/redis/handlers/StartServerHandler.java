package it.ohalee.cerebrum.basement.redis.handlers;

import it.ohalee.basementlib.api.redis.messages.handler.BasementMessageHandler;
import it.ohalee.cerebrum.basement.redis.message.StartServerMessage;
import it.ohalee.cerebrum.docker.DockerService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StartServerHandler implements BasementMessageHandler<StartServerMessage> {

    private final DockerService dockerService;

    @Override
    public void execute(StartServerMessage message) {
        if(message.getUuid().equals(DockerService.uuid)) return;
        dockerService.startServer(message.getRanchName(), message.getServerName(), false);
    }

    @Override
    public Class<StartServerMessage> getCommandClass() {
        return StartServerMessage.class;
    }
}
