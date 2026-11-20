package com.arun.ResumeAnalyzer.Controller;

import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.arun.ResumeAnalyzer.Entity.Resume;
import com.arun.ResumeAnalyzer.service.ResumeService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) throws TikaException {
        try {
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());

            Map<String, String> parsedData = resumeService.processFile(file);
            return ResponseEntity.ok(parsedData);

        } catch (IOException e) {
            e.printStackTrace(); // Optional: Print the full stack trace
            return ResponseEntity.status(500).body(Map.of("error", "Error processing the file"));
        }
    }
    
    // ✅ GET method to retrieve all resume data
    @GetMapping("/all")
    public List<Resume> getAllResumes() {
        return resumeService.getAllResumes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resume> getResumeById(@PathVariable Long id) {
        return resumeService.getResumeById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Resume not found with ID: " + id));
    }


    // Delete resume by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteResume(@PathVariable("id") Long id) {
        try {
            resumeService.deleteResumeById(id);
            return ResponseEntity.ok("Resume with ID " + id + " deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting resume with ID " + id);
        }
    }



}
