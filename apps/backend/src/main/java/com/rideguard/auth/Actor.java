package com.rideguard.auth;

import java.util.UUID;

public record Actor(UUID userId, UUID familyId, String role) {}
