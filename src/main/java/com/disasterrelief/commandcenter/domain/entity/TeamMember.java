package com.disasterrelief.commandcenter.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class TeamMember {
    private UUID memberId;
    private String name;
    private String role;
}

