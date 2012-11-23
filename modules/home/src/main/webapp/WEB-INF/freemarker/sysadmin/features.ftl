<#escape x as x?html>

<style>
.feature-flags th { text-align: right; }
.feature-flags td, .feature-flags th {
  padding: 1em 0.5em;
}
</style>

<h1>Feature flags</h1>

<p>
If you're on a multi-instance system, this controller will only
update one instance at a time. Updating a flag on only one instance could
lead to confusing and/or undefined behaviour. 
</p>

<table class="feature-flags">
<#list currentValues as feature>
<tr>
<th>${feature.name}</th>
<td>${feature.value?string}</td>
<td>
<form action="<@url page="/sysadmin/features"/>" method="POST">
<input type="hidden" name="name" value="${feature.name}">
Set to 
<input type="submit" name="value" value="false"> or 
<input type="submit" name="value" value="true">
</form>
</td>
</tr>
</#list>
</table>

</#escape>