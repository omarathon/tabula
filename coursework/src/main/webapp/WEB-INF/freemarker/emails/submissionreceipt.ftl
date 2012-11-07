Hello ${user.firstName}
	          
This email confirms that you made a submission for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name}.

Submission date: ${submissionDate}
Submission ID: ${submission.id}
<#if submission.allAttachments??>
Uploaded attachments:		
<#list submission.allAttachments as attachment>
	${attachment.name}
</#list>  
</#if>

To review your submission, please visit:

${url}

This email was sent from an automated system, and replies to it will not reach a real person.