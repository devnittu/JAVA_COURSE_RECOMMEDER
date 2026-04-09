package com.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Generic paginated response wrapper for all list endpoints.
 * Supports infinite scroll and traditional pagination.
 * 
 * Example response:
 * {
 *   "content": [...CourseDTO],
 *   "total": 1047,
 *   "hasMore": true,
 *   "nextOffset": 20,
 *   "limit": 20
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> content;           // Current page items
    private int total;                 // Total items in DB
    private boolean hasMore;           // Are there more pages?
    private int nextOffset;            // Use for next request (offset + limit)
    private int limit;                 // Items per page
    
    /**
     * Convenience constructor
     */
    public PaginatedResponse(List<T> content, int total, int offset, int limit) {
        this.content = content;
        this.total = total;
        this.limit = limit;
        this.nextOffset = offset + limit;
        this.hasMore = (offset + limit) < total;
    }
}
