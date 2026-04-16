package com.example.ragApp.data;


import jakarta.persistence.*;

import java.util.List;


@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private String userId;

    // ---------------- SECTION 1 : BUSINESS IDENTITY ----------------

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer yearOfIncorporation;

    @Column(nullable = false)
    private Integer operationStartYear;

    private String website;

    @Column(nullable = false)
    private String legalStructure;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_additional_registrations",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "additional_registration")
    private List<String> additionalRegistrations;

    @Column(nullable = false)
    private String primaryBusinessCategory;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_industry_sector",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "industry_sector")
    private List<String> industrySector;

    @Column(nullable = false)
    private String productCategory;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_operating_setup",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "operating_setup")
    private List<String> operatingSetup;

    @Column(nullable = false)
    private String registeredOfficeState;

    @Column(nullable = false)
    private String registeredOfficeCity;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_operational_states",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "operational_state")
    private List<String> operationalStates;

    @Column(nullable = false)
    private String annualTurnover;

    @Column(nullable = false)
    private String employees;

    @Column(nullable = false)
    private Boolean runningLoan;

    @Column(nullable = false)
    private String biggestBusinessChallenge;

    @Column(nullable = false)
    private String topGoal6Months;

    // ---------------- SECTION 2 : FINANCE ----------------

    private Double turnoverYear1;
    private Double turnoverYear2;
    private Double turnoverYear3;

    private String averageMarginRange;

    // ---------------- SECTION 3 : MANUFACTURING ----------------

    private String manufacturingType;

    private String machineryStatus;

    private Double monthlyProductionQuantity;

    private String productionUnit;

    private String capacityUtilization;

    private String rawMaterialSource;

    // ---------------- SECTION 4 : TRADING ----------------

    private String tradingType;

    // ---------------- SECTION 5 : SALES & MARKET ----------------

    @ElementCollection
    @CollectionTable(
            name = "user_profile_primary_customer_type",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "customer_type")
    private List<String> primaryCustomerType;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_sales_geography",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "sales_geography")
    private List<String> salesGeography;

    private String averageOrderValue;

    private String primarySalesChannel;

    private String monthlyMarketingBudget;

    // ---------------- SECTION 6 : FINANCE & WORKING CAPITAL ----------------

    private String totalLoanTakenRange;

    private String outstandingLoanRange;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_loan_sources",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "loan_source")
    private List<String> loanSources;

    private String interestRateRange;

    private String fundingStatus;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_biggest_financial_concern",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "financial_concern")
    private List<String> biggestFinancialConcern;

    private String creditGivenToCustomers;

    private String creditReceivedFromSuppliers;

    private String workingCapitalCycle;

    private String cibilScoreRange;

    private String collateralAvailable;

    // ---------------- SECTION 7 : GROWTH ----------------

    @ElementCollection
    @CollectionTable(
            name = "user_profile_growth_plan",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "growth_plan")
    private List<String> growthPlan;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_government_support_interest",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "support_interest")
    private List<String> governmentSupportInterest;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYearOfIncorporation() {
        return yearOfIncorporation;
    }

    public void setYearOfIncorporation(Integer yearOfIncorporation) {
        this.yearOfIncorporation = yearOfIncorporation;
    }

    public Integer getOperationStartYear() {
        return operationStartYear;
    }

    public void setOperationStartYear(Integer operationStartYear) {
        this.operationStartYear = operationStartYear;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLegalStructure() {
        return legalStructure;
    }

    public void setLegalStructure(String legalStructure) {
        this.legalStructure = legalStructure;
    }

    public List<String> getAdditionalRegistrations() {
        return additionalRegistrations;
    }

    public void setAdditionalRegistrations(List<String> additionalRegistrations) {
        this.additionalRegistrations = additionalRegistrations;
    }

    public String getPrimaryBusinessCategory() {
        return primaryBusinessCategory;
    }

    public void setPrimaryBusinessCategory(String primaryBusinessCategory) {
        this.primaryBusinessCategory = primaryBusinessCategory;
    }

    public List<String> getIndustrySector() {
        return industrySector;
    }

    public void setIndustrySector(List<String> industrySector) {
        this.industrySector = industrySector;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public List<String> getOperatingSetup() {
        return operatingSetup;
    }

    public void setOperatingSetup(List<String> operatingSetup) {
        this.operatingSetup = operatingSetup;
    }

    public String getRegisteredOfficeState() {
        return registeredOfficeState;
    }

    public void setRegisteredOfficeState(String registeredOfficeState) {
        this.registeredOfficeState = registeredOfficeState;
    }

    public String getRegisteredOfficeCity() {
        return registeredOfficeCity;
    }

    public void setRegisteredOfficeCity(String registeredOfficeCity) {
        this.registeredOfficeCity = registeredOfficeCity;
    }

    public List<String> getOperationalStates() {
        return operationalStates;
    }

    public void setOperationalStates(List<String> operationalStates) {
        this.operationalStates = operationalStates;
    }

    public String getAnnualTurnover() {
        return annualTurnover;
    }

    public void setAnnualTurnover(String annualTurnover) {
        this.annualTurnover = annualTurnover;
    }

    public String getEmployees() {
        return employees;
    }

    public void setEmployees(String employees) {
        this.employees = employees;
    }

    public Boolean getRunningLoan() {
        return runningLoan;
    }

    public void setRunningLoan(Boolean runningLoan) {
        this.runningLoan = runningLoan;
    }

    public String getBiggestBusinessChallenge() {
        return biggestBusinessChallenge;
    }

    public void setBiggestBusinessChallenge(String biggestBusinessChallenge) {
        this.biggestBusinessChallenge = biggestBusinessChallenge;
    }

    public String getTopGoal6Months() {
        return topGoal6Months;
    }

    public void setTopGoal6Months(String topGoal6Months) {
        this.topGoal6Months = topGoal6Months;
    }

    public Double getTurnoverYear1() {
        return turnoverYear1;
    }

    public void setTurnoverYear1(Double turnoverYear1) {
        this.turnoverYear1 = turnoverYear1;
    }

    public Double getTurnoverYear2() {
        return turnoverYear2;
    }

    public void setTurnoverYear2(Double turnoverYear2) {
        this.turnoverYear2 = turnoverYear2;
    }

    public Double getTurnoverYear3() {
        return turnoverYear3;
    }

    public void setTurnoverYear3(Double turnoverYear3) {
        this.turnoverYear3 = turnoverYear3;
    }

    public String getAverageMarginRange() {
        return averageMarginRange;
    }

    public void setAverageMarginRange(String averageMarginRange) {
        this.averageMarginRange = averageMarginRange;
    }

    public String getManufacturingType() {
        return manufacturingType;
    }

    public void setManufacturingType(String manufacturingType) {
        this.manufacturingType = manufacturingType;
    }

    public String getMachineryStatus() {
        return machineryStatus;
    }

    public void setMachineryStatus(String machineryStatus) {
        this.machineryStatus = machineryStatus;
    }

    public Double getMonthlyProductionQuantity() {
        return monthlyProductionQuantity;
    }

    public void setMonthlyProductionQuantity(Double monthlyProductionQuantity) {
        this.monthlyProductionQuantity = monthlyProductionQuantity;
    }

    public String getProductionUnit() {
        return productionUnit;
    }

    public void setProductionUnit(String productionUnit) {
        this.productionUnit = productionUnit;
    }

    public String getCapacityUtilization() {
        return capacityUtilization;
    }

    public void setCapacityUtilization(String capacityUtilization) {
        this.capacityUtilization = capacityUtilization;
    }

    public String getRawMaterialSource() {
        return rawMaterialSource;
    }

    public void setRawMaterialSource(String rawMaterialSource) {
        this.rawMaterialSource = rawMaterialSource;
    }

    public String getTradingType() {
        return tradingType;
    }

    public void setTradingType(String tradingType) {
        this.tradingType = tradingType;
    }

    public List<String> getPrimaryCustomerType() {
        return primaryCustomerType;
    }

    public void setPrimaryCustomerType(List<String> primaryCustomerType) {
        this.primaryCustomerType = primaryCustomerType;
    }

    public List<String> getSalesGeography() {
        return salesGeography;
    }

    public void setSalesGeography(List<String> salesGeography) {
        this.salesGeography = salesGeography;
    }

    public String getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(String averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public String getPrimarySalesChannel() {
        return primarySalesChannel;
    }

    public void setPrimarySalesChannel(String primarySalesChannel) {
        this.primarySalesChannel = primarySalesChannel;
    }

    public String getMonthlyMarketingBudget() {
        return monthlyMarketingBudget;
    }

    public void setMonthlyMarketingBudget(String monthlyMarketingBudget) {
        this.monthlyMarketingBudget = monthlyMarketingBudget;
    }

    public String getTotalLoanTakenRange() {
        return totalLoanTakenRange;
    }

    public void setTotalLoanTakenRange(String totalLoanTakenRange) {
        this.totalLoanTakenRange = totalLoanTakenRange;
    }

    public String getOutstandingLoanRange() {
        return outstandingLoanRange;
    }

    public void setOutstandingLoanRange(String outstandingLoanRange) {
        this.outstandingLoanRange = outstandingLoanRange;
    }

    public List<String> getLoanSources() {
        return loanSources;
    }

    public void setLoanSources(List<String> loanSources) {
        this.loanSources = loanSources;
    }

    public String getInterestRateRange() {
        return interestRateRange;
    }

    public void setInterestRateRange(String interestRateRange) {
        this.interestRateRange = interestRateRange;
    }

    public String getFundingStatus() {
        return fundingStatus;
    }

    public void setFundingStatus(String fundingStatus) {
        this.fundingStatus = fundingStatus;
    }

    public List<String> getBiggestFinancialConcern() {
        return biggestFinancialConcern;
    }

    public void setBiggestFinancialConcern(List<String> biggestFinancialConcern) {
        this.biggestFinancialConcern = biggestFinancialConcern;
    }

    public String getCreditGivenToCustomers() {
        return creditGivenToCustomers;
    }

    public void setCreditGivenToCustomers(String creditGivenToCustomers) {
        this.creditGivenToCustomers = creditGivenToCustomers;
    }

    public String getCreditReceivedFromSuppliers() {
        return creditReceivedFromSuppliers;
    }

    public void setCreditReceivedFromSuppliers(String creditReceivedFromSuppliers) {
        this.creditReceivedFromSuppliers = creditReceivedFromSuppliers;
    }

    public String getWorkingCapitalCycle() {
        return workingCapitalCycle;
    }

    public void setWorkingCapitalCycle(String workingCapitalCycle) {
        this.workingCapitalCycle = workingCapitalCycle;
    }

    public String getCibilScoreRange() {
        return cibilScoreRange;
    }

    public void setCibilScoreRange(String cibilScoreRange) {
        this.cibilScoreRange = cibilScoreRange;
    }

    public String getCollateralAvailable() {
        return collateralAvailable;
    }

    public void setCollateralAvailable(String collateralAvailable) {
        this.collateralAvailable = collateralAvailable;
    }

    public List<String> getGrowthPlan() {
        return growthPlan;
    }

    public void setGrowthPlan(List<String> growthPlan) {
        this.growthPlan = growthPlan;
    }

    public List<String> getGovernmentSupportInterest() {
        return governmentSupportInterest;
    }

    public void setGovernmentSupportInterest(List<String> governmentSupportInterest) {
        this.governmentSupportInterest = governmentSupportInterest;
    }
}




