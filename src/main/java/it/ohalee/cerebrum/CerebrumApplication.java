package it.ohalee.cerebrum;

import it.ohalee.cerebrum.basement.BasementLoader;
import it.ohalee.cerebrum.basement.BasementService;
import it.ohalee.cerebrum.commands.CerebrumCommands;
import it.ohalee.cerebrum.commands.TabCompletation;
import it.ohalee.cerebrum.config.CerebrumConfigAdapter;
import it.ohalee.cerebrum.docker.DockerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Path;

@SpringBootApplication
public class CerebrumApplication {

    public static void main(String[] args) {
        Path dir = new File(".").toPath();
        BasementLoader loader = new BasementLoader(dir);

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

    public static void exit() {
        System.exit(0);
    }

}
