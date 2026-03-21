package com.zyc.copier_v0.modules.account.config.config;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.security.credentials")
@Getter
@Setter
public class CredentialCryptoProperties {

    @NotBlank
    private String secret = "dev-credential-secret";
}
