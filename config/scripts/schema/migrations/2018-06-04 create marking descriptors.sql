CREATE TABLE MARKINGDESCRIPTOR (
  ID            NVARCHAR2(255) NOT NULL,
  DISCRIMINATOR CHAR(1)        NOT NULL,
  DEPARTMENT_ID NVARCHAR2(255),
  MIN_MARK      INT            NOT NULL,
  MAX_MARK      INT            NOT NULL,
  TEXT          NCLOB          NOT NULL,
  CONSTRAINT MARKINGDESCRIPTOR_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_MARKINGDESCRIPTOR_DEPARTMENT
  ON MARKINGDESCRIPTOR (DEPARTMENT_ID);

INSERT INTO MARKINGDESCRIPTOR
VALUES ('2cb3c982-67eb-11e8-850c-1a000260cfe0', 'U', NULL, 0, 0, 'Work of no merit OR Absent, work not submitted, penalty in some misconduct cases');
INSERT INTO MARKINGDESCRIPTOR VALUES ('4df670f4-67eb-11e8-b6a4-1a000260cfe0', 'U', NULL, 12, 25,
                                      'Poor quality work well below the standards required for the appropriate stage of an Honours degree.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('58112390-67eb-11e8-9bce-1a000260cfe0', 'U', NULL, 32, 32,
                                      'Work is significantly below the standard required for the appropriate stage of an Honours degree. Some evidence of study and some knowledge and evidence of understanding but subject to very serious omissions and errors.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('624288fe-67eb-11e8-93ca-1a000260cfe0', 'U', NULL, 38, 38,
                                      'Work does not meet standards required for the appropriate stage of an Honours degree. Evidence of study and demonstrates some knowledge and some basic understanding of relevant concepts and techniques, but subject to significant omissions and errors.');
INSERT INTO MARKINGDESCRIPTOR
VALUES ('6cf6047e-67eb-11e8-abc6-1a000260cfe0', 'U', NULL, 42, 48, 'Work of limited quality, demonstrating some relevant knowledge and understanding.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('77f9f380-67eb-11e8-bd5e-1a000260cfe0', 'U', NULL, 52, 58,
                                      'Competent work, demonstrating reasonable knowledge and understanding, some analysis, organisation, accuracy, relevance, presentation and appropriate skills.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('a7c80232-67eb-11e8-bd29-1a000260cfe0', 'U', NULL, 62, 68,
                                      'High quality work demonstrating good knowledge and understanding, analysis, organisation, accuracy, relevance, presentation and appropriate skills.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('a941465a-67eb-11e8-95a5-1a000260cfe0', 'U', NULL, 74, 88,
                                      'Very high quality work demonstrating excellent knowledge and understanding, analysis, organisation, accuracy, relevance, presentation and appropriate skills. Work which may extend existing debates or interpretations.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('aa93a07a-67eb-11e8-b819-1a000260cfe0', 'U', NULL, 94, 94,
                                      'Exceptional work of the highest quality, demonstrating excellent knowledge and understanding, analysis, organisation, accuracy, relevance, presentation and appropriate skills. At final year level: work may achieve or be close to publishable standard.');
INSERT INTO MARKINGDESCRIPTOR VALUES ('abdd35cc-67eb-11e8-97e0-1a000260cfe0', 'U', NULL, 100, 100,
                                      'Work of original and exceptional quality which in the examiners’ judgement merits special recognition by the award of the highest possible mark.');
