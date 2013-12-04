${moderatorName} has rejected your feedback for student ${studentId} on ${assignment.name}.

The moderator left the following comments ...

${rejectionComments}
<#if adjustedMark??>Adjusted mark: ${adjustedMark}</#if>
<#if adjustedGrade??>Adjusted grade: ${adjustedGrade}</#if>

Please visit <@url page=markingUrl /> to update the feedback and submit it for moderation again.