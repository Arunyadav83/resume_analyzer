package com.arun.ResumeAnalyzer.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.arun.ResumeAnalyzer.Entity.Resume;
import com.arun.ResumeAnalyzer.dao.ResumeDAO;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeService {

    @Autowired
    private ResumeDAO resumeRepository;

    public Map<String, String> processFile(MultipartFile file) throws IOException, TikaException {
        String resumeText = extractTextFromFile(file);
        System.out.println("Extracted Resume Text:\n" + resumeText);

        Map<String, String> parsedData = parseResumeData(resumeText);
        saveResumeData(parsedData);

        return parsedData;
    }

    private String extractTextFromFile(MultipartFile file) throws IOException, TikaException {
        Tika tika = new Tika();
        return tika.parseToString(file.getInputStream());
    }

    private Map<String, String> parseResumeData(String resumeText) {
        Map<String, String> data = new HashMap<>();

        Map<String, List<String>> sectionKeywords = Map.of(
                "Summary", List.of("summary", "profile", "objective", "professional summary", "about me"),
                "Education", List.of("education", "academic", "qualifications", "educational qualifications"),
                "Experience", List.of("experience", "work", "employment", "career", "professional experience" ,"WORK EXPERIENCE"),
                "Personal Information", List.of("contact", "personal", "details", "bio data", "personal information"),
                "Skills", List.of("key skills", "technologies", "tools", "abilities", "technical skills", "languages", "frameworks", "skills", "programming")
        );

        Map<String, StringBuilder> sections = new HashMap<>();
        String currentSection = null;
        StringBuilder initialSummary = new StringBuilder();

        String[] lines = resumeText.split("\\r?\\n");

        for (String line : lines) {
            String cleanLine = line.trim();
            String lowerLine = cleanLine.toLowerCase();

            if (cleanLine.isBlank() || cleanLine.matches(".*\\.(png|jpg|jpeg|gif).*")) continue;

            boolean foundSection = false;

            for (Map.Entry<String, List<String>> entry : sectionKeywords.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (lowerLine.matches("^\\s*" + Pattern.quote(keyword) + "\\s*:?$")) {
                        currentSection = entry.getKey();
                        sections.putIfAbsent(currentSection, new StringBuilder());
                        foundSection = true;
                        break;
                    }
                }
                if (foundSection) break;
            }

            if (!foundSection) {
                if (currentSection != null) {
                    sections.get(currentSection).append(cleanLine).append("\n");
                } else {
                    initialSummary.append(cleanLine).append("\n");
                }
            }
        }

        // Extract professional experience from summary if it exists
        String summaryText = sections.getOrDefault("Summary", new StringBuilder(initialSummary)).toString().trim();
        String experienceText = sections.getOrDefault("Experience", new StringBuilder("Not Found")).toString().trim();
        
        // If experience is "Not Found" but summary contains professional experience
        if (experienceText.equals("Not Found") && summaryText.toLowerCase().contains("professional experience")) {
            // Split summary into parts
            String[] summaryParts = summaryText.split("(?i)professional experience");
            if (summaryParts.length > 1) {
                // First part is the actual summary
                String actualSummary = summaryParts[0].trim();
                // Second part is the experience
                String extractedExperience = summaryParts[1].trim();
                
                // Update both sections
                data.put("Summary", actualSummary);
                data.put("Experience", extractedExperience);
                
                // Remove the experience part from the sections map to prevent duplication
                sections.put("Summary", new StringBuilder(actualSummary));
                sections.put("Experience", new StringBuilder(extractedExperience));
            }
        }

        // Extract personal info
        StringBuilder personalInfo = new StringBuilder();
        String name = extractName(resumeText);
        String email = extractEmail(resumeText);
        String phone = extractPhone(resumeText);

        if (name != null) personalInfo.append("Name: ").append(name).append("\n");
        if (email != null) personalInfo.append("Email: ").append(email).append("\n");
        if (phone != null) personalInfo.append("Phone: ").append(phone).append("\n");

        // Assign sections to data map
        for (String section : sectionKeywords.keySet()) {
            if (section.equals("Summary")) {
                data.putIfAbsent(section, summaryText); // Only put if not already processed
            } else if (section.equals("Personal Information")) {
                data.put(section, personalInfo.toString().trim());
            } else if (section.equals("Experience")) {
                data.putIfAbsent(section, experienceText); // Only put if not already processed
            } else {
                String sectionContent = sections.getOrDefault(section, new StringBuilder("Not Found")).toString().trim();
                data.put(section, sectionContent);
            }
        }

        return data;
    }
    private String extractEmail(String text) {
        Matcher matcher = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractPhone(String text) {
        Matcher matcher = Pattern.compile("(\\+\\d{1,3}[\\s-]?)?\\d{10}").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractName(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty() && !line.toLowerCase().contains("resume")) {
                return line.trim();
            }
        }
        return null;
    }

    private void saveResumeData(Map<String, String> parsedData) {
        Resume resume = new Resume();
        resume.setSummary(parsedData.get("Summary"));
        resume.setEducation(parsedData.get("Education"));
        resume.setExperience(parsedData.get("Experience"));
        resume.setPersonalInformation(parsedData.get("Personal Information"));
        resume.setSkills(parsedData.get("Skills"));
        resumeRepository.save(resume);
    }

    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    public Optional<Resume> getResumeById(Long id) {
        return resumeRepository.findById(id);
    }
    public void deleteResumeById(Long id) {
    	resumeRepository.deleteById(id);
    }
}


