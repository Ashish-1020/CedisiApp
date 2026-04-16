package com.example.ragApp.helper.questionMapper;

import com.example.ragApp.helper.UseCaseType;

import java.util.List;
import java.util.Map;

public class UseCaseMandatoryQuestions {

    public static final Map<UseCaseType, List<Integer>> USE_CASE_MAP =
            Map.ofEntries(

                    Map.entry(UseCaseType.BUSINESS_FINANCE_ADVICE,
                            List.of(14,19,20,16,34,36,38,41,42,43,27,28,29,30,31,32)),

                    Map.entry(UseCaseType.PREPARE_BANK_LOAN,
                            List.of(5,6,14,19,20,42,43,16,33,35)),

                    Map.entry(UseCaseType.ELIGIBILITY_CHECKER,
                            List.of(5,6,7,8,11,14,15,42,43,45)),

                    Map.entry(UseCaseType.CAPITAL_DEPLOYMENT,
                            List.of(14,20,23,25,38,41)),

                    Map.entry(UseCaseType.REVIEW_BALANCE_SHEET,
                            List.of(19,20,33,34,35,36,38)),

                    Map.entry(UseCaseType.SCALE_UP_PLAN,
                            List.of(14,20,17,18,44,29,31)),

                    Map.entry(UseCaseType.MARKETING_GROWTH,
                            List.of(28,29,30,31,32,17)),

                    Map.entry(UseCaseType.HR_EFFICIENCY,
                            List.of(15,17,18,21)),

                    Map.entry(UseCaseType.EXPORT_PROCESS,
                            List.of(6,8,9,29,28,14,11)),

                    Map.entry(UseCaseType.IMPORT_PROCESS,
                            List.of(6,8,9,27,14)),

                    Map.entry(UseCaseType.PAYMENT_RECOVERY_EMAIL,
                            List.of(6,39,14))
            );
}