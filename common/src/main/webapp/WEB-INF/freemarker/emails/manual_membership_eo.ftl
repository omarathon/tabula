The following departments have manually added students to assignments and/or small group sets in Tabula:

<#list departments as department>
 * ${department.name} - (<@fmt.p mapGet(numAssignments, department.code) "assignment"/> and <@fmt.p mapGet(numSmallGroupSets, department.code) "small group set"/>)
</#list>