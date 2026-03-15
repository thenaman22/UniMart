package com.unimart.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class ListingDtos {
    public record ListingMediaInput(
        @NotBlank String storageKey,
        @NotBlank String contentType,
        long fileSize
    ) {}

    public record CreateListingRequest(
        @NotNull Long communityId,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotBlank String category,
        @NotBlank String itemCondition,
        @Valid @NotEmpty List<ListingMediaInput> media
    ) {}

    public record UpdateListingRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotBlank String category,
        @NotBlank String itemCondition
    ) {}

    public record ReportListingRequest(@NotBlank String reason) {}
}
