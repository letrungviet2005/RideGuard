package com.rideguard.shared;

public record ApiError(int status, String code, String message) {}
