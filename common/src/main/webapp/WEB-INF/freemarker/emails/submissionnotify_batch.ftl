${submissions?size} submissions for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name} have been received.

<#list submissions as submission>
- ${submission.submission.studentIdentifier}
    - Submission date: ${submission.submissionDate}
    - Submission ID: ${submission.submission.id}
    - Uploaded attachments:
<#list submission.submission.allAttachments as attachment>
        - ${attachment.name}
</#list>
<#if submission.feedbackDeadlineDate??>
    - Student feedback deadline: ${submission.feedbackDeadlineDate}
</#if>
</#list>

<#-- Only show these links if it's an email. -->
<#if isEmail!false>
To unsubscribe from these messages, go here and select "No alerts".

<@url context='/' page='/settings'/>
</#if>
