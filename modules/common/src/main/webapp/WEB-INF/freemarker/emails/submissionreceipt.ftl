This email confirms that you made a submission for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name}.

Submission date: ${submissionDate}
Submission ID: ${submission.id}
University ID: ${user.warwickId}
<#if submission.allAttachments??>
Uploaded attachments:		
<#list submission.allAttachments as attachment>
	${attachment.name}
</#list>  
</#if>

To review your submission, please visit:

<@url page=path context="/coursework" />