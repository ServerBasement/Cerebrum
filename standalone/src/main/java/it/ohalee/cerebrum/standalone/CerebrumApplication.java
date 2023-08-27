package it.ohalee.cerebrum.standalone;

import it.ohalee.cerebrum.common.classpath.ClassPathAppender;
import it.ohalee.cerebrum.common.classpath.JarInJarClassPathAppender;
import it.ohalee.cerebrum.common.loader.LoaderBootstrap;
import it.ohalee.cerebrum.standalone.commands.CerebrumCommands;
import it.ohalee.cerebrum.standalone.commands.TabCompletation;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigAdapter;
import it.ohalee.cerebrum.standalone.dependency.DependencyManager;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.basement.BasementLoader;
import it.ohalee.cerebrum.standalone.basement.BasementService;
import it.ohalee.cerebrum.standalone.dependency.Dependency;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class CerebrumApplication implements LoaderBootstrap {

    private final Path dir;
    private BasementLoader loader;
    @Getter
    private final ClassPathAppender classPathAppender;

    public CerebrumApplication() {
        this.dir = new File(".").toPath();
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
    }

    protected Set<Dependency> getGlobalDependencies() {
        return new HashSet<>(Arrays.asList(Dependency.values()));
    }

    @Override
    public void onLoad() {
        new DependencyManager(this).loadDependencies(getGlobalDependencies());
        this.loader = new BasementLoader(dir);
    }

    @Override
    public void onEnable(String[] args) {
        CerebrumConfigAdapter settings = (CerebrumConfigAdapter) loader.getBasement().plugin()
                .provideConfigurationAdapter(CerebrumApplication.class, dir.resolve("settings.yml").toFile(), true);
        CerebrumConfigAdapter share = (CerebrumConfigAdapter) loader.getBasement().plugin()
                .provideConfigurationAdapter(CerebrumApplication.class, dir.resolve("share.yml").toFile(), true);

        DockerService dockerService = new DockerService(settings, share);
        new BasementService(loader, dockerService);

        TabCompletation.setDockerService(dockerService);
        CerebrumCommands.setDockerService(dockerService);

        dockerService.postExecution();
        SpringApplication.run(CerebrumApplication.class, args);
    }

    @Override
    public void onDisable() {

    }
}
