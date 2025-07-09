package org.phong.zenflow.core.provider;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        // You can replace this with actual user fetching logic
        return Optional.ofNullable("00000000-0000-0000-0000-000000000000");
    }
}
