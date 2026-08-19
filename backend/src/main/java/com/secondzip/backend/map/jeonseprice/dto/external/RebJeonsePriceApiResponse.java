package com.secondzip.backend.map.jeonseprice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RebJeonsePriceApiResponse {

    @JsonProperty("SttsApiTblData")
    private List<Section> sections;

    public List<RebJeonsePriceRowDTO> getRows() {
        if (sections == null) {
            return Collections.emptyList();
        }

        return sections.stream()
                .map(Section::getRow)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(Collections::emptyList);
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Section {

        private List<RebJeonsePriceRowDTO> row;
    }
}
