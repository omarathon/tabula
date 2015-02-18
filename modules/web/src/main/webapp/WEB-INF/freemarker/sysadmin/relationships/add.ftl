<#escape x as x?html>

<h1>Add a new student relationship type</h1>

<@f.form method="post" action="${url('/sysadmin/relationships/add')}" commandName="addStudentRelationshipTypeCommand" cssClass="form-horizontal">

	<@f.errors cssClass="error form-errors" />
	
	<#assign newRecord=true />
	<#include "_fields.ftl" />
	
	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/relationships" />">Cancel</a>
	</div>

</@f.form>


</#escape>