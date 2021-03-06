package com.rbs.testharness.model;

public class PricingTestCaseResponse {
	
	private Integer testTransactionId;
	private Integer testSetId;
	private String testTransactionNo;
	private String applicationIdentity;
	private String bankDivision;
	private String productFamily;
	private String productName;
	private Integer riskBand;
	private Integer borrowingAmount;
	private Integer termFactor;
	private Double actualAir;
	private Double actualApr;
	private Double expectetAir;
	private Double expectetApr;
	private Character testTransactionFlag;
	private String xmlDifference;
	private Long totalRecord;
	
	public Integer getTestTransactionId() {
		return testTransactionId;
	}
	public void setTestTransactionId(Integer testTransactionId) {
		this.testTransactionId = testTransactionId;
	}
	public Integer getTestSetId() {
		return testSetId;
	}
	public void setTestSetId(Integer testSetId) {
		this.testSetId = testSetId;
	}
	public String getTestTransactionNo() {
		return testTransactionNo;
	}
	public void setTestTransactionNo(String testTransactionNo) {
		this.testTransactionNo = testTransactionNo;
	}
	public String getApplicationIdentity() {
		return applicationIdentity;
	}
	public void setApplicationIdentity(String applicationIdentity) {
		this.applicationIdentity = applicationIdentity;
	}
	public String getBankDivision() {
		return bankDivision;
	}
	public void setBankDivision(String bankDivision) {
		this.bankDivision = bankDivision;
	}
	public String getProductFamily() {
		return productFamily;
	}
	public void setProductFamily(String productFamily) {
		this.productFamily = productFamily;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public Integer getRiskBand() {
		return riskBand;
	}
	public void setRiskBand(Integer riskBand) {
		this.riskBand = riskBand;
	}
	public Integer getBorrowingAmount() {
		return borrowingAmount;
	}
	public void setBorrowingAmount(Integer borrowingAmount) {
		this.borrowingAmount = borrowingAmount;
	}
	public Integer getTermFactor() {
		return termFactor;
	}
	public void setTermFactor(Integer termFactor) {
		this.termFactor = termFactor;
	}
	public Double getActualAir() {
		return actualAir;
	}
	public void setActualAir(Double actualAir) {
		this.actualAir = actualAir;
	}
	public Double getActualApr() {
		return actualApr;
	}
	public void setActualApr(Double actualApr) {
		this.actualApr = actualApr;
	}
	public Double getExpectetAir() {
		return expectetAir;
	}
	public void setExpectetAir(Double expectetAir) {
		this.expectetAir = expectetAir;
	}
	public Double getExpectetApr() {
		return expectetApr;
	}
	public void setExpectetApr(Double expectetApr) {
		this.expectetApr = expectetApr;
	}
	public Character getTestTransactionFlag() {
		return testTransactionFlag;
	}
	public void setTestTransactionFlag(Character testTransactionFlag) {
		this.testTransactionFlag = testTransactionFlag;
	}
	public String getXmlDifference() {
		return xmlDifference;
	}
	public void setXmlDifference(String xmlDifference) {
		this.xmlDifference = xmlDifference;
	}
	public Long getTotalRecord() {
		return totalRecord;
	}
	public void setTotalRecord(Long totalRecord) {
		this.totalRecord = totalRecord;
	}
}
