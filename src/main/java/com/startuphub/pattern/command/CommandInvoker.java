package com.startuphub.pattern.command;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class CommandInvoker {
    private final List<String> history = new ArrayList<>();

    public void run(Command command) {
        command.execute();
        history.add(command.describe());
    }

    public List<String> getHistory() { return history; }
}
