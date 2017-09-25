The following departments have manually added students to assignments and/or small group sets in Tabula:

<#list departments as department>
 * ${department.name} - <@routes.admin.manualmembershipeo department />
</#list>