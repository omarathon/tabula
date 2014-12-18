-- TAB-2513
ALTER TABLE feedback
ADD (
	adjustedMark number(5, 0),
	adjustedGrade nvarchar2(255),
	adjustmentReason nvarchar2(600),
	adjustmentComments nclob
);