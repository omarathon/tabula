-- remove FEEDBACK_ID reference for orphaned markerfeedback
update markerfeedback set FEEDBACK_ID = null where ID in (select mf.id from markerfeedback mf left join feedback f on mf.FEEDBACK_ID = f.ID where f.id is null);
-- set legacy stages according to CM1 workflow position
update markerfeedback set stage = 'legacy-first-marker' where ID in (select mf.id from markerfeedback mf join feedback f on f.FIRST_MARKER_FEEDBACK = mf.id);
update markerfeedback set stage = 'legacy-second-marker' where ID in (select mf.id from markerfeedback mf join feedback f on f.SECOND_MARKER_FEEDBACK = mf.id);
update markerfeedback set stage = 'legacy-third-marker' where ID in (select mf.id from markerfeedback mf join feedback f on f.THIRD_MARKER_FEEDBACK = mf.id);

-- finds any orphaned marker feedback that is a result of rejection on the old CM1 double seen workflow
update markerfeedback set FEEDBACK_ID = null where ID in (
  SELECT mf.id
  FROM markerfeedback mf
    JOIN feedback f ON f.id = mf.feedback_id
    LEFT JOIN markerfeedback mf1 ON f.FIRST_MARKER_FEEDBACK = mf1.id AND mf1.id = mf.id
    LEFT JOIN markerfeedback mf2 ON f.SECOND_MARKER_FEEDBACK = mf2.id AND mf1.id = mf.id
    LEFT JOIN markerfeedback mf3 ON f.THIRD_MARKER_FEEDBACK = mf3.id AND mf1.id = mf.id
  WHERE mf.stage IS NULL AND mf1.id IS NULL AND mf2.id IS NULL AND mf3.id IS NULL
);

-- constraint to prevent duplicate marking workflow stages per student and marker
ALTER TABLE markerfeedback ADD CONSTRAINT mf_stage_unique UNIQUE (feedback_id, marker, stage);