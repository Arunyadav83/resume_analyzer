package com.arun.ResumeAnalyzer.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.arun.ResumeAnalyzer.Entity.Resume;

public interface ResumeDAO extends JpaRepository<Resume, Long> {
}

