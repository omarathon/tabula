-- HFC-127 support multiple file attachments on submissions

ALTER TABLE ASSIGNMENT 
ADD (fileAttachmentLimit NUMBER(1, 0) DEFAULT 1 );

ALTER TABLE ASSIGNMENT 
ADD (fileAttachmentExtensions NVARCHAR2(4000) );