<#if newAgent?has_content>
	You have been assigned ${newAgent.officialName} as a ${relationshipType.agentRole}.
</#if>

<#if oldAgents?size == 1>
	${oldAgents} is no longer your ${agent_role}.
<#elseif oldAgents?has_content>
	<@fmt.format_list oldAgents> are no longer your ${agent_role}s.
</#if>
