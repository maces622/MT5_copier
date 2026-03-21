package com.zyc.copier_v0.modules.account.config.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveSymbolMappingRequest {

    @NotNull
    private Long followerAccountId;

    @NotBlank
    private String masterSymbol;

    @NotBlank
    private String followerSymbol;
}
