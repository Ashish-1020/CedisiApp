package com.example.ragApp.dto;


public record BusinessQueryRequest(
        String businessName,
        String legalStructure,
        String primaryBusinessCategory,
        String industrySector,
        String productCategory,
        String city,
        String state,
        String businessStage,
        String annualTurnover,
        String employees,
        String question,
        String language

) {

}
