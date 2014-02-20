You have now been assigned as ${relationshipType.agentRole} to the following students:

<#list changes as change><#compress>
* ${change.modifiedRelationship.studentMember.officialName} <#compress>
	<#if change.oldAgent??>
		(Previous ${relationshipType.agentRole} ${change.oldAgent.officialName})
	</#if>
</#compress>
</#compress>

</#list>

You can view all of your ${relationshipType.studentRole}s at <@url page='${path}'/>.