alter table originalityreport
    drop column matchPercentage, -- just re-use the original overlap for this
    drop column similarity; -- can calculate this on the fly




