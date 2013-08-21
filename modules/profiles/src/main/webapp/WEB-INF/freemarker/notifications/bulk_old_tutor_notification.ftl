You are no longer assigned as personal tutor to the following students:

<#list changes as change><#compress>
* ${change.modifiedRelationship.studentMember.officialName} <#compress>
	<#if !(change.modifiedRelationship.endDate?? && change.modifiedRelationship.endDate.beforeNow)>
		(New tutor ${change.modifiedRelationship.agentName})
	</#if>
</#compress>
</#compress>

</#list>

You can view all of your tutees at <@url page='${path}'/>.