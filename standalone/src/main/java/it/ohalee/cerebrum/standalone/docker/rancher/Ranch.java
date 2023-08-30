package it.ohalee.cerebrum.standalone.docker.rancher;

import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.app.util.CerebrumError;
import it.ohalee.cerebrum.app.util.CerebrumReason;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigurationNode;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Ranch {

    @Getter
    private final String name;
    private final Map<String, ServerContainer> servers = new ConcurrentHashMap<>();
    private CerebrumConfigurationNode ranchSection;

    public Ranch(String name, CerebrumConfigurationNode node) {
        this.name = name;
        this.ranchSection = node;
    }

    public Collection<ServerContainer> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    public ServerContainer registerWorker(String name, String workerName, ServerContainer.Type type, boolean running, boolean loaded) {
        return registerContainer(name, workerName, type, ranchSection.section("worker." + workerName), running, loaded);
    }

    private ServerContainer registerContainer(String name, String registeredName, ServerContainer.Type type, CerebrumConfigurationNode section, boolean running, boolean loaded) {
        ServerContainer newServerContainer = new ServerContainer(name, registeredName, type);
        newServerContainer.setContainerSection(section);
        newServerContainer.setRunning(running);
        newServerContainer.setLoaded(loaded);
        servers.put(newServerContainer.getName().replace(this.name + "_", ""), newServerContainer);
        return newServerContainer;
    }

    public Optional<ServerContainer> getServer(String serverName) {
        return Optional.ofNullable(servers.get(serverName));
    }

    public void findContainers(List<com.github.dockerjava.api.model.Container> availableRancherContainers) {
        Logger.info("Founded Containers -> " + availableRancherContainers.stream().map(container -> container.getNames()[0]).collect(Collectors.toList()));

        for (ServerContainer.Type containerType : ServerContainer.Type.values()) {
            for (String containerPatternName : ranchSection.section(containerType.toString().toLowerCase()).getKeys()) {
                availableRancherContainers.stream()
                        .filter(container -> container.getNames()[0].substring(1).startsWith(name + "_" + containerPatternName))
                        .forEach(container -> {
                                    Logger.info("Container Name -> " + Arrays.toString(container.getNames()) + " -> " + container.getNames()[0].substring(1).replace(name + "_", "") + " status " + container.getState());
                                    registerContainer(container.getNames()[0].substring(1), containerPatternName,
                                            containerType, ranchSection.section(containerType.toString().toLowerCase() + "." + containerPatternName),
                                            container.getState().contains("running"), true);
                                }
                        );
            }
        }
    }

    public void registerLeaders() {
        for (String leader : ranchSection.section("leader").getKeys()) {
            Logger.info("Registering leader -> " + leader);
            String name = this.name + "_" + leader;
            if (servers.containsKey(leader) && servers.get(leader).isRunning()) continue;
            ServerContainer container = registerContainer(name, leader, ServerContainer.Type.LEADER, ranchSection.section("leader." + leader), false, false);
            if (ranchSection.getBoolean("leader." + leader + ".startup", true))
                container.start();
        }
    }

    public Set<String> getWorkers() {
        return new HashSet<>(ranchSection.section("worker").getKeys());
    }

    public void recalculateConfiguration(CerebrumConfigurationNode configuration) {
        this.ranchSection = configuration;
        for (ServerContainer server : servers.values()) {
            server.setContainerSection(ranchSection.section(server.getType().toString().toLowerCase() + "." + server.getRegisteredName()));
        }
    }

    public CerebrumError shutdown() {
        if (servers.isEmpty())
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "No servers found in this ranch");

        for (ServerContainer value : servers.values()) {
            CerebrumError error = value.stop();
            if (error.code() != CerebrumReason.OK) {
                return error;
            }
        }
        return CerebrumError.of(CerebrumReason.OK, null);
    }

    public CerebrumError startLeaders() {
        if (servers.isEmpty())
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "No servers found in this ranch");

        for (ServerContainer value : servers.values()) {
            if (value.getType() == ServerContainer.Type.LEADER) {
                CerebrumError error = value.start();
                if (error.code() != CerebrumReason.OK) {
                    return error;
                }
            }
        }
        return CerebrumError.of(CerebrumReason.OK, null);
    }

}
