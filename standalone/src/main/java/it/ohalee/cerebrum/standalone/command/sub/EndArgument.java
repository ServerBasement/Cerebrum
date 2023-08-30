package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.command.StandaloneCommandManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EndArgument implements ArgumentCommand {

    private final StandaloneCommandManager manager;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        String out = manager.execute("stopall", ranch, serverName, value);
        return out + "\n" + manager.execute("quit", ranch, serverName, value);
    }
}
