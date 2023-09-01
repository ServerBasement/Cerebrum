package it.ohalee.cerebrum.standalone.docker.container;

import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import it.ohalee.basementlib.api.redis.messages.implementation.ServerShutdownMessage;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.app.util.CerebrumError;
import it.ohalee.cerebrum.app.util.CerebrumReason;
import it.ohalee.cerebrum.standalone.basement.BasementLoader;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigurationNode;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import it.ohalee.cerebrum.app.util.Validate;

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

    public void setContainerSection(CerebrumConfigurationNode node) {
        this.containerSection = node;

        String folder = Validate.notNull(node.getString("logs", null), name + ": Logs folder cannot be null");
        File logsFolder = new File(folder.replace("{name}", name));
        if (!logsFolder.exists()) {
            if (!logsFolder.mkdirs()) {
                Logger.severe("Cannot create logs folder for container " + name);
            }
        }

        worldDirectory = node.getString("world", null);
        ipv4 = node.getString("ipv4", null);

        hostConfig = HostConfig.newHostConfig()
                .withAutoRemove(true)
                .withBinds(
                        Bind.parse(Validate.notNull(node.getString("server", null), name + ": Server directory cannot be null") + ":/server"),
                        Bind.parse(folder.replace("{name}", name) + ":/server/logs")
                );

        if (ipv4 == null || ipv4.isEmpty()) {
            hostConfig.withNetworkMode(node.getString("net", null));
        }

        if (node.get("port", null) != null) {
            int port = node.getInteger("port", 25565);
            ExposedPort exposedPort = ExposedPort.tcp(port);
            Ports portBindings = new Ports();
            portBindings.bind(exposedPort, Ports.Binding.bindPort(port));
            hostConfig.withPortBindings(portBindings);
            exposedPorts.add(exposedPort);
        } else if (node.get("ports", null) != null) {
            List<String> ports = node.getStringList("ports", Collections.emptyList());
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
        }
    }

    public CerebrumError start() {
        if (running) {
            Logger.warn("Operation failed. Container " + name + " is already running.");
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Container " + name + " is already running.");
        }

        String image = Validate.notNull(containerSection.getString("image", null), name + ": Image cannot be null");

        try (ListImagesCmd cmd = DockerService.getClient().listImagesCmd().withImageNameFilter(image)) {
            if (cmd.exec().isEmpty()) {
                Logger.severe(name + ": cannot find image " + image);
                return CerebrumError.of(CerebrumReason.SERVER_ERROR, name + ": cannot find image " + image);
            }
        }

        try (CreateContainerCmd cmd = DockerService.getClient().createContainerCmd(image)) {
            cmd.withName(name)
                    .withHostName(name)
                    .withHostConfig(hostConfig)
                    .withUser("1000:1000")
                    .withWorkingDir("/server");

            String zone = containerSection.getString("timezone", null);
            if (zone != null)
                cmd.withEnv("TZ=" + zone);
            if (worldDirectory != null && !worldDirectory.isEmpty())
                cmd.withEntrypoint("/bin/sh", "start.sh", name, "--docker-world", worldDirectory);
            else
                cmd.withEntrypoint("/bin/sh", "start.sh", name);
            cmd.withStdinOpen(true).withTty(true);
            if (!exposedPorts.isEmpty())
                cmd.withExposedPorts(exposedPorts);
            running = true;
            cmd.exec();

            Logger.info("New container " + name + " with image " + image + " is starting...");
            try (StartContainerCmd startContainerCmd = DockerService.getClient().startContainerCmd(name)) {
                startContainerCmd.exec();
            }

            if (ipv4 != null && !ipv4.isEmpty()) {
                try (ConnectToNetworkCmd connectToNetworkCmd = DockerService.getClient().connectToNetworkCmd()) {
                    connectToNetworkCmd.withNetworkId(containerSection.getString("net", null))
                            .withContainerId(name)
                            .exec();
                }
            }
        }
        return CerebrumError.of(CerebrumReason.OK, null);
    }

    public CerebrumError stop() {
        if (!running) {
            Logger.warn("Operation failed. Container " + name + " is already stopped.");
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Container " + name + " is already stopped.");
        }

        if (!loaded) {
            DockerService.getExecutor().submit(() -> {
                try (StopContainerCmd stopContainerCmd = DockerService.getClient().stopContainerCmd(name)) {
                    stopContainerCmd.exec();
                }
            });
            running = false;
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Container " + name + " is running but it is not loaded. It will be stopped.");
        }

        Logger.info("Stopping container " + name + "...");
        BasementLoader.get().redisManager().publishMessage(new ServerShutdownMessage(DockerService.SENDER_NAME, name));
        //Logger.info("Container " + name + " stopped."); todo wait for a confirm? It is not said that the server has really shut down
        running = false;
        loaded = false;
        return CerebrumError.of(CerebrumReason.OK, null);
    }

    public enum Type {
        LEADER, WORKER
    }

}
