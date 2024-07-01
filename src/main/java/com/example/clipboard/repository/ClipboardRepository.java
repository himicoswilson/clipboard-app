package com.example.clipboard.repository;

import com.example.clipboard.model.Clipboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClipboardRepository extends JpaRepository<Clipboard, Long> {
    @Query("SELECT c FROM Clipboard c ORDER BY c.createdAt DESC")
    List<Clipboard> findAllOrderByCreatedAtDesc();
}
