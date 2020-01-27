${submissions?size} submissions for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name} have been marked as plagiarised.

<#list submissions as submission>
- ${submission.studentIdentifier}
</#list>
