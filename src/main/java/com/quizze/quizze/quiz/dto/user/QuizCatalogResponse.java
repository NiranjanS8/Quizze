package com.quizze.quizze.quiz.dto.user;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizCatalogResponse {

    private final List<QuizSummaryResponse> content;
    private final List<String> availableCategories;
    private final int pageNumber;
    private final int pageSize;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;
    private final boolean hasPrevious;
}
