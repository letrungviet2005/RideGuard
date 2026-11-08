package com.rideguard.shared;

import java.util.UUID;

public record ApiError(int status, String code, String message, UUID requestId) {}
