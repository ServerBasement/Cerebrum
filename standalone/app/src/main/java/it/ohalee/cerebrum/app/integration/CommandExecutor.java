package it.ohalee.cerebrum.app.integration;

import java.util.List;

public interface CommandExecutor {

    String execute(String arg, String ranch, String serverName, Boolean b);

    List<String> tabCompletion(String currentWord, List<String> words, String currentWordUpToCursor);
}
