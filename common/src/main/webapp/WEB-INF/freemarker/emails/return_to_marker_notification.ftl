<@fmt.p number=numReleasedFeedbacks singular="submission" /> for ${assignment.name} - ${assignment.module.code?upper_case} <@fmt.p number=numReleasedFeedbacks singular="has" plural="have" shownumber=false /> been returned to you.<#--
--><#if comment?has_content>


The following reason was given: ${comment}</#if><#--
--><#if feedbackDeadlineDate??>


Student feedback is due on ${feedbackDeadlineDate}.</#if>
