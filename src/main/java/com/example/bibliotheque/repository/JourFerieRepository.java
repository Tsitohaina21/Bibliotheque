package com.example.bibliotheque.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bibliotheque.model.JourFerie;

public interface JourFerieRepository extends JpaRepository<JourFerie, Integer> {
    boolean existsByDateFerie(LocalDate date);
}
