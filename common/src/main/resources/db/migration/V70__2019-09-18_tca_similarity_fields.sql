alter table originalityreport
    add column matchPercentage smallint,
    add column similarityRequestedOn timestamp,
    add column similarityLastGenerated timestamp;



