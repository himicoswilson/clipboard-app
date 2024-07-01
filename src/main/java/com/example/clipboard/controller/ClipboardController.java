package com.example.clipboard.controller;

import com.example.clipboard.model.Clipboard;
import com.example.clipboard.repository.ClipboardRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clipboard")
public class ClipboardController {

    private final ClipboardRepository clipboardRepository;

    public ClipboardController(ClipboardRepository clipboardRepository) {
        this.clipboardRepository = clipboardRepository;
    }

    @GetMapping
    public List<Clipboard> getAllClipboard() {
        return clipboardRepository.findAllOrderByCreatedAtDesc();
    }

    @GetMapping("/latest")
    public Clipboard getLatestClipboard() {
        return (clipboardRepository.findAllOrderByCreatedAtDesc()).get(0);
    }

    @PostMapping
    public Clipboard createClipboard(@RequestBody Clipboard clipboard) throws Exception {
        if (clipboard.getContent().isEmpty()) {
            throw new Exception("Content is empty");
        }
        return clipboardRepository.save(clipboard);
    }

    @PutMapping("/{id}")
    public Clipboard updateClipboard(@PathVariable Long id, @RequestBody Clipboard clipboard) throws Exception {
        if (clipboard.getContent().isEmpty()) {
            throw new Exception("Content is empty");
        }
        Clipboard existingClipboard = clipboardRepository.findById(id).orElseThrow();
        existingClipboard.setContent(clipboard.getContent());
        return clipboardRepository.save(existingClipboard);
    }

    @DeleteMapping("/{id}")
    public void deleteClipboard(@PathVariable Long id) {
        clipboardRepository.deleteById(id);
    }
}
