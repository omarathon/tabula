You are no longer assigned as ${relationshipType.agentRole} to the following students:

<#list modifiedRelationships as rel><#compress>
* ${rel.studentMember.officialName} <#compress>
	<#if !(rel.endDate?? && change.rel.endDate.beforeNow)>
		(New ${relationshipType.agentRole} ${change.modifiedRelationship.agentName})
	</#if>
</#compress>
</#compress>
</#list>