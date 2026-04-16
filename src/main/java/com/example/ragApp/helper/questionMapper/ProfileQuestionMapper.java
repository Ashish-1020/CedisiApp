package com.example.ragApp.helper.questionMapper;

import com.example.ragApp.data.UserProfile;

import java.util.Map;
import java.util.function.Function;

public class ProfileQuestionMapper {

    public static final Map<Integer, Function<UserProfile, Object>> QUESTION_FIELD_MAP = Map.ofEntries(

            Map.entry(5, UserProfile::getLegalStructure),
            Map.entry(6, UserProfile::getAdditionalRegistrations),
            Map.entry(7, UserProfile::getPrimaryBusinessCategory),
            Map.entry(8, UserProfile::getIndustrySector),
            Map.entry(9, UserProfile::getProductCategory),
            Map.entry(10, UserProfile::getOperatingSetup),
            Map.entry(11, UserProfile::getRegisteredOfficeState),
            Map.entry(13, UserProfile::getOperationalStates),
            Map.entry(14, UserProfile::getAnnualTurnover),
            Map.entry(15, UserProfile::getEmployees),
            Map.entry(16, UserProfile::getRunningLoan),
            Map.entry(17, UserProfile::getBiggestBusinessChallenge),
            Map.entry(18, UserProfile::getTopGoal6Months),
            Map.entry(19, UserProfile::getTurnoverYear1),
            Map.entry(20, UserProfile::getAverageMarginRange),
            Map.entry(21, UserProfile::getManufacturingType),
            Map.entry(23, UserProfile::getMonthlyProductionQuantity),
            Map.entry(25, UserProfile::getCapacityUtilization),
            Map.entry(27, UserProfile::getTradingType),
            Map.entry(28, UserProfile::getPrimaryCustomerType),
            Map.entry(29, UserProfile::getSalesGeography),
            Map.entry(30, UserProfile::getAverageOrderValue),
            Map.entry(31, UserProfile::getPrimarySalesChannel),
            Map.entry(32, UserProfile::getMonthlyMarketingBudget),
            Map.entry(33, UserProfile::getTotalLoanTakenRange),
            Map.entry(34, UserProfile::getOutstandingLoanRange),
            Map.entry(35, UserProfile::getLoanSources),
            Map.entry(36, UserProfile::getInterestRateRange),
            Map.entry(38, UserProfile::getBiggestFinancialConcern),
            Map.entry(39, UserProfile::getCreditGivenToCustomers),
            Map.entry(41, UserProfile::getWorkingCapitalCycle),
            Map.entry(42, UserProfile::getCibilScoreRange),
            Map.entry(43, UserProfile::getCollateralAvailable),
            Map.entry(44, UserProfile::getGrowthPlan),
            Map.entry(45, UserProfile::getGovernmentSupportInterest)
    );
}
