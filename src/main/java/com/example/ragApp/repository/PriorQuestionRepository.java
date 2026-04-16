package com.example.ragApp.repository;


import com.example.ragApp.data.PriorQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriorQuestionRepository extends JpaRepository<PriorQuestion, UUID> {

    List<PriorQuestion> findByPromptTemplateIdOrderByQuestionOrder(UUID promptTemplateId);
}
