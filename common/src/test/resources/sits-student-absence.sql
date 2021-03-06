-- let's make a fake SITS

DROP TABLE SRS_SAB IF EXISTS;

CREATE TABLE IF NOT EXISTS SRS_SAB
(
  SAB_STUC  VARCHAR(12) NOT NULL,
  SAB_SEQ2  VARCHAR(3)  NOT NULL,
  SAB_SRTN  VARCHAR(15),
  SAB_RAAC  VARCHAR(6),
  SAB_BEGD  DATE,
  SAB_EXRD  DATE,
  SAB_ENDD  DATE,
  SAB_PDAR  VARCHAR(1),
  SAB_AYRC  VARCHAR(12),
  SAB_UDF1  VARCHAR(15),
  SAB_UDF2  VARCHAR(15),
  SAB_UDF3  VARCHAR(15),
  SAB_UDF4  VARCHAR(15),
  SAB_UDF5  VARCHAR(15),
  SAB_UDF6  VARCHAR(15),
  SAB_UDF7  VARCHAR(15),
  SAB_UDF8  VARCHAR(15),
  SAB_UDF9  VARCHAR(15),
  SAB_UDFA  VARCHAR(15),
  SAB_UDFB  VARCHAR(15),
  SAB_UDFC  VARCHAR(15),
  SAB_UDFD  VARCHAR(15),
  SAB_UDFE  VARCHAR(15),
  SAB_UDFF  VARCHAR(15),
  SAB_UDFG  VARCHAR(15),
  SAB_UDFH  VARCHAR(15),
  SAB_UDFI  VARCHAR(15),
  SAB_UDFJ  VARCHAR(100),
  SAB_UDFK  VARCHAR(100),
  SAB_NOTE  VARCHAR(2000),
  CONSTRAINT SRS_SABP1 PRIMARY KEY (SAB_STUC, SAB_SEQ2)
);
