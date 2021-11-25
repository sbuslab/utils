package com.sbuslab.utils;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestBoolean {

    @NotNull
    @JsonProperty("isActive")
    private boolean isActive;

    @NotNull
    private Boolean isTest;
}
