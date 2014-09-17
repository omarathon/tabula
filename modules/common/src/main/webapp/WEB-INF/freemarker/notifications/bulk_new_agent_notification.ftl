You have now been assigned as ${relationshipType.agentRole} to the following students:

<#list modifiedRelationships as rel>
* ${rel.studentMember.officialName}
</#list>
