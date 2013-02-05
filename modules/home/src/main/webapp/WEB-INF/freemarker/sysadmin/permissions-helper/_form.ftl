<@f.form action="${url('/sysadmin/permissions-helper')}" cssClass="form-horizontal" commandName="permissionsHelperCommand">
	<fieldset>
		<@form.labelled_row "user" "User ID">
  			<@form.userpicker path="user" object=true />
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
							<@f.option value="${permission._1}" label=permission._2 />
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