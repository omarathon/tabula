You are no longer assigned as ${relationshipType.agentRole} to the following students:

<#list modifiedRelationships as rel>
* ${rel.studentMember.officialName}
	<#if !(rel.endDate?? && rel.endDate.beforeNow)>
		(New ${relationshipType.agentRole} ${rel.agentName})
	</#if>
</#list>