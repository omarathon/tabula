The following departments have manually added students to assignments and/or small group sets in Tabula:

<#list departments as department>
 * ${department.name} - <@url context='/admin' page='/department/${department.code}/manualmembership/eo'/>
</#list>