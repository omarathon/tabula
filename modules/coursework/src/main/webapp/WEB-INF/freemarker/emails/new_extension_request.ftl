${student.fullName} has requested an extension for the assignment '${assignment.name}' for ${assignment.module.code?upper_case}, ${assignment.module.name}.

They have requested until ${requestedExpiryDate} to complete the assignment. The following reasons were given for the request:

${reasonForRequest}

Futher details related to this request:

<#if moduleManagers?has_content >
Module Managers
<#list moduleManagers as manager>
${manager.getFullName()} (${manager.getWarwickId()}) (${manager.getEmail()})
</#list>
</#if>

Student Contact Details
Mobile Number: ${(student.mobileNumber)!"Not available"}
Telephone Number: ${(student.phoneNumber)!"Not available"}
Email Address: ${(student.email)!"Not available"}

<#if supervisors?has_content >
Student Supervisor Details
<#list supervisors as supervisor>
${supervisor.agentMember.fullName} (${supervisor.agentMember.universityId}), ${supervisor.agentMember.description}
</#list>
</#if>

${studentCourseString}

To review this extension request and any supporting documentation, please click the link below.

<@url page=path context="/coursework" />