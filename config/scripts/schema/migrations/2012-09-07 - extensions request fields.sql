-- HFC-262
ALTER TABLE Extension
ADD (
	requestedExpiryDate timestamp,
	requestedOn timestamp,
	rejected NUMBER(1,0) DEFAULT 0
);