-- remove FEEDBACK_ID reference for orphened markerfeedback
update markerfeedback set FEEDBACK_ID = null where ID in (select mf.id from markerfeedback mf left join feedback f on mf.FEEDBACK_ID = f.ID where f.id is null);
-- set legacy stages according to CM1 workflow position
update markerfeedback set stage = 'legacy-first-marker' where ID in (select mf.id from markerfeedback mf join feedback f on f.FIRST_MARKER_FEEDBACK = mf.id);
update markerfeedback set stage = 'legacy-second-marker' where ID in (select mf.id from markerfeedback mf join feedback f on f.SECOND_MARKER_FEEDBACK = mf.id);
update markerfeedback set stage = 'legacy-third-marker' where ID in (select mf.id from markerfeedback mf join feedback f on f.THIRD_MARKER_FEEDBACK = mf.id);
-- constraint to prevent duplicate marking workflow stages per student and marker
ALTER TABLE markerfeedback ADD CONSTRAINT mf_stage_unique UNIQUE (feedback_id, marker, stage);