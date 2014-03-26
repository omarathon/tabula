${moderatorName} has rejected your feedback for student ${studentId} on ${assignment.name} for ${module.code?upper_case} ${module.name}.

The moderator left the following comments ...

${rejectionComments}
<#if adjustedMark??>Adjusted mark: ${adjustedMark}</#if>
<#if adjustedGrade??>Adjusted grade: ${adjustedGrade}</#if>