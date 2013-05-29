<#escape x as x?html>

<h1>Create small groups for <@fmt.module_name module /></h1>

<@f.form method="post" action="${url('/admin/module/${module.code}/groups/${smallGroupSet.id}/edit')}" commandName="editSmallGroupSetCommand" cssClass="form-horizontal">

	<@f.errors cssClass="error form-errors" />
	
	<#assign newRecord=false />
	<#include "_fields.ftl" />
	
	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@routes.depthome module=module />">Cancel</a>
	</div>

</@f.form>

</#escape>