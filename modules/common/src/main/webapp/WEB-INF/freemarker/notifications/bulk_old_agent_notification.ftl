You are no longer assigned as ${relationshipType.agentRole} to the following students:

<#list changes as change><#compress>
* ${change.modifiedRelationship.studentMember.officialName} <#compress>
	<#if !(change.modifiedRelationship.endDate?? && change.modifiedRelationship.endDate.beforeNow)>
		(New ${relationshipType.agentRole} ${change.modifiedRelationship.agentName})
	</#if>
</#compress>
</#compress>

</#list>

You can view all of your ${relationshipType.studentRole}s at <@url page='${path}'/>.