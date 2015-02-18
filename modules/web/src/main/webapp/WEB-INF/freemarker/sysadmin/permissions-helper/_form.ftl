<@f.form action="${url('/sysadmin/permissions-helper')}" cssClass="form-horizontal" commandName="permissionsHelperCommand">
	<fieldset>
		<@form.labelled_row "user" "User ID">
  			<@form.flexipicker path="user" placeholder="Type name or usercode" />
		</@form.labelled_row>
		
		<@form.labelled_row "scopeType" "Permission scope type">
			<label class="radio">
				<input type="radio" name="${status.expression}" value="" <#if !status.value?? || status.value?is_string>checked</#if> /> 
				None
			</label>
		
			<#list allPermissionTargets as targetClass>
				<label class="radio">
					<@f.radiobutton path="scopeType" value="${targetClass.name}" />
					${targetClass.simpleName}
				</label>
			</#list>
		</@form.labelled_row>
		
		<@form.labelled_row "scope" "Permission scope">
			<@f.input path="scope" cssClass="text" /><br><small>Type the ID or code that you wish to use as a permission scope</small>
		</@form.labelled_row>
		
		<@form.labelled_row "permission" "Permission">
			<@f.select path="permission">
				<option value="">Show all</option>
				<#list allPermissions?keys as group>
					<#if group == "">
						<optgroup label="Global">
					<#else>
						<optgroup label="${group}">
					</#if>
						<#list allPermissions[group] as permission>
							<option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
						</#list>
					</optgroup>
				</#list>
			</@f.select>
		</@form.labelled_row>
	</fieldset>
	
	<div class="submit-buttons">
	<input class="btn btn-large btn-primary" type="submit" value="Submit">
	</div>
</@f.form>