-- HFC-260
ALTER TABLE ASSIGNMENT
ADD (
	allowExtensions NUMBER(1,0) DEFAULT 0,
	allowExtensionRequests NUMBER(1,0) DEFAULT 0
);