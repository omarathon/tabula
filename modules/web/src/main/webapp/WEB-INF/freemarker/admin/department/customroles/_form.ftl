<#--

Adding or editing a custom role definition

-->
<#if view_type="add">
	<#assign submit_text="Create" />
<#elseif view_type="edit">
	<#assign submit_text="Save" />
</#if>

<#escape x as x?html>
<#compress>

<h1><#if view_type = "add">Create<#else>Edit</#if> custom role definition</h1>
<#assign commandName="command" />

<@f.form method="post" action="${form_url}" commandName=commandName cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

<#--
Common form fields.
-->
<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="text" />
	<div class="help-block">
		A descriptive name that will be used to refer to this role.
	</div>
</@form.labelled_row>

<@form.labelled_row "baseDefinition" "Base role definition">
	<@f.select path="baseDefinition">
		<#list allRoleDefinitions as roleDefinition>
			<#assign roleDefinitionName><#compress>
				<#if roleDefinition.id??>
				${roleDefinition.id}
				<#elseif roleDefinition.selector??>
				${roleDefinition.name}(${roleDefinition.selector.id})
				<#else>
				${roleDefinition.name}
				</#if>
			</#compress></#assign>
			<option value="${roleDefinitionName}"<#if status.value! == roleDefinitionName> selected</#if>>${roleDefinition.description}</option>
		</#list>
	</@f.select>
	<div class="help-block">
		The "starting point" for this custom role definition.
	</div>
</@form.labelled_row>

<div class="submit-buttons">
	<input type="submit" value="${submit_text}" class="btn btn-primary">
	<a class="btn" href="<@routes.admin.customroles department />">Cancel</a>
</div>

</@f.form>

</#compress>
</#escape>