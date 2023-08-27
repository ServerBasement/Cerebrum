package it.ohalee.cerebrum.commands;

import it.ohalee.cerebrum.docker.DockerService;
import it.ohalee.cerebrum.docker.rancher.Ranch;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TabCompletation implements ValueProvider {

    private static DockerService dockerService;

    public static void setDockerService(DockerService service) {
        dockerService = service;
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
        if(context.currentWord() == null)
            return Collections.emptyList();
        switch (context.currentWord()) {
            case "ranch":
                return dockerService.getRegisteredRanches().parallelStream()
                        .filter(ranch -> ranch.startsWith(context.currentWordUpToCursor()))
                        .map(CompletionProposal::new)
                        .collect(Collectors.toList());
            case "serverName": {
                Optional<Ranch> optRanch = dockerService.getRanch(context.getWords().get(1));
                if(!optRanch.isPresent())
                    return Collections.emptyList();
                String ranchName = optRanch.get().getName();
                return optRanch.get().getServers().parallelStream()
                        .filter(container -> container.getName().replace(ranchName + "_", "").startsWith(context.currentWordUpToCursor()))
                        .map(container -> new CompletionProposal(container.getName().replace(ranchName + "_", "")))
                        .collect(Collectors.toList());
            }
            default:
                return Collections.emptyList();
        }
    }

}
