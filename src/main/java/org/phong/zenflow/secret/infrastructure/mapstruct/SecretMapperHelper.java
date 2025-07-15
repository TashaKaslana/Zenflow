package org.phong.zenflow.secret.infrastructure.mapstruct;

import lombok.AllArgsConstructor;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.util.AESUtil;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class SecretMapperHelper {
    private final AESUtil aesUtil;

    public String decrypt(String encryptedValue) {
        try {
            return aesUtil.decrypt(encryptedValue);
        } catch (Exception e) {
            throw new SecretDomainException("Failed to decrypt secret value", e);
        }
    }
}
