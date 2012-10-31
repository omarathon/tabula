<h1>Mark schemes</h1>

<p>Mark schemes can be created here and then used by one or more assignments to define how marking is done for that assignment.</p>

<#if !markSchemes?has_content>
<p>
No mark schemes have been created yet. Click <strong>Create</strong> below to make one.
</p>
</#if>

<p><a class="btn" href="<@routes.markschemeadd department=command.department />"><i class="icon-plus"></i> Create</a></p>

<#if markSchemes?has_content>
<table class="table table-bordered table-striped">
<tr>
	<th>Mark scheme name</th>
	<th></th>
</tr>
<#list markSchemes as markScheme>
<tr>
	<td>${markScheme.name}</td><td><a href="<@routes.markschemeedit markScheme />">Edit</a></td>
</tr>
</#list>
</table>
</#if>