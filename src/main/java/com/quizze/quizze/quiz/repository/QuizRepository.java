package com.quizze.quizze.quiz.repository;

import com.quizze.quizze.quiz.domain.Quiz;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {

    List<Quiz> findByPublishedTrue();

    Optional<Quiz> findByIdAndPublishedTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quiz q where q.id = :id and q.published = true")
    Optional<Quiz> findPublishedByIdForUpdate(@Param("id") Long id);

    @Query("""
            select distinct c.name
            from Quiz q
            join q.category c
            where q.published = true
            order by c.name asc
            """)
    List<String> findPublishedCategoryNames();
}
