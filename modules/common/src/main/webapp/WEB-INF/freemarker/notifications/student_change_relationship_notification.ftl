<#compress>
<#if newAgents?has_content><#--
-->		You have been assigned <@fmt.format_list_of_members newAgents/> as ${relationshipType.agentRole}<#--
--></#if><#--
--><#if oldAgents?size == 1><@fmt.format_list_of_members oldAgents/> is no longer your ${relationshipType.agentRole}.<#--
--><#elseif oldAgents?has_content><@fmt.format_list_of_members oldAgents/> are no longer your ${relationshipType.agentRole}s.<#--
--></#if><#--
--></#compress>