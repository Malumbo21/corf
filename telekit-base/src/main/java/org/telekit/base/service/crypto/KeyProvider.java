package org.telekit.base.service.crypto;

import org.jetbrains.annotations.Nullable;

import java.security.Key;

public interface KeyProvider {

    @Nullable Key getKey();
}