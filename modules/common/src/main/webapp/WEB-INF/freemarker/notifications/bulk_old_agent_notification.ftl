You are no longer assigned as ${relationshipType.agentRole} to the following students:

<#list modifiedRelationships as rel>
* ${rel.studentMember.officialName}<#compress>
<#if !(rel.endDate?? && rel.endDate.beforeNow)>
 (new ${relationshipType.agentRole} ${rel.agentName})
</#compress></#if>
</#list>
