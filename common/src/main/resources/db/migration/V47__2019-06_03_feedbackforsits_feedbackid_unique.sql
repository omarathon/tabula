ALTER TABLE feedbackforsits
  ADD CONSTRAINT fk_feedbackforsits_feedback_id UNIQUE (feedback_id);