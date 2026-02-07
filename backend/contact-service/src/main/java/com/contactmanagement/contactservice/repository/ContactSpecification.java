package com.contactmanagement.contactservice.repository;

import com.contactmanagement.contactservice.dto.ContactSearchRequest;
import com.contactmanagement.contactservice.model.Contact;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ContactSpecification {

    public static Specification<Contact> buildSpecification(ContactSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            boolean needsDistinct = false;

            // Name filter: search in firstName OR lastName (case-insensitive, partial match)
            if (request.getName() != null && !request.getName().trim().isEmpty()) {
                String namePattern = "%" + request.getName().trim().toLowerCase() + "%";
                Predicate firstNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")), namePattern
                );
                Predicate lastNamePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")), namePattern
                );
                predicates.add(criteriaBuilder.or(firstNamePredicate, lastNamePredicate));
            }

            // Email filter: search in emails collection (case-insensitive, partial match)
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                Join<Contact, String> emailJoin = root.join("emails", JoinType.LEFT);
                String emailPattern = "%" + request.getEmail().trim().toLowerCase() + "%";
                Predicate emailPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(emailJoin), emailPattern
                );
                predicates.add(emailPredicate);
                needsDistinct = true;
            }

            // Phone filter: search in phones collection (partial match)
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                Join<Contact, String> phoneJoin = root.join("phones", JoinType.LEFT);
                String phonePattern = "%" + request.getPhone().trim() + "%";
                Predicate phonePredicate = criteriaBuilder.like(phoneJoin, phonePattern);
                predicates.add(phonePredicate);
                needsDistinct = true;
            }

            // Zip filter: search for zip code pattern in addresses (case-insensitive)
            if (request.getZip() != null && !request.getZip().trim().isEmpty()) {
                Join<Contact, String> addressJoin = root.join("addresses", JoinType.LEFT);
                String zipPattern = "%" + request.getZip().trim() + "%";
                // Search for zip code pattern (5 digits or 5+4 format)
                // This will match zip codes anywhere in the address string
                Predicate zipPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(addressJoin), zipPattern.toLowerCase()
                );
                predicates.add(zipPredicate);
                needsDistinct = true;
            }

            // Use distinct to avoid duplicate results when joins are used
            if (needsDistinct) {
                query.distinct(true);
            }

            // Combine all predicates with AND logic
            // If no predicates, return null (no filtering)
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Always true predicate
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
