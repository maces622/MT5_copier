package com.zyc.copier_v0.modules.account.config.api;

import java.time.Instant;
import lombok.Data;

@Data
public class SymbolMappingResponse {

    private Long id;
    private Long followerAccountId;
    private String masterSymbol;
    private String followerSymbol;
    private Instant createdAt;
    private Instant updatedAt;
}
