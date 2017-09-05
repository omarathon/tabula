The following departments have manually added students in Tabula assignments and/or small group sets.

<#list departments as department>
 * ${department.name} - (<@fmt.p mapGet(numAssignments, department.code) "assignment"/> and <@fmt.p mapGet(numSmallGroupSets, department.code) "small group set"/>)
</#list>