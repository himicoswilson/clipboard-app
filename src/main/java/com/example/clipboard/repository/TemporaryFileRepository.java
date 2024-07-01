package com.example.clipboard.repository;

import com.example.clipboard.model.TemporaryFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporaryFileRepository extends JpaRepository<TemporaryFile, Long> {
}