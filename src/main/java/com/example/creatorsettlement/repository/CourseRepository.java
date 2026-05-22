package com.example.creatorsettlement.repository;

import com.example.creatorsettlement.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
