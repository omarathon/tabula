<#if newAgent??>
Your ${relationshipType.studentRole} ${student.officialName} has now been reassigned to ${newAgent.officialName}.
<#else>
You are no longer assigned as ${relationshipType.agentRole} to ${student.officialName}.
</#if>
The student profile for ${student.officialName} can be found at <@url page='${path}'/>.