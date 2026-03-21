package com.zyc.copier_v0.modules.account.config.config;

import javax.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.security.credentials")
public class CredentialCryptoProperties {

    @NotBlank
    private String secret = "dev-credential-secret";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
