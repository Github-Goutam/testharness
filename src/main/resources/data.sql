DROP TABLE IF EXISTS THS_PRC_ATTRIBUTE_REF;
DROP TABLE IF EXISTS THS_PRC_TESTSET_TB;
DROP TABLE IF EXISTS THS_PRC_TEST_TXN_TB;
DROP TABLE IF EXISTS THS_PRC_LOOKUP_REF;

CREATE TABLE THS_PRC_ATTRIBUTE_REF (

  THS_PRC_ATTRIBUTE_ID 		INTEGER  PRIMARY KEY,
  REF_DATA_KEY    VARCHAR(2),
  DESCRIPTION     VARCHAR(250),
  IS_ACTIVE       VARCHAR(1),
  CREATED_BY      VARCHAR(50),
  CREATED_TS      DATE,
  LAST_UPD_BY     VARCHAR(50),
  LAST_UPD_TS     DATE
  
);

CREATE TABLE THS_PRC_TESTSET_TB (

  TEST_SET_ID 		INTEGER  PRIMARY KEY,
  APPL_IDENTITY     VARCHAR(25),
  BANK_DIVISION     VARCHAR(25),
  PROD_FAMILY       VARCHAR(25),
  PROD_NAME    		VARCHAR(25),
  RISK_BAND     	VARCHAR(25),
  TERM       		VARCHAR(25),
  BORROWING_AMT     VARCHAR(25),
  PROCESSED_FLAG    VARCHAR(25),
  CREATED_BY        VARCHAR(50),
  CREATED_TS        DATE,
  LAST_UPD_BY       VARCHAR(50),
  LAST_UPD_TS       DATE
  
);


CREATE TABLE THS_PRC_TEST_TXN_TB (

  TEST_TXN_ID 		INTEGER  PRIMARY KEY,
  TEST_SET_ID 		INTEGER,
  TEST_TXN_NO 		VARCHAR(25),
  APPL_IDENTITY     VARCHAR(25),
  BANK_DIVISION     VARCHAR(25),
  PROD_FAMILY       VARCHAR(25),
  PROD_NAME    		VARCHAR(25),
  RISK_BAND     	VARCHAR(25),
  TERM       		VARCHAR(25),
  BORROWING_AMT     VARCHAR(25),
  TEST_TXN_FLAG     VARCHAR(25),
  ACT_AIR 			INTEGER,
  ACT_APR 			INTEGER,
  EXCPT_AIR 		INTEGER,
  EXCPT_APR 		INTEGER,
  XML_DIFF  		VARCHAR(500),
  CREATED_BY        VARCHAR(50),
  CREATED_TS        DATE,
  LAST_UPD_BY       VARCHAR(50),
  LAST_UPD_TS       DATE
  
);

CREATE TABLE THS_PRC_LOOKUP_REF (

  THS_PRC_LOOKUP_ID 		INTEGER  PRIMARY KEY,
  RISK_BAND     	INTEGER,
  TERM       		VARCHAR(25),
  BORROWING_AMT     VARCHAR(25),
  AIR_RATE			INTEGER,
  APR_RATE			INTEGER,
  CREATED_BY        VARCHAR(50),
  CREATED_TS        DATE,
  LAST_UPD_BY       VARCHAR(50),
  LAST_UPD_TS       DATE
  
);
insert into THS_PRC_LOOKUP_REF values(1,5,'>=12<=35','>=1000<5000',6,0,'','2008-11-11','','2008-11-11');
insert into THS_PRC_LOOKUP_REF values(2,5,'>=12<=35','>=5000<10000',7,0,'','2008-11-11','','2008-11-11');
insert into THS_PRC_LOOKUP_REF values(3,5,'>=12<=35','>=10000<15000',8,0,'','2008-11-11','','2008-11-11');