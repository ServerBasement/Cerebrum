package it.ohalee.cerebrum.basement.redis.handlers;

import it.ohalee.basementlib.api.redis.messages.handler.BasementMessageHandler;
import it.ohalee.basementlib.api.redis.messages.implementation.VelocityNotifyMessage;
import it.ohalee.cerebrum.docker.DockerService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.TriConsumer;

@RequiredArgsConstructor
public class VelocityNotifyHandler implements BasementMessageHandler<VelocityNotifyMessage> {

    private final DockerService dockerService;

    @Override
    public void execute(VelocityNotifyMessage message) {
        if(message.isShutdown()) {
            run(dockerService::setStatus, false);
        } else {
            run(dockerService::setLoaded, true);
        }
    }

    @Override
    public Class<VelocityNotifyMessage> getCommandClass() {
        return VelocityNotifyMessage.class;
    }

    private void run(TriConsumer<String, String, Boolean> consumer, boolean value) {
        consumer.accept("server", "velocity", value);
    }
}
