ALTER TABLE Department
ADD (
	allowExtensionRequests number(1,0),
	extensionGuidelineSummary nvarchar2(4000),
	extensionGuidelineLink nvarchar2(255)
);