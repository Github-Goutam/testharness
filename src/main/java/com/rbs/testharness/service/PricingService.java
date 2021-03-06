package com.rbs.testharness.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rbs.testharness.common.GenerateExcelReport;
import com.rbs.testharness.common.GeneratePdfReport;
import com.rbs.testharness.common.THConstant;
import com.rbs.testharness.common.THException;
import com.rbs.testharness.entity.PricingBusinessAttributeEntity;
import com.rbs.testharness.entity.PricingLookUpEntity;
import com.rbs.testharness.entity.PricingTestCaseResponseEntity;
import com.rbs.testharness.entity.PricingTestSetEntity;
import com.rbs.testharness.helper.PricingHelper;
import com.rbs.testharness.helper.ReferenceDataHelper;
import com.rbs.testharness.model.PricingAttributeRequest;
import com.rbs.testharness.model.PricingBusinessAttribute;
import com.rbs.testharness.model.PricingTestCaseResponse;
import com.rbs.testharness.model.PricingTestCaseResult;
import com.rbs.testharness.model.PricingTestSet;
import com.rbs.testharness.repository.PricingBusinessAttributeRepository;
import com.rbs.testharness.repository.PricingLookUpRepository;
import com.rbs.testharness.repository.PricingTestCaseResponseRepository;
import com.rbs.testharness.repository.PricingTestSetRepository;
import com.rbs.testharness.helper.TestScenarioHelper;
import com.rbs.testharness.common.GenerateTestScenarioExcel;

@Service
public class PricingService {
	
	@Autowired
	private PricingHelper pricingHelper;
	
	@Autowired
	private ReferenceDataHelper referenceDataHelper;
	
	@Autowired
	PricingBusinessAttributeRepository parameterAttributeRepository;
	
	@Autowired
	PricingTestSetRepository pricingTestSetRepository;
	
	@Autowired
	PricingTestCaseResponseRepository pricingTestCaseResponseRepository;
	
	@Autowired
	PricingLookUpRepository pricingLookUpRepository;
	
	@Autowired
	GeneratePdfReport generatePdfReport;
	
	@Autowired
	GenerateExcelReport generateExcelReport;
	
	@Autowired
	GenerateTestScenarioExcel generateTestScenarioExcel;
	
	@Autowired
	private TestScenarioHelper testScenarioHelper;
	
	/*
	 * 
	 */
	public List<PricingBusinessAttribute> businessAttributes(){
		List<PricingBusinessAttributeEntity> parameterAttributeEntityList=parameterAttributeRepository.findAll();
		List<PricingBusinessAttribute> parameterAttributeList=new ArrayList<>();
		if(parameterAttributeEntityList!=null && parameterAttributeEntityList.size()>0) {
			parameterAttributeEntityList.forEach(page->{
				PricingBusinessAttribute testCase=new PricingBusinessAttribute();
				BeanUtils.copyProperties(page, testCase);
				parameterAttributeList.add(testCase);
			});
		}else {
			throw new THException(HttpStatus.NOT_FOUND,"Parameter attribute not found","Not found");
		}
		return parameterAttributeList;
	}
	
	
	/*
	 * This method will generate the Test case Combination based on the attribute inpputs
	 */
	public List<PricingTestCaseResponse> generateTestCaseCombination(PricingAttributeRequest attributeInput) {
		final List<List<Integer>> lists = new ArrayList<List<Integer>>();
		lists.add(new ArrayList<>(attributeInput.getBorrowingAmount()));
		lists.add(new ArrayList<>(attributeInput.getRiskFactor()));
		lists.add(new ArrayList<>(attributeInput.getTermFactor()));
		//Logic to add the record in the Test_Set Table
		String borrowingAmount = attributeInput.getBorrowingAmount().stream().map(String::valueOf).collect(Collectors.joining(","));
		String riskBand = attributeInput.getRiskFactor().stream().map(String::valueOf).collect(Collectors.joining(","));
		String termFactor = attributeInput.getTermFactor().stream().map(String::valueOf).collect(Collectors.joining(","));
		PricingTestSetEntity testSetEntity=new PricingTestSetEntity();
		BeanUtils.copyProperties(attributeInput, testSetEntity);
		testSetEntity.setBorrowingAmount(borrowingAmount);
		testSetEntity.setRiskBand(riskBand);
		testSetEntity.setTermFactor(termFactor);
		testSetEntity=pricingTestSetRepository.save(testSetEntity);
		//Logic to create test case combination
		final List<Integer> list4 = new ArrayList<Integer>();
		PricingHelper.permute(lists, (permutation -> list4.addAll(permutation)));
		System.out.println("Total testcase size::"+list4.size());
		int j = 0;
		int transactionId=1;
		List<PricingTestCaseResponseEntity> pricingTestCaseResponseEntityList = new ArrayList<>();

		for (int i = 0; i < list4.size() / 3; i++) {
			PricingTestCaseResponseEntity pricingTestCaseResponseEntity = new PricingTestCaseResponseEntity();
			pricingTestCaseResponseEntity.setTestSetId(testSetEntity.getTestSetId());
			String counter = String.format("%05d", transactionId++); 
			pricingTestCaseResponseEntity.setTestTransactionNo("TH_"+testSetEntity.getTestSetId()+"_"+ counter);
			pricingTestCaseResponseEntity.setTestTransactionFlag(THConstant.TestCase_Processed_N);
			pricingTestCaseResponseEntity.setApplicationIdentity(attributeInput.getApplicationIdentity());
			pricingTestCaseResponseEntity.setBankDivision(attributeInput.getBankDivision());
			pricingTestCaseResponseEntity.setProductFamily(attributeInput.getProductFamily());
			pricingTestCaseResponseEntity.setProductName(attributeInput.getProductName());
			pricingTestCaseResponseEntity.setBorrowingAmount(list4.get(j++));
			pricingTestCaseResponseEntity.setRiskBand(list4.get(j++));
			pricingTestCaseResponseEntity.setTermFactor(list4.get(j++));
			pricingTestCaseResponseEntityList.add(pricingTestCaseResponseEntity);
		}
		//Saving to Transaction Testcase DB
		pricingTestCaseResponseEntityList=pricingTestCaseResponseRepository.saveAll(pricingTestCaseResponseEntityList);
		List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
		if(pricingTestCaseResponseEntityList!=null && !pricingTestCaseResponseEntityList.isEmpty()) {
			Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
			for(PricingTestCaseResponseEntity resposeList:pricingTestCaseResponseEntityList) {
				PricingTestCaseResponse testCase=new PricingTestCaseResponse();
				BeanUtils.copyProperties(resposeList, testCase);
				testCase.setApplicationIdentity(businessAttributeMap.get(resposeList.getApplicationIdentity()));
				testCase.setBankDivision(businessAttributeMap.get(resposeList.getBankDivision()));
				testCase.setProductName(businessAttributeMap.get(resposeList.getProductName()));
				testCase.setProductFamily(businessAttributeMap.get(resposeList.getProductFamily()));
				testCase.setTotalRecord((long) pricingTestCaseResponseEntityList.size());
				pricingTestCaseResponseList.add(testCase);
			}
		}else {
			throw new THException(HttpStatus.NOT_FOUND,"Test Case not found","Not found");
		}
		return pricingTestCaseResponseList;
	}
	/*
	 * This method will generate the Test case AIR & APR from the lookup table data
	 */
	public List<PricingTestCaseResponse> generateTestCaseAirApr(Integer testSetId) {
		List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
		List<PricingTestCaseResponseEntity> pricingTestCaseResponseEntityList=new ArrayList<>();

		if(null!=testSetId) {
			Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseEntityLists=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
			if(pricingTestCaseResponseEntityLists.isPresent()) {
				pricingTestCaseResponseEntityLists.get().forEach(pricingTestCaseResponses->{
					PricingTestCaseResponseEntity pricingTestCaseResponseEntity=new PricingTestCaseResponseEntity();
					BeanUtils.copyProperties(pricingTestCaseResponses, pricingTestCaseResponseEntity);
					Integer term=pricingTestCaseResponses.getTermFactor();
					Integer borrowingAmount=pricingTestCaseResponses.getBorrowingAmount();
					Integer risk=pricingTestCaseResponses.getRiskBand();
					//Forming the Term Factor Range
					String termRange=THConstant.Empty_String;
					if(term>=12 && term<=35){
						termRange=THConstant.Term_Range_12to35;
					}
					else if(term>=36 && term<=59){
						termRange=THConstant.Term_Range_36to59;
					}
					else if(term>=60 && term<=120){
						termRange=THConstant.Term_Range_60to120;
					}
					//Forming the Risk Band Range
					String borrowingAmountRange=THConstant.Empty_String;
					if(borrowingAmount>=1000 && borrowingAmount<5000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_1000to5000;
					}
					else if(borrowingAmount>=5000 && borrowingAmount<7500) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_5000to7500;
					}
					else if(borrowingAmount>=6500 && borrowingAmount<10000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_7500to10000;
					}
					else if(borrowingAmount>=10000 && borrowingAmount<15000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_10000to15000;
					}
					else if(borrowingAmount>=15000 && borrowingAmount<20000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_15000to20000;
					}
					else if(borrowingAmount>=20000 && borrowingAmount<25000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_20000to25000;
					}
					else if(borrowingAmount>=25000 && borrowingAmount<30000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_25000to30000;
					}
					else if(borrowingAmount>=30000 && borrowingAmount<35000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_30000to35000;
					}
					else if(borrowingAmount>=35000 && borrowingAmount<40000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_35000to40000;
					}
					else if(borrowingAmount>=40000 && borrowingAmount<45000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_40000to45000;
					}
					else if(borrowingAmount>=45000 && borrowingAmount<50000) {
						borrowingAmountRange=THConstant.BorrowingAmount_Range_45000to50000;
					}
					//Making DB call to retrive the AIR & APR
					Optional<PricingLookUpEntity> pricingLookUp=pricingLookUpRepository.findByRiskBandAndTermFactorAndBorrowingAmount(risk,termRange,borrowingAmountRange);
					if(pricingLookUp.isPresent()) {
						pricingTestCaseResponseEntity.setExpectetAir(pricingLookUp.get().getAirRate());
						pricingTestCaseResponseEntity.setExpectetApr(pricingLookUp.get().getAprRate());
					}
					else {
						pricingTestCaseResponseEntity.setExpectetAir(THConstant.Default_Air);
						pricingTestCaseResponseEntity.setExpectetApr(THConstant.Default_Apr);
					}
					pricingTestCaseResponseEntityList.add(pricingTestCaseResponseEntity);
					//Saving one by one
					pricingTestCaseResponseRepository.save(pricingTestCaseResponseEntity);
				});
				Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseWithAirApr=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
				if(null!=pricingTestCaseResponseWithAirApr && pricingTestCaseResponseWithAirApr.get().size()>0) {
					Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
					pricingTestCaseResponseWithAirApr.get().forEach(pricingTestCaseResponseAirApr->{
						PricingTestCaseResponse pricingTestCaseResponse=new PricingTestCaseResponse();
						BeanUtils.copyProperties(pricingTestCaseResponseAirApr, pricingTestCaseResponse);
						pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(pricingTestCaseResponseAirApr.getApplicationIdentity()));
						pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(pricingTestCaseResponseAirApr.getBankDivision()));
						pricingTestCaseResponse.setProductName(businessAttributeMap.get(pricingTestCaseResponseAirApr.getProductName()));
						pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(pricingTestCaseResponseAirApr.getProductFamily()));
						pricingTestCaseResponse.setTotalRecord(Long.valueOf(pricingTestCaseResponseWithAirApr.get().size()));
						pricingTestCaseResponseList.add(pricingTestCaseResponse);
					});
				}
			}else {
				throw new THException(HttpStatus.NOT_FOUND,"Test Case not found","Not found");
			}
		}
		return pricingTestCaseResponseList;
	}
	
	/*
	 * Final result based on the ODM interaction
	 */
	public PricingTestCaseResult generateTestCaseResult(Integer testSetId) {
		PricingTestCaseResult pricingTestCaseResult=new PricingTestCaseResult();
		List<PricingTestCaseResponseEntity> pricingTestCaseResponseEntityList=new ArrayList<>();

		if(null!=testSetId) {
			Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseEntityLists=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
			if(pricingTestCaseResponseEntityLists.isPresent()) {
				int testCasePassed=0;
				int testCaseFailed=0;
				for(PricingTestCaseResponseEntity pricingTestCaseResponses : pricingTestCaseResponseEntityLists.get()) {
					PricingTestCaseResponseEntity pricingTestCaseResponseEntity=new PricingTestCaseResponseEntity();
					BeanUtils.copyProperties(pricingTestCaseResponses, pricingTestCaseResponseEntity);
					
					//ODM Interaction Logic for Actual AIR & APR
					
					//Update the actual AIR & APR to DB based on the ODM response
					pricingTestCaseResponseEntity.setActualAir(7.6);
					pricingTestCaseResponseEntity.setActualApr(0.6);
					
					//Test Case passes /Failed logic by comparing expected for actual
					testCasePassed++;
					//Update testTransactionFlag 
					//pricingTestCaseResponseEntity.setTestTransactionFlag(THConstant.TestCase_Processed_Y);
					pricingTestCaseResponseEntityList.add(pricingTestCaseResponseEntity);
					//Saving one by one
					pricingTestCaseResponseRepository.save(pricingTestCaseResponseEntity);
				}
				//Finally update to Transaction table
				Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseWithAirApr=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
				List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
				if(null!=pricingTestCaseResponseWithAirApr && pricingTestCaseResponseWithAirApr.get().size()>0) {
					Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
					pricingTestCaseResponseWithAirApr.get().forEach(pricingTestCaseResults->{
						PricingTestCaseResponse pricingTestCaseResponse=new PricingTestCaseResponse();
						BeanUtils.copyProperties(pricingTestCaseResults, pricingTestCaseResponse);
						pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(pricingTestCaseResults.getApplicationIdentity()));
						pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(pricingTestCaseResults.getBankDivision()));
						pricingTestCaseResponse.setProductName(businessAttributeMap.get(pricingTestCaseResults.getProductName()));
						pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(pricingTestCaseResults.getProductFamily()));
						pricingTestCaseResponseList.add(pricingTestCaseResponse);
					});
				}
				//Setting the final result to ResultModel
				
				pricingTestCaseResult.setTestcasesResultList(pricingTestCaseResponseList);
				pricingTestCaseResult.setPassed(testCasePassed);
				pricingTestCaseResult.setFailed(testCaseFailed);
				pricingTestCaseResult.setTotalTestCases(pricingTestCaseResponseList.size());
			}else {
				throw new THException(HttpStatus.NOT_FOUND,"Test Case not found","Not found");
			}
		}
		return pricingTestCaseResult;
	}
		

	/*
	 * This method used for retrieving pagination Test cases pages by page
	 */
	
	public List<PricingTestCaseResponse> findByPageNo(Integer testSetId,Integer pageNo){
		Pageable pageRequest=PageRequest.of(pageNo-1, THConstant.pageSize, Sort.by("testTransactionId"));
		Page<PricingTestCaseResponseEntity> pageList= pricingTestCaseResponseRepository.findByTestSetId(testSetId,pageRequest);
		List<PricingTestCaseResponse> testCaseList = new ArrayList<>();
		if(pageList!=null && pageList.getSize()>0) {
			Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
			pageList.forEach(page->{
				PricingTestCaseResponse testCase=new PricingTestCaseResponse();
				BeanUtils.copyProperties(page, testCase);
				testCase.setApplicationIdentity(businessAttributeMap.get(page.getApplicationIdentity()));
				testCase.setBankDivision(businessAttributeMap.get(page.getBankDivision()));
				testCase.setProductName(businessAttributeMap.get(page.getProductName()));
				testCase.setProductFamily(businessAttributeMap.get(page.getProductFamily()));
				testCase.setTotalRecord(pageList.getTotalElements());
				testCaseList.add(testCase);
			});
		}
		return testCaseList;
	}
	/*
	 * Generate PDF
	 */
	
	public ByteArrayInputStream generatePDF(Integer testSetId){
		
		Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseEntityList=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
		List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
		if(null!=pricingTestCaseResponseEntityList && pricingTestCaseResponseEntityList.get().size()>0) {
			Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
			pricingTestCaseResponseEntityList.get().forEach(pricingTestCaseResults->{
				PricingTestCaseResponse pricingTestCaseResponse=new PricingTestCaseResponse();
				BeanUtils.copyProperties(pricingTestCaseResults, pricingTestCaseResponse);
				pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(pricingTestCaseResults.getApplicationIdentity()));
				pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(pricingTestCaseResults.getBankDivision()));
				pricingTestCaseResponse.setProductName(businessAttributeMap.get(pricingTestCaseResults.getProductName()));
				pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(pricingTestCaseResults.getProductFamily()));
				pricingTestCaseResponseList.add(pricingTestCaseResponse);
			});
		}		
		return generatePdfReport.testCaseResultReport(pricingTestCaseResponseList);
	}
	
 public ByteArrayInputStream generateExcel(Integer testSetId){
		
		Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseEntityList=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
		List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
		if(null!=pricingTestCaseResponseEntityList && pricingTestCaseResponseEntityList.get().size()>0) {
			Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
			pricingTestCaseResponseEntityList.get().forEach(pricingTestCaseResults->{
				PricingTestCaseResponse pricingTestCaseResponse=new PricingTestCaseResponse();
				BeanUtils.copyProperties(pricingTestCaseResults, pricingTestCaseResponse);
				pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(1));
				pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(2));
				pricingTestCaseResponse.setProductName(businessAttributeMap.get(3));
				pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(4));
				pricingTestCaseResponseList.add(pricingTestCaseResponse);
			});
		}		
		return generateExcelReport.generateExcelReport(pricingTestCaseResponseList);
	}
 
 //Adding Reference Data to LookUp
 public HttpStatus generateReferenceData(MultipartFile uploadfile)  {
	 File convFile = new File( uploadfile.getOriginalFilename());
	 List<PricingLookUpEntity> pricingLookUpEntityList  =new ArrayList<PricingLookUpEntity>();
	 try {
		 uploadfile.transferTo(convFile);
		pricingLookUpEntityList = referenceDataHelper.generateReferenceData(convFile);
	} catch (IllegalStateException |InvalidFormatException | IOException|NumberFormatException e) {
		 throw new THException(HttpStatus.EXPECTATION_FAILED,"Unable to read the uploaded file due to Invalid Input","Exception Occured");
	}
	 if(pricingLookUpEntityList!=null && !pricingLookUpEntityList.isEmpty()) {
		 pricingLookUpRepository.saveAll(pricingLookUpEntityList);
	 }
	 else {
		 throw new THException(HttpStatus.NOT_MODIFIED,"Unable to Save Uploaded Reference Data","Not Modified");
	 }
	 return HttpStatus.OK;
 }
 
 /**
	 * Method to fetch all sets details for the given dates and environment
	 * @param fromDate
	 * @param toDate
	 * @return pricingTestSets
	 */
	public List<PricingTestSet> fetchTestSetDetails (LocalDate fromDate, LocalDate toDate, String environment, int prodName) {		
		List<PricingTestSet> pricingTestSets = new ArrayList<> ();
		//productName,processedFlag
		List<PricingTestSetEntity> pricingTestSetEntities = pricingTestSetRepository.
			findByCreatedTsBetweenAndEnvironmentAndProductNameAndProcessedFlag(
					fromDate.atStartOfDay(),toDate.atStartOfDay(), environment, prodName, THConstant.TestCase_Processed_Y);
		Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
		if (pricingTestSetEntities != null && !pricingTestSetEntities.isEmpty()) {
			
			for (PricingTestSetEntity pricingTestSetEntity : pricingTestSetEntities) {
				
				PricingTestSet pricingTestSet = new PricingTestSet();
				BeanUtils.copyProperties(pricingTestSetEntity, pricingTestSet);
				pricingTestSet.setApplicationIdentity(businessAttributeMap.get(pricingTestSetEntity.getApplicationIdentity()));
				pricingTestSet.setBankDivision(businessAttributeMap.get(pricingTestSetEntity.getBankDivision()));
				pricingTestSet.setProductName(businessAttributeMap.get(pricingTestSetEntity.getProductName()));
				pricingTestSet.setProductFamily(businessAttributeMap.get(pricingTestSetEntity.getProductFamily()));
				pricingTestSets.add(pricingTestSet);
			}		
			
		} else {
			throw new THException(HttpStatus.NOT_FOUND,"Test Sets not found","Not found");
		}
		
		return pricingTestSets;	
		
	}
	
	/**
	 * Method to fetch all test transaction details for the given test set id
	 * @param testSetId
	 * @return
	 */
	public PricingTestCaseResult fetchTestTransactionDetails (int testSetId) {
		List<PricingTestCaseResponse> pricingTestCaseResponses = new ArrayList <>();
		PricingTestCaseResult pricingTestCaseResult=new PricingTestCaseResult();
		Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
		List<PricingTestCaseResponseEntity> pricingTestCaseResponseEntities = pricingTestCaseResponseRepository.findByTestSetId(testSetId);
		int passedCount = 0, failedCount = 0;
		if (pricingTestCaseResponseEntities != null && !pricingTestCaseResponseEntities.isEmpty()){
			
			for (PricingTestCaseResponseEntity pricingTestCaseResponseEntity : pricingTestCaseResponseEntities) {
				
				if (pricingTestCaseResponseEntity.getTestTransactionFlag().equals('Y')) {
					passedCount++;
				} else {
					failedCount++;
				}
				PricingTestCaseResponse pricingTestCaseResponse = new PricingTestCaseResponse();
				BeanUtils.copyProperties(pricingTestCaseResponseEntity, pricingTestCaseResponse);
				pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(pricingTestCaseResponseEntity.getApplicationIdentity()));
				pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(pricingTestCaseResponseEntity.getBankDivision()));
				pricingTestCaseResponse.setProductName(businessAttributeMap.get(pricingTestCaseResponseEntity.getProductName()));
				pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(pricingTestCaseResponseEntity.getProductFamily()));
				pricingTestCaseResponse.setTotalRecord((long)pricingTestCaseResponseEntities.size());
				pricingTestCaseResponses.add(pricingTestCaseResponse);				
			}
			
			pricingTestCaseResult.setTestcasesResultList(pricingTestCaseResponses);
			pricingTestCaseResult.setPassed(passedCount);
			pricingTestCaseResult.setFailed(failedCount);
			pricingTestCaseResult.setTotalTestCases(pricingTestCaseResponses.size());
			
		} else {
			throw new THException(HttpStatus.NOT_FOUND,"Test Transaction not found","Not found");
		}
		return pricingTestCaseResult;
	}
	//Generating Test Scenarios report in Excel
public ByteArrayInputStream generateTestCaseScenarioExcel(Integer testSetId) {
	Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseEntityList=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
	Optional<PricingTestSetEntity>  testSetRepo = pricingTestSetRepository.findById(testSetId);
	List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
	if(null!=pricingTestCaseResponseEntityList && pricingTestCaseResponseEntityList.get().size()>0) {
		Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
		pricingTestCaseResponseEntityList.get().forEach(pricingTestCaseResults->{
			PricingTestCaseResponse pricingTestCaseResponse=new PricingTestCaseResponse();
			BeanUtils.copyProperties(pricingTestCaseResults, pricingTestCaseResponse);
			pricingTestCaseResponse.setEnvironment(testSetRepo.get().getEnvrionment());
			pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(pricingTestCaseResults.getApplicationIdentity()));
			pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(pricingTestCaseResults.getBankDivision()));
			pricingTestCaseResponse.setProductName(businessAttributeMap.get(pricingTestCaseResults.getProductName()));
			pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(pricingTestCaseResults.getProductFamily()));
			pricingTestCaseResponseList.add(pricingTestCaseResponse);
		});
	}		
	return generateTestScenarioExcel.generateExcelReport(pricingTestCaseResponseList);
}

//Adding Selected Test Cases
public PricingTestCaseResult selectedTestCaseExecution(MultipartFile uploadfile)  {
	PricingTestCaseResult pricingTestCaseResult=new PricingTestCaseResult();
	 List<PricingTestCaseResponseEntity> pricingTestCaseResponseEntityList  =new ArrayList<PricingTestCaseResponseEntity>();
	 try {
		
		 pricingTestCaseResponseEntityList = testScenarioHelper.generateSelectedTestCase(uploadfile);
	} catch (IllegalStateException |InvalidFormatException | IOException|NumberFormatException e) {
		 e.printStackTrace();
		 throw new THException(HttpStatus.EXPECTATION_FAILED,"Unable to read the uploaded file due to Invalid Input","Exception Occured");
	}
	 if(pricingTestCaseResponseEntityList!=null && !pricingTestCaseResponseEntityList.isEmpty()) {
			int testSetId=pricingTestCaseResponseEntityList.get(0).getTestSetId();
			long deleteRec=pricingTestCaseResponseRepository.deleteByTestSetId(testSetId);
			List<Integer> borrowingAmt= pricingTestCaseResponseRepository.findDistinctBorrowingAmtByTestSetId(testSetId);
			String borrowingAmount = borrowingAmt.stream().map(String::valueOf).collect(Collectors.joining(","));
		 pricingTestCaseResponseEntityList =  pricingTestCaseResponseRepository.saveAll(pricingTestCaseResponseEntityList);
		 System.out.println("size::"+pricingTestCaseResponseEntityList.size()+"Deleted records::"+deleteRec+"BorrowingAmout::"+borrowingAmount);
		 	int testCasePassed=0;
			int testCaseFailed=0;
			
			pricingTestCaseResponseEntityList.forEach(pricingTestCaseResponses->{
				PricingTestCaseResponseEntity pricingTestCaseResponseEntity=new PricingTestCaseResponseEntity();
				BeanUtils.copyProperties(pricingTestCaseResponses, pricingTestCaseResponseEntity);
				
			
				pricingTestCaseResponseRepository.save(pricingTestCaseResponseEntity);
			});
			//Finally update to Transaction table
			Optional<List<PricingTestCaseResponseEntity>> pricingTestCaseResponseWithAirApr=pricingTestCaseResponseRepository.findByTestSetId(testSetId);
			List<PricingTestCaseResponse> pricingTestCaseResponseList=new ArrayList<>();
			if(null!=pricingTestCaseResponseWithAirApr && pricingTestCaseResponseWithAirApr.get().size()>0) {
				Map<Integer, String> businessAttributeMap = pricingHelper.findBusinessAttributeDescription();
				pricingTestCaseResponseWithAirApr.get().forEach(pricingTestCaseResults->{
					PricingTestCaseResponse pricingTestCaseResponse=new PricingTestCaseResponse();
					BeanUtils.copyProperties(pricingTestCaseResults, pricingTestCaseResponse);
					pricingTestCaseResponse.setApplicationIdentity(businessAttributeMap.get(pricingTestCaseResults.getApplicationIdentity()));
					pricingTestCaseResponse.setBankDivision(businessAttributeMap.get(pricingTestCaseResults.getBankDivision()));
					pricingTestCaseResponse.setProductName(businessAttributeMap.get(pricingTestCaseResults.getProductName()));
					pricingTestCaseResponse.setProductFamily(businessAttributeMap.get(pricingTestCaseResults.getProductFamily()));
					pricingTestCaseResponseList.add(pricingTestCaseResponse);
				});
			}
			//Setting the final result to ResultModel
			
			pricingTestCaseResult.setTestcasesResultList(pricingTestCaseResponseList);
			pricingTestCaseResult.setPassed(testCasePassed);
			pricingTestCaseResult.setFailed(testCaseFailed);
			pricingTestCaseResult.setTotalTestCases(pricingTestCaseResponseList.size());
	 }
	 else {
		 throw new THException(HttpStatus.NOT_MODIFIED,"Unable to Save Uploaded Reference Data","Not Modified");
	 }
	 return pricingTestCaseResult;
}

}
