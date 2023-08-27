package it.ohalee.cerebrum;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class CustomShellPrompt implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        return new AttributedString("CEREBRUM > ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }
}