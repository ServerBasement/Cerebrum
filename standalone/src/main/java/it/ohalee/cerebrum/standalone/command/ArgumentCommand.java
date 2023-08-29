package it.ohalee.cerebrum.standalone.command;

import java.util.Collections;
import java.util.List;

public interface ArgumentCommand {

    String execute(String arg, String ranch, String serverName, Boolean value);

    default List<String> tabCompletion(List<String> words, String currentWordUpToCursor) {
        return Collections.emptyList();
    }

}
