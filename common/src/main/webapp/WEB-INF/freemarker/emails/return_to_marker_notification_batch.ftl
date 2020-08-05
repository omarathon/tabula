<#list notifications as notification>
<@fmt.p number=notification['numReleasedFeedbacks'] singular="submission" /> for ${notification['assignment'].name} - ${notification['assignment'].module.code?upper_case} <@fmt.p number=notification['numReleasedFeedbacks'] singular="has" plural="have" shownumber=false /> been returned to you.<#--
--><#if notification['comments']?has_content>


The following <@fmt.p number=notification['comments']?size singular="reason was" plural="reasons were" shownumber=false /> given:

<#list notification['comments'] as comment>
- ${comment}
</#list></#if><#--
--><#if notification['feedbackDeadlineDate']??>

Student feedback is due on ${notification['feedbackDeadlineDate']}.</#if><#--
--><#if notification_has_next>


---

</#if></#list>
