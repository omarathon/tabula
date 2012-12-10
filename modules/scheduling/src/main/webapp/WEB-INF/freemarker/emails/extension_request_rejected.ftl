Dear ${user.firstName}

Your extension request has been rejected for the assignment '${assignment.name}' for ${module.code?upper_case}, ${module.name}.
The deadline for the assignment is ${originalAssignmentDate}. Any submissions made after the deadline will be
subject to the usual late penalties.

<#if extension.approvalComments?has_content>
The administrator left the following comments:

${extension.approvalComments}

</#if>
The assignment deadline is displayed at the top of the submission page:

<@url page=path context="/coursework" />


This email was sent from an automated system, and replies to it will not reach a real person.