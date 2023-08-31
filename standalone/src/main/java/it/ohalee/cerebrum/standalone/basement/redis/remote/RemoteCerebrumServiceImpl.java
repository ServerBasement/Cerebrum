package it.ohalee.cerebrum.standalone.basement.redis.remote;

import it.ohalee.basementlib.api.remote.RemoteCerebrumService;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RemoteCerebrumServiceImpl implements RemoteCerebrumService {

    private final DockerService dockerService;

    @Override
    public void createServer(String name) {
        String ranchName = name.split("-")[0];
        String serverName = name.substring(ranchName.length() + 1);
        dockerService.startServer(ranchName, serverName, true);
    }
}
