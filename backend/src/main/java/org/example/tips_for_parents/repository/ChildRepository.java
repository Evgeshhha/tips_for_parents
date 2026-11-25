package org.example.tips_for_parents.repository;

import org.example.tips_for_parents.exception.ApiErrors;
import org.example.tips_for_parents.model.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChildRepository extends JpaRepository<Child, UUID> {

    default Child getOrThrow(UUID id) {
        return findById(id).orElseThrow(() -> ApiErrors.notFound("Ребёнок не найден"));
    }
}