package com.disasterrelief.commandcenter.application;

import com.disasterrelief.commandcenter.domain.command.AcknowledgeCommandCommand;
import com.disasterrelief.commandcenter.domain.command.SendCommandToTeamCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/commands")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendCommand(@RequestBody SendCommandToTeamCommand command) {
        commandService.sendCommand(command);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/acknowledge")
    public ResponseEntity<Void> acknowledge(@RequestBody AcknowledgeCommandCommand command) {
        commandService.acknowledgeCommand(command);
        return ResponseEntity.accepted().build();
    }
}