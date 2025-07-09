package org.phong.zenflow.core.responses; // Or a suitable package

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationInfo {
    private int currentPage;
    private int pageSize;
    private int totalItems;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public <T> PaginationInfo(Page<T> page) {
        this.currentPage = page.getNumber();
        this.pageSize = page.getSize();
        this.totalItems = Math.toIntExact(page.getTotalElements());
        this.totalPages = page.getTotalPages();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }
}