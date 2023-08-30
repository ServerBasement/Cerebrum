package it.ohalee.cerebrum.standalone.basement;

import it.ohalee.basementlib.api.server.BukkitServer;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.rancher.Ranch;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Optional;

public class BasementService {

    private final DockerService dockerService;

    public BasementService(BasementLoader loader, DockerService dockerService) {
        this.dockerService = dockerService;

        loader.getBasement().serverManager().setServerAddConsumer(server -> {
            Logger.info("Server add message -> " + server.getName());
            set(server, dockerService::setLoaded, true);
        });
        loader.getBasement().serverManager().setServerRemoveConsumer(server -> {
            Logger.info("Server remove message -> " + server.getName());
            set(server, dockerService::setStatus, false);
        });
    }

    private void set(BukkitServer server, TriConsumer<String, String, Boolean> consumer, boolean status) {
        String ranchName = server.getName().split("_")[0];
        Optional<Ranch> optionalRanch = dockerService.getRanch(ranchName);
        consumer.accept(optionalRanch.isPresent() ? ranchName : "server", server.getName().replace(ranchName + "_", ""), status);
    }

}
