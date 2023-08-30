package it.ohalee.cerebrum.standalone.command;

import it.ohalee.cerebrum.app.CerebrumApplication;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.app.integration.CommandExecutor;
import it.ohalee.cerebrum.standalone.command.sub.*;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import it.ohalee.cerebrum.standalone.docker.rancher.Ranch;

import java.util.*;
import java.util.stream.Collectors;

public class StandaloneCommandManager implements CommandExecutor {

    private final DockerService dockerService;
    private final Map<String, ArgumentCommand> arguments = new HashMap<>();

    public StandaloneCommandManager(DockerService dockerService) {
        this.dockerService = dockerService;

        this.arguments.put("start", new StartArgument(dockerService));
        this.arguments.put("stop", new StopArgument(dockerService));
        this.arguments.put("stopall", new StopAllArgument(dockerService));
        this.arguments.put("startall", new StartAllArgument(dockerService));
        this.arguments.put("list", new ListArgument(dockerService));
        this.arguments.put("quit", new QuitArgument(dockerService));
        this.arguments.put("end", new EndArgument(dockerService));
        this.arguments.put("reload", new ReloadArgument(dockerService));
        this.arguments.put("update", new UpdateArgument(dockerService));
    }

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean b) {
        Logger.info(arg + " command issued");

        ArgumentCommand subCommand = arguments.get(arg.toLowerCase(Locale.ROOT));
        if (subCommand != null) {
            return subCommand.execute(arg, ranch, serverName, b);
        }
        return null;
    }

    @Override
    public List<String> tabCompletion(String currentWord, List<String> words, String currentWordUpToCursor) {
        List<String> tab = new LinkedList<>(dockerService.getRegisteredRanches());

        for (Ranch ranch : dockerService.getRanches()) {
            tab.addAll(ranch.getServers().stream().map(ServerContainer::getName).toList());
        }
        return tab;
    }
}
