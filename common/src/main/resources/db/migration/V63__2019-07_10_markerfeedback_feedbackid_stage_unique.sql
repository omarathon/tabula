  ALTER TABLE markerfeedback
  ADD CONSTRAINT idx_markerfeedback_feedback_id_stage UNIQUE (feedback_id, stage);
