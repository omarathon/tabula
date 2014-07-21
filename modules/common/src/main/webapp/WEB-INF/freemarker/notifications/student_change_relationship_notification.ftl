<#if oldAgent?? && newAgent??>
	<#if oldAgent.universityId == newAgent.universityId>
		You have been re-assigned ${newAgent.officialName} as a ${relationshipType.agentRole}.
	<#else>
		Your ${relationshipType.agentRole} has been changed from ${oldAgent.officialName} to ${newAgent.officialName}.
	</#if>
<#elseif oldAgent??>
${oldAgent.officialName} is no longer assigned as your ${relationshipType.agentRole}.
<#elseif newAgent??>
You have been assigned ${newAgent.officialName} as a ${relationshipType.agentRole}.
</#if>
