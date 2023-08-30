package it.ohalee.cerebrum.app.commands;

import it.ohalee.cerebrum.app.integration.CommandExecutor;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TabCompletation implements ValueProvider {

    private static CommandExecutor commandExecutor;

    public static void setDockerService(CommandExecutor commandExecutor) {
        TabCompletation.commandExecutor = commandExecutor;
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
        return commandExecutor.tabCompletion(context.currentWord(), context.getWords(), context.currentWordUpToCursor())
                .stream()
                .map(CompletionProposal::new)
                .toList();
    }

}
