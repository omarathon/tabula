This message confirms that a submission for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name} has been received.

This message is sent to you as a manager of the module.

Submission date: ${submissionDate}
Submission ID: ${submission.id}

<#if submission.allAttachments??>
Uploaded attachments:		
<#list submission.allAttachments as attachment>
	${attachment.name}
</#list>  
</#if>

You can download all the attachments for this submission here:

<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/submission-${submission.universityId}.zip'/>


All submissions for this assignment can be found here:

<@url page='/admin/module/${module.code}/assignments/${assignment.id}/list'/>



To unsubscribe from these messages, go here and select "No alerts".

<@url page='/admin/usersettings#submission-alerts'/>