ALTER TABLE FileAttachment
ADD (
	extension_id nvarchar2(255)
);

create index fileattachment_extension on fileattachment(extension_id);