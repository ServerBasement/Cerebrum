package it.ohalee.cerebrum.standalone.docker.container;

import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import it.ohalee.basementlib.api.redis.messages.implementation.ServerShutdownMessage;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigurationNode;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.standalone.basement.BasementLoader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Getter
@Setter
public class ServerContainer {

    private final String name;
    private final String registeredName;
    private final Type type;
    private List<ExposedPort> exposedPorts = new ArrayList<>();
    private boolean running = false;
    private boolean loaded;
    private CerebrumConfigurationNode containerSection;
    private HostConfig hostConfig;
    private String worldDirectory;
    private String ipv4;

    public void setContainerSection(CerebrumConfigurationNode containerSection) {
        this.containerSection = containerSection;

        File logsFolder = new File(containerSection.getString("logs", null).replace("{name}", name));
        if (!logsFolder.exists()) {
            if (!logsFolder.getParentFile().exists()) {
                logsFolder.getParentFile().mkdir();
            }
            logsFolder.mkdir();
        }

        worldDirectory = containerSection.getString("world", null);
        ipv4 = containerSection.getString("ipv4", null);

        hostConfig = HostConfig.newHostConfig()
                .withAutoRemove(true)
                .withBinds(Bind.parse(containerSection.getString("server", null) + ":/server"),
                        Bind.parse(containerSection.getString("logs", null).replace("{name}", name) + ":/server/logs"));

        if (ipv4 == null || ipv4.isEmpty()) {
            hostConfig.withNetworkMode(containerSection.getString("net", null));
        }

        if (containerSection.get("port", null) != null) {
            int port = containerSection.getInteger("port", 25565);
            ExposedPort exposedPort = ExposedPort.tcp(port);
            Ports portBindings = new Ports();
            portBindings.bind(exposedPort, Ports.Binding.bindPort(port));
            hostConfig.withPortBindings(portBindings);
            exposedPorts.add(exposedPort);
        } else if (containerSection.get("ports", null) != null) {
            try {
                List<String> ports = containerSection.getStringList("ports", Collections.emptyList());
                Ports portBindings = new Ports();
                for (String portString : ports) {
                    String[] args = portString.split(":");
                    ExposedPort exposedPort;
                    int port;
                    if (args.length == 1) {
                        port = Integer.parseInt(args[0]);
                        exposedPort = ExposedPort.tcp(port);
                    } else {
                        port = Integer.parseInt(args[1]);
                        if (args[0].equals("udp")) {
                            exposedPort = ExposedPort.udp(port);
                        } else {
                            exposedPort = ExposedPort.tcp(port);
                        }
                    }
                    portBindings.bind(exposedPort, Ports.Binding.bindPort(port));
                    exposedPorts.add(exposedPort);
                }
                hostConfig.withPortBindings(portBindings);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void start() {
        if (running) {
            Logger.warn("Operation failed. Container " + name + " is already running.");
            return;
        }
        String image = containerSection.getString("image", null);
        try (CreateContainerCmd cmd = DockerService.getClient().createContainerCmd(image)) {
            cmd.withName(name)
                    .withHostName(name)
                    .withHostConfig(hostConfig)
                    .withUser("1000:1000")
                    .withWorkingDir("/server")
                    .withEnv("TZ=Europe/Rome"); // TODO: 26/08/2023 Change this to a config value
            if (worldDirectory != null && !worldDirectory.isEmpty()) {
                cmd.withEntrypoint("/bin/sh", "start.sh", name, "--docker-world", worldDirectory);
            } else {
                cmd.withEntrypoint("/bin/sh", "start.sh", name);
            }
            cmd.withStdinOpen(true)
                    .withTty(true);
            if (!exposedPorts.isEmpty())
                cmd.withExposedPorts(exposedPorts);
            running = true;
            Logger.info("A new container with image (" + image + ") and name (" + name + ") is starting...");
            cmd.exec();
            try (StartContainerCmd startContainerCmd = DockerService.getClient().startContainerCmd(name)) {
                startContainerCmd.exec();
                if (ipv4 != null && !ipv4.isEmpty()) {
                    try (ConnectToNetworkCmd connectToNetworkCmd = DockerService.getClient().connectToNetworkCmd()) {
                        connectToNetworkCmd
                                .withNetworkId(containerSection.getString("net", null))
                                .withContainerId(name)
                                .exec();
                    }
                }
            }
        }


    }

    public void stop() {
        if (!running) {
            Logger.warn("Operation failed. Container " + name + " is already stopped.");
            return;
        }
        if (!loaded) {
            DockerService.getExecutor().submit(() -> {
                try (StopContainerCmd stopContainerCmd = DockerService.getClient().stopContainerCmd(name)) {
                    stopContainerCmd.exec();
                }
            });
            running = false;
            return;
        }
        Logger.info("Stopping container " + name + "...");
        BasementLoader.get().redisManager().publishMessage(new ServerShutdownMessage(DockerService.SENDER_NAME, name));
        Logger.info("Container " + name + " stopped.");
        running = false;
        loaded = false;
    }

    public enum Type {
        LEADER, WORKER
    }

}
