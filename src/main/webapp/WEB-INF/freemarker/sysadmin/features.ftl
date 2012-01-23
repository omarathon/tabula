<#escape x as x?html>

<table>
<#list currentFeatures as feature>
<tr>
<th>${feature.name}</th>
<td>${feature.value}</td>
</tr>
</#list>
</table>

</#escape>