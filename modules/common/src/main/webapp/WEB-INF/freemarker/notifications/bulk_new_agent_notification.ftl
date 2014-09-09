<!-- don't give old agents as each student will have a different set -->
You have now been assigned as ${relationshipType.agentRole} to the following students:
<#list modifiedRelationships as rel>
* ${rel.studentMember.officialName} <#compress>
</#compress>
</#list>
