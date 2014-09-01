You have now been assigned as ${relationshipType.agentRole} to the following students:
<#list changes as change>

* ${change.modifiedRelationship.studentMember.officialName} <#compress>
	<#if change.oldAgent??>
		(Previous ${relationshipType.agentRole} ${change.oldAgent.officialName})
	</#if>
</#compress>
</#list>