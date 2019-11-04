alter table originalityreport
drop column matchPercentage, -- just re-use the original overlap
drop column similarity, -- can calculate this on the fly
drop column nextSubmitAttempt, -- legacy Urkund field
drop column submitAttempts, -- legacy Urkund field
drop column submittedDate, -- legacy Urkund field
drop column nextResponseAttempt, -- legacy Urkund field
drop column responseAttempts, -- legacy Urkund field
drop column responseReceived, -- legacy Urkund field
drop column reportUrl, -- legacy Urkund field
drop column significance, -- legacy Urkund field
drop column matchCount, -- legacy Urkund field
drop column sourceCount, -- legacy Urkund field
drop column urkundResponse, -- legacy Urkund field
drop column urkundResponseCode; -- legacy Urkund field