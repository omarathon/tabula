CREATE TABLE marker_usergroup (
	assignment_id nvarchar2(255) not null,
	markermap_id nvarchar2(255),
	marker_uni_id nvarchar2(255)
);

ALTER TABLE markerfeedback ADD grade NVARCHAR2(255);

ALTER TABLE markscheme ADD markingMethod NVARCHAR2(255);
UPDATE markscheme SET markingMethod = 'StudentsChooseMarker';
commit;

