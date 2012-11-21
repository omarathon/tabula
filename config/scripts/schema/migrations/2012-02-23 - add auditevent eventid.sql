-- ID that is shared by multiple auditevent rows when they relate
-- to different stages of the same event.

ALTER TABLE AUDITEVENT 
ADD (EVENTID NCHAR(36) );

CREATE INDEX "AUDITEVENT_EVENTID_IDX" ON AUDITEVENT(EVENTID);
