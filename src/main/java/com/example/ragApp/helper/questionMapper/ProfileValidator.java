package com.example.ragApp.helper.questionMapper;



import com.example.ragApp.data.UserProfile;
import com.example.ragApp.helper.UseCaseType;

import java.util.*;
import java.util.function.Function;

public class ProfileValidator {

    public static List<Integer> getMissingFields(UserProfile profile, UseCaseType useCase) {

        List<Integer> requiredCodes =
                UseCaseMandatoryQuestions.USE_CASE_MAP.get(useCase);

        List<Integer> missing = new ArrayList<>();

        for (Integer code : requiredCodes) {

            Function<UserProfile, Object> extractor =
                    ProfileQuestionMapper.QUESTION_FIELD_MAP.get(code);

            if (extractor == null) continue;

            Object value = extractor.apply(profile);

            if (value == null ||
                    (value instanceof String && ((String) value).isBlank()) ||
                    (value instanceof Collection && ((Collection<?>) value).isEmpty())) {

                missing.add(code);
            }
        }

        return missing;
    }
}
