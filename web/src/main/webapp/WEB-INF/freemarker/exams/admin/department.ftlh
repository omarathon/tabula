<#function route_function dept>
  <#local result><@routes.cm2.departmenthome dept academicYear /></#local>
  <#return result />
</#function>

<@fmt.id7_deptheader "Exams" route_function "for" />


