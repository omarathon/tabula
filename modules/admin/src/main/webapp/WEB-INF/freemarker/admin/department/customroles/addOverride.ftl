<#escape x as x?html>
	<#assign form_url><@routes.addcustomroleoverride customRoleDefinition /></#assign>

	<h1>Add custom role override</h1>

	<@f.form method="post" action="${form_url}" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<fieldset>
			<@form.labelled_row "permission" "Permission">
				<@f.select path="permission">
					<#list allPermissions?keys as group>
						<optgroup label="${group}">
							<#list allPermissions[group] as permission>
								<option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
							</#list>
						</optgroup>
					</#list>
				</@f.select>
			</@form.labelled_row>

			<@form.labelled_row "overrideType" "Allowed?">
				<@f.select path="overrideType">
					<option value="true">Allow</option>
					<option value="false">Disallow</option>
				</@f.select>
			</@form.labelled_row>
		</fieldset>

		<div class="submit-buttons">
			<input type="submit" value="Add" class="btn btn-primary">
			<a class="btn" href="<@routes.customroleoverrides customRoleDefinition />">Cancel</a>
		</div>
	</@f.form>
</#escape>