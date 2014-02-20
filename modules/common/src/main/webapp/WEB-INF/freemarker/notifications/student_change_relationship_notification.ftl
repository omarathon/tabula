<#if oldAgent??>
<#if newAgent??>
Your ${relationshipType.agentRole} has been changed from ${oldAgent.officialName} to ${newAgent.officialName}.
<#else>
${oldAgent.officialName} is no longer assigned as your ${relationshipType.agentRole}.
</#if>
<#else>
You have been assigned ${newAgent.officialName} as a ${relationshipType.agentRole}.
</#if>

You can view this information on your student profile page at <@url page='${path}'/>.