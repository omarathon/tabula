You recently asked to have the submissions for the assignment ${assignmentTitle} checked for plagiarism. The check has now completed and is ready to be viewed by you or anyone else with permission to view the assignment's submissions.

<#if failureCount gt 0>
<@fmt.p number=failureCount singular="submission"/> could not be checked:

<#list failedUploads?keys as key>${key} --  ${failedUploads[key]}</#list>
</#if>
