You have now been assigned as personal tutor to the following students:

<#list changes as change><#compress>
* ${change.modifiedRelationship.studentMember.officialName} <#compress>
	<#if change.oldTutor??>
		(Previous tutor ${change.oldTutor.officialName})
	</#if>
</#compress>
</#compress>

</#list>

You can view all of your tutees at <@url page='${path}'/>.