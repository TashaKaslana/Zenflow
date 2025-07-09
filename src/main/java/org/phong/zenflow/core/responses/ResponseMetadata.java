package org.phong.zenflow.core.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseMetadata {
    private PaginationInfo pagination;
    private Map<String, String> links;

    public <T> ResponseMetadata(Page<T> page) {
        this.pagination = new PaginationInfo(page);
    }

    public ResponseMetadata(Map<String, String> links) {
        this.links = links;
    }

    public <T> ResponseMetadata(Page<T> page, Map<String, String> links) {
        this.pagination = new PaginationInfo(page);
        this.links = links;
    }
}