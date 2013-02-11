Hello ${user.firstName}
	          
You recently asked to have the submissions for the assignment ${assignmentTitle} checked for plagiarism. The check has now completed and is ready to be viewed by you or anyone else with permission to view the assignment's submissions.

<#if failureCount gt 1>
	${failureCount} submissions could not be checked.
<#elseif feedbackcount gt 0>
	${failureCount} submission could not be checked.
</#if>

You may see the results on the list of submissions page, which you can find here:

<@url page=path context="/coursework" />


This email was sent from an automated system, and replies to it will not reach a real person.