package com.aicodingmentor.problem.repository;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProblemSpecifications {

    private ProblemSpecifications() {
    }

    public static Specification<Problem> filterBy(ProblemDifficulty difficulty, String tag) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            if (tag != null && !tag.trim().isEmpty()) {
                Join<Problem, String> tagJoin = root.join("tags");
                predicates.add(cb.equal(cb.lower(tagJoin), tag.trim().toLowerCase()));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
