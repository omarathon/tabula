You recently asked to have the submissions for the assignment ${assignmentTitle} checked for plagiarism. The check has now completed and is ready to be viewed by you or anyone else with permission to view the assignment's submissions.

<#if failureCount gt 0>
<#if failureCount gt 1>
${failureCount} submissions could not be checked:
<#elseif failureCount gt 0>
${failureCount} submission could not be checked:
</#if>

<#list failedUploads?keys as key>
${key} --  ${failedUploads[key]}
</#list>
</#if>
You may see the results on the list of submissions page, which you can find here:

<@url page=path context="/coursework" />