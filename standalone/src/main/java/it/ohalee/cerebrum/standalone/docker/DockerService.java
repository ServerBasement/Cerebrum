package it.ohalee.cerebrum.standalone.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import it.ohalee.basementlib.api.redis.RedisManager;
import it.ohalee.basementlib.api.redis.messages.implementation.VelocityNotifyMessage;
import it.ohalee.basementlib.api.remote.RemoteCerebrumService;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.app.util.CerebrumError;
import it.ohalee.cerebrum.app.util.CerebrumReason;
import it.ohalee.cerebrum.standalone.basement.BasementLoader;
import it.ohalee.cerebrum.standalone.basement.redis.handlers.StartServerHandler;
import it.ohalee.cerebrum.standalone.basement.redis.handlers.VelocityNotifyHandler;
import it.ohalee.cerebrum.standalone.basement.redis.message.StartServerMessage;
import it.ohalee.cerebrum.standalone.basement.redis.remote.RemoteCerebrumServiceImpl;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigAdapter;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import it.ohalee.cerebrum.standalone.docker.rancher.Ranch;
import lombok.Getter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DockerService {

    public static final UUID uuid = UUID.randomUUID();
    public static final String SENDER_NAME = "cerebrum";
    @Getter
    private final static ExecutorService executor = Executors.newFixedThreadPool(4);
    @Getter
    private static DockerClient client;
    private final CerebrumConfigAdapter settings;
    private final CerebrumConfigAdapter share;

    private final Map<String, Ranch> ranches = new HashMap<>();

    public DockerService(CerebrumConfigAdapter settings, CerebrumConfigAdapter share) {
        this.settings = settings;
        this.share = share;
        client = registerClient();
    }

    public Set<String> getRegisteredRanches() {
        return Collections.unmodifiableSet(ranches.keySet());
    }

    public Collection<Ranch> getRanches() {
        return Collections.unmodifiableCollection(ranches.values());
    }

    public Optional<Ranch> getRanch(String ranch) {
        return Optional.ofNullable(ranches.get(ranch));
    }

    public void postExecution() {
        registerTopics();
        findRanches();
        updateJars();
    }

    public void registerTopics() {
        RedisManager redisManager = BasementLoader.get().redisManager();
        if (settings.getBoolean("leader", true))
            redisManager.redissonClient().getRemoteService().register(RemoteCerebrumService.class, new RemoteCerebrumServiceImpl(this), 3, Executors.newSingleThreadExecutor());
        redisManager.registerTopicListener(VelocityNotifyMessage.TOPIC, new VelocityNotifyHandler(this));
        redisManager.registerTopicListener(StartServerMessage.TOPIC, new StartServerHandler(this));
    }

    private DockerClient registerClient() {
        DockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerConfig.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(dockerConfig, dockerHttpClient);
    }

    public void findRanches() {
        try (ListContainersCmd cmd = DockerService.getClient().listContainersCmd()) {
            List<Container> allContainers = cmd.exec();
            for (String ranchName : settings.getKeys()) {
                Ranch newRanch = new Ranch(ranchName, settings.section(ranchName));
                ranches.put(ranchName, newRanch);
                executor.submit(() -> {
                    newRanch.findContainers(allContainers.stream()
                            .filter(container -> container.getNames().length > 0 && container.getNames()[0].substring(1).startsWith(ranchName))
                            .collect(Collectors.toList()));
                    newRanch.registerLeaders();
                });
            }
        }
    }

    public CerebrumError handle(Consumer<ServerContainer> consumer) {
        if (getRanches().isEmpty())
            return CerebrumError.of(CerebrumReason.RANCH_ERROR, "No ranches registered.");
        for (Ranch ranch : getRanches())
            for (ServerContainer server : ranch.getServers())
                consumer.accept(server);
        return CerebrumError.of(CerebrumReason.OK, null);
    }

    public CerebrumError startServer(String ranchName, String serverName, boolean flush) {
        Ranch ranch = ranches.get(ranchName);
        if (ranch == null) {
            if (flush) {
                BasementLoader.get().redisManager().publishMessage(new StartServerMessage(uuid, ranchName, serverName));
                return CerebrumError.of(CerebrumReason.OK, null); // Flush is intentional so OK is returned
            }
            return CerebrumError.of(CerebrumReason.RANCH_ERROR, "No ranches registered.");
        }

        String qualifiedName = ranchName + "-" + serverName;
        for (String worker : ranch.getWorkers()) {
            if (qualifiedName.startsWith(ranch.getName() + "-" + worker)) {
                Logger.info("Starting worker container " + ranch.getName() + "-" + serverName + "...");
                ranch.registerWorker(qualifiedName, worker, ServerContainer.Type.WORKER, false, false).start();
                return CerebrumError.of(CerebrumReason.OK, null);
            }
        }

        Optional<ServerContainer> container = ranch.getServer(serverName);
        if (container.isEmpty()) {
            Logger.warn("Operation (start) failed, " + serverName + " in ranch " + ranchName + " is not registered.");
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Server not registered in ranch " + ranchName + ".");
        }

        Logger.info("Starting container " + ranch.getName() + "-" + serverName + "...");
        return container.get().start();
    }

    public CerebrumError stopServer(String ranchName, String serverName) {
        Ranch ranch = ranches.get(ranchName);
        if (ranch == null)
            return CerebrumError.of(CerebrumReason.RANCH_ERROR, "No ranches registered.");

        Optional<ServerContainer> container = ranch.getServer(serverName);
        if (container.isEmpty()) {
            Logger.warn("Operation (stop) failed, " + serverName + " in ranch " + ranchName + " is not registered.");
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Server not registered in ranch " + ranchName + ".");
        }

        Logger.info("Stopping container " + ranchName + "-" + serverName + "...");
        return container.get().stop();
    }

    public CerebrumError setStatus(String ranchName, String serverName, boolean running) {
        Ranch ranch = ranches.get(ranchName);
        if (ranch == null)
            return CerebrumError.of(CerebrumReason.RANCH_ERROR, "No ranches registered.");

        Optional<ServerContainer> container = ranch.getServer(serverName);
        if (container.isEmpty()) {
            Logger.warn("Operation (status change) failed, " + serverName + " in ranch " + ranchName + " is not registered.");
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Server not registered in ranch " + ranchName + ".");
        }

        Logger.info("Setting status of server " + serverName + " to " + (running ? "running" : "stopped"));
        container.get().setRunning(running);
        if (!running)
            container.get().setLoaded(false);
        return CerebrumError.of(CerebrumReason.OK, null);
    }

    public CerebrumError setLoaded(String ranchName, String serverName, boolean loaded) {
        Ranch ranch = ranches.get(ranchName);
        if (ranch == null)
            return CerebrumError.of(CerebrumReason.RANCH_ERROR, "No ranches registered.");

        Optional<ServerContainer> container = ranch.getServer(serverName);
        if (container.isEmpty()) {
            Logger.warn("Operation (loaded change) failed, " + serverName + " in ranch " + ranchName + " is not registered.");
            return CerebrumError.of(CerebrumReason.SERVER_ERROR, "Server not registered in ranch " + ranchName + ".");
        }

        Logger.info("Setting status of server " + serverName + " to " + (loaded ? "loaded" : "unloaded"));
        container.get().setLoaded(loaded);
        return CerebrumError.of(CerebrumReason.OK, null);
    }

    public void recalculateConfiguration() {
        ranches.clear();
        findRanches();
    }

    public CerebrumError updateJars() {
        Collection<String> seekingJarsPrefixes = share.getKeys();
        Map<String, String> prefixMap = new HashMap<>(); // file -> prefix
        FilenameFilter filter = (dir, name) -> name.endsWith(".jar") && seekingJarsPrefixes.stream()
                .anyMatch(prefix -> {
                    if (name.startsWith(prefix)) {
                        prefixMap.put(name, prefix);
                        return true;
                    }
                    return false;
                });
        File dir = new File("share/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles(filter);
        if (files == null || files.length == 0) {
            Logger.warn("Jars update aborted.");
            return CerebrumError.of(CerebrumReason.ERROR, "No files found.");
        }

        for (File file : files) {
            String prefix = prefixMap.get(file.getName());
            Logger.info("Updating... '" + prefix + "'");

            for (String settingsServerPath : share.getStringList(prefix, Collections.emptyList())) {
                File dirToPlugins = new File((settings.getString(settingsServerPath + ".server", null) + "plugins").replaceAll("/home", ""));
                try {
                    Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(dirToPlugins.getAbsolutePath() + "/" + file.getName()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Logger.severe("Could not update '" + prefix + "' to '" + settingsServerPath + "'.", e);
                    return CerebrumError.of(CerebrumReason.ERROR, "Could not update '" + prefix + "' to '" + settingsServerPath + "'.");
                }
            }

            Logger.info(file.delete() ? (prefix + " updated!") : (prefix + " could not be updated."));
        }
        return CerebrumError.of(CerebrumReason.OK, null);
    }

}
