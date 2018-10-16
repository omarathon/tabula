<#if scheduledDate?has_content>
	<#assign formattedDate><@fmt.date date=scheduledDate stripHtml=true /></#assign>
	<#assign formattedDate =  formattedDate?replace('&#8194;',' ') />
</#if>
<@compress single_line=true>
	<#if scheduledDate?has_content>
		As of ${formattedDate} you will be assigned as ${relationshipType.agentRole} to ${student.fullName}.
		<#if oldAgents?has_content>The current ${relationshipType.agentRole} is ${oldAgents?first.fullName}.</#if>
	<#else>
		You have now been assigned as ${relationshipType.agentRole} to ${student.fullName}.
		<#if oldAgents?has_content>The previous ${relationshipType.agentRole} was ${oldAgents?first.fullName}.</#if>
	</#if>
</@compress>