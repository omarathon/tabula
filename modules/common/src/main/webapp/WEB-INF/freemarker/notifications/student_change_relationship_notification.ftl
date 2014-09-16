<#compress>
<#if newAgent?has_content>
You have been assigned ${newAgent.officialName} as a ${relationshipType.agentRole}.  </#if><#if oldAgents?size == 1><@fmt.format_list_of_members oldAgents/> is no longer your ${relationshipType.agentRole}.<#elseif oldAgents?has_content><@fmt.format_list_of_members oldAgents/> are no longer your ${relationshipType.agentRole}s.
</#if>
</#compress>