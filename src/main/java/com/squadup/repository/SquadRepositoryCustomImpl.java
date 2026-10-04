package com.squadup.repository;

import com.squadup.dto.SquadSearchCriteria;
import com.squadup.entity.Squad;
import com.squadup.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom repository implementation. The "Impl" suffix is what lets Spring Data find this
 * class and plug it into {@link SquadRepository}. Builds a dynamic query with the JPA
 * Criteria API so that every filter in {@link SquadSearchCriteria} is optional.
 */
public class SquadRepositoryCustomImpl implements SquadRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Squad> search(SquadSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Squad> query = cb.createQuery(Squad.class);
        Root<Squad> squad = query.from(Squad.class);
        query.select(squad).where(predicates(criteria, cb, query, squad));
        if (pageable.getSort().isSorted()) {
            query.orderBy(QueryUtils.toOrders(pageable.getSort(), squad, cb));
        } else {
            query.orderBy(cb.desc(squad.get("createdAt")));
        }

        TypedQuery<Squad> typed = em.createQuery(query);
        if (pageable.isPaged()) {
            typed.setFirstResult((int) pageable.getOffset());
            typed.setMaxResults(pageable.getPageSize());
        }
        List<Squad> content = typed.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Squad> countRoot = countQuery.from(Squad.class);
        countQuery.select(cb.count(countRoot)).where(predicates(criteria, cb, countQuery, countRoot));
        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] predicates(SquadSearchCriteria c, CriteriaBuilder cb, CriteriaQuery<?> query, Root<Squad> squad) {
        List<Predicate> predicates = new ArrayList<>();

        if (hasText(c.text())) {
            String like = "%" + c.text().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(squad.get("name")), like),
                    cb.like(cb.lower(squad.get("game")), like),
                    cb.like(cb.lower(squad.get("language")), like)));
        }
        if (hasText(c.game())) {
            predicates.add(cb.equal(cb.lower(squad.get("game")), c.game().trim().toLowerCase()));
        }
        if (hasText(c.language())) {
            predicates.add(cb.equal(cb.lower(squad.get("language")), c.language().trim().toLowerCase()));
        }
        if (c.openOnly()) {
            predicates.add(cb.lessThan(cb.size(squad.get("members")), squad.get("maxMembers")));
        }
        if (c.excludeMemberId() != null) {
            Subquery<Long> joined = query.subquery(Long.class);
            Root<Squad> inner = joined.from(Squad.class);
            Join<Squad, User> member = inner.join("members");
            joined.select(inner.get("id"))
                    .where(cb.equal(member.get("id"), c.excludeMemberId()));
            predicates.add(cb.not(squad.get("id").in(joined)));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
