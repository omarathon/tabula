Outcomes last recorded<#if submission.outcomesLastRecordedBy??> by ${submission.outcomesLastRecordedBy.fullName!submission.outcomesLastRecordedBy.userId}</#if><#if outcomesLastRecordedOn??> at ${outcomesLastRecordedOn}</#if>:

MIT-${submission.key}: ${startDate} - <#if endDate??>${endDate}<#else>(ongoing)</#if><#--

--><#if ((submission.outcomeGrading.entryName)!"") != "Rejected"><#if submission.acuteOutcome??>


**${submission.acuteOutcome.description}**</#if><#--

--><#if submission.affectedAssessments?has_content>


Affected assessments:
<#list submission.affectedAssessments as assessment><#--
--><#if ((assessment.acuteOutcome.entryName)!"") == ((submission.acuteOutcome.entryName)!"")>
- ${assessment.module.code?upper_case} ${assessment.module.name} (${assessment.academicYear.toString}) - ${assessment.name}
</#if><#--
--></#list></#if><#else>


The submission was rejected - <#list submission.rejectionReasons as rejectionReason><#if rejectionReason.entryName == "Other">${submission.rejectionReasonsOther}<#else>${rejectionReason.description}</#if><#if rejectionReason_has_next>, </#if></#list>.
</#if>