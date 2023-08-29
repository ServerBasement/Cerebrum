package it.ohalee.cerebrum.app.commands;

import it.ohalee.cerebrum.app.integration.CommandExecutor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class CerebrumCommands {

    private static CommandExecutor commandExecutor;

    public static void setDockerService(CommandExecutor commandExecutor) {
        CerebrumCommands.commandExecutor = commandExecutor;
    }

    @ShellMethod("Starts a container and its server")
    public String start(@ShellOption(value = "-r", valueProvider = TabCompletation.class) String ranch, @ShellOption(value = "-s", valueProvider = TabCompletation.class, defaultValue = "all") String serverName) {
        return commandExecutor.execute("start", ranch, serverName, false);
    }

    @ShellMethod("Stops a container and its server")
    public String stop(@ShellOption(value = "-r", valueProvider = TabCompletation.class) String ranch, @ShellOption(value = "-s", valueProvider = TabCompletation.class, defaultValue = "all") String serverName) {
        return commandExecutor.execute("stop", ranch, serverName, null);
    }

    @ShellMethod("Stops all containers")
    public String stopAll() {
        return commandExecutor.execute("stopall", null, null, null);
    }

    @ShellMethod("Starts all containers")
    public String startAll() {
        return commandExecutor.execute("startall", null, null, null);
    }

    @ShellMethod("Stops cerebrum but not containers")
    public String quit() {
        return commandExecutor.execute("quit", null, null, null);
    }

    @ShellMethod("Stops cerebrum and all the containers")
    public String end() {
        return commandExecutor.execute("end", null, null, null);
    }

    @ShellMethod("Reload configurations")
    public String reload() {
        return commandExecutor.execute("reload", null, null, null);
    }

    @ShellMethod("Lists all containers")
    public String list(@ShellOption(defaultValue = "all", valueProvider = TabCompletation.class) String ranch) {
        return commandExecutor.execute("list", ranch, null, null);
    }

    @ShellMethod("Send file update")
    public String update() {
        return commandExecutor.execute("update", null, null, null);
    }

}
