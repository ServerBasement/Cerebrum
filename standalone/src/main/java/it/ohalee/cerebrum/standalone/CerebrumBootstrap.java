package it.ohalee.cerebrum.standalone;

import it.ohalee.cerebrum.app.integration.CommandExecutor;
import it.ohalee.cerebrum.app.scheduler.CerebrumScheduler;
import it.ohalee.cerebrum.common.classpath.ClassPathAppender;
import it.ohalee.cerebrum.common.classpath.JarInJarClassPathAppender;
import it.ohalee.cerebrum.common.loader.LoaderBootstrap;
import it.ohalee.cerebrum.standalone.basement.BasementLoader;
import it.ohalee.cerebrum.standalone.basement.BasementService;
import it.ohalee.cerebrum.standalone.command.StandaloneCommandManager;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigAdapter;
import it.ohalee.cerebrum.standalone.dependency.Dependency;
import it.ohalee.cerebrum.standalone.dependency.DependencyManager;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

@Getter
public class CerebrumBootstrap implements LoaderBootstrap {

    private final Consumer<CommandExecutor> consumer;
    private final Path dir = new File(".").toPath();
    private final ClassPathAppender classPathAppender;
    private final CerebrumScheduler scheduler;
    private final DependencyManager dependencyManager;
    private BasementLoader loader;

    public CerebrumBootstrap(Consumer<CommandExecutor> consumer) {
        this.consumer = consumer;
        this.scheduler = new CerebrumScheduler();

        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        this.dependencyManager = new DependencyManager(this);
    }

    @Override
    public void onLoad() {
        this.dependencyManager.loadDependencies(new HashSet<>(Arrays.asList(Dependency.values())));
        this.loader = new BasementLoader(dir);
    }

    @Override
    public void onEnable() {
        CerebrumConfigAdapter settings = (CerebrumConfigAdapter) loader.getBasement().plugin()
                .provideConfigurationAdapter(CerebrumBootstrap.class, dir.resolve("settings.yml").toFile(), true);
        CerebrumConfigAdapter share = (CerebrumConfigAdapter) loader.getBasement().plugin()
                .provideConfigurationAdapter(CerebrumBootstrap.class, dir.resolve("share.yml").toFile(), true);

        DockerService dockerService = new DockerService(settings, share);
        new BasementService(loader, dockerService);

        StandaloneCommandManager commandManager = new StandaloneCommandManager(dockerService);
        consumer.accept(commandManager);

        dockerService.postExecution();
    }

    @Override
    public void onDisable() {
        this.scheduler.shutdownScheduler();
        this.scheduler.shutdownExecutor();
    }
}
