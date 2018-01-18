<#if failureCount gt 0>
You recently sent <@fmt.p totalCount "submission" /> for the assignment '${assignmentTitle}' to Turnitin for a plagiarism check.
Turnitin <#if successCount gt 0>checked <@fmt.p successCount "submission" /> successfully but </#if>could not check the following <#if failureCount gt 1><@fmt.p failureCount "submission" /><#else>submission</#if>:

<#list failedReports as report>
- ${report.attachment.submissionValue.submission.studentIdentifier} - ${report.attachment.name} - ${report.lastTurnitinError}
</#list>

<#if successCount gt 0><#if successCount == 1>A plagiarism report for the submission is now ready.<#else>Plagiarism reports for <@fmt.p successCount "submission" /> are now ready.</#if>
Anyone with permission to view the assignment submissions can view <@fmt.p number=successCount singular="the report" shownumber=false />.</#if>
<#else>
Plagiarism reports are ready for the <@fmt.p totalCount "submission" /> for the assignment '${assignmentTitle}'
you recently sent to Turnitin. Anyone with permission to view the assignment submissions can view the reports.
</#if>