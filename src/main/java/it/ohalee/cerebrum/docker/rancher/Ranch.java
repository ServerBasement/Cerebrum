package it.ohalee.cerebrum.docker.rancher;

import it.ohalee.basementlib.api.config.generic.adapter.ConfigurationAdapter;
import it.ohalee.cerebrum.config.CerebrumConfigAdapter;
import it.ohalee.cerebrum.Logger;
import it.ohalee.cerebrum.docker.container.ServerContainer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor
public class Ranch {

    @Getter
    private final String name;
    private CerebrumConfigAdapter ranchSection;

    private final Map<String, ServerContainer> servers = new ConcurrentHashMap<>();

    public Collection<ServerContainer> getServers() {
        return Collections.unmodifiableCollection(servers.values());
    }

    public ServerContainer registerWorker(String name, String workerName, ServerContainer.Type type, boolean running, boolean loaded) {
        return registerContainer(name, workerName, type, ranchSection.section("worker." + workerName), running, loaded);
    }

    private ServerContainer registerContainer(String name, String registeredName, ServerContainer.Type type, ConfigurationAdapter section, boolean running, boolean loaded) {
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

        Logger.getInstance().info("FOUND CONTAINERS -> " + availableRancherContainers.stream().map(container -> container.getNames()[0]).collect(Collectors.toList()));

        for (ServerContainer.Type containerType : ServerContainer.Type.values()) {
            for (String containerPatternName : ranchSection.section(containerType.toString().toLowerCase()).getKeys()) {
                availableRancherContainers.stream()
                        .filter(container -> container.getNames()[0].substring(1).startsWith(name + "_" + containerPatternName))
                        .forEach(container -> {
                            Logger.getInstance().info("CONTAINER NAME -> " + Arrays.toString(container.getNames()) + " -> " + container.getNames()[0].substring(1).replace(name + "_", "") + " status " + container.getState());
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
            Logger.getInstance().info("REGISTERING LEADER -> " + leader);
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

    public void recalculateConfiguration(CerebrumConfigAdapter configuration) {
        this.ranchSection = configuration;
        for (ServerContainer server : servers.values()) {
            server.setContainerSection(ranchSection.section(server.getType().toString().toLowerCase() + "." + server.getRegisteredName()));
        }
    }

    public void shutdown() {
        for (ServerContainer value : servers.values())
            value.stop();
    }

    public void startLeaders() {
        for (ServerContainer value : servers.values()) {
            if (value.getType() == ServerContainer.Type.LEADER)
                value.start();
        }
    }

}
