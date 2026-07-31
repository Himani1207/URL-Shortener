package com.example.url_shortner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of an analytics breakdown, e.g. {@code {"label": "Chrome 120", "count": 42}}.
 *
 * <p>Separate from the repository's {@code LabelCount} projection on purpose: the
 * projection is a persistence-layer interface backed by Hibernate, and returning it
 * from a controller would leak the persistence layer into the API contract and make
 * the JSON shape depend on a proxy. This is the transport-layer equivalent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A single labelled count in an analytics breakdown")
public class LabelCountResponse {

    @Schema(example = "Chrome 120")
    private String label;

    @Schema(example = "42")
    private Long count;
}
