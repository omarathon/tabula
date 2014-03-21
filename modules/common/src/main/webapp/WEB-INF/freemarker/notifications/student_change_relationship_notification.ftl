<#if oldAgent?? && newAgent??>
Your ${relationshipType.agentRole} has been changed from ${oldAgent.officialName} to ${newAgent.officialName}.
<#elseif oldAgent??>
${oldAgent.officialName} is no longer assigned as your ${relationshipType.agentRole}.
<#elseif newAgent??>
You have been assigned ${newAgent.officialName} as a ${relationshipType.agentRole}.
</#if>

You can view this information on your student profile page at <@url page='${path}'/>.