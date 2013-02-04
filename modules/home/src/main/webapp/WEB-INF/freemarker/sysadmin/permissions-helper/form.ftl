<h1>Permissions helper</h1>

<p>Discover why a user does or does not have certain permissions...</p>

<@f.form action="" cssClass="form-horizontal" commandName="permissionsHelperCommand">
	<fieldset>
		<@form.labelled_row "user" "User ID">
  			<@form.userpicker name="user" />
		</@form.labelled_row>
		
		<@form.labelled_row "scopeType" "Permission scope type">
			<label class="radio">
				<input type="radio" name="${status.expression}" value="" <#if !status.value??>checked</#if> /> 
				None
			</label>
		
			<#list allPermissionTargets as targetClass>
				<label class="radio">
					<input type="radio" name="${status.expression}" value="${targetClass.name}" <#if status.value?? && status.value = targetClass.name>checked</#if> /> 
					${targetClass.simpleName}
				</label>
			</#list>
		</@form.labelled_row>
		
		<@form.labelled_row "scope" "Permission scope">
			<@f.input path="scope" cssClass="text" /><br><small>Type the ID or code that you wish to use as a permission scope</small>
		</@form.labelled_row>
		
		<@form.labelled_row "permission" "Permission">
			<select name="${status.expression}">
				<option value="">Show all</option>
				<#list allPermissions?keys as group>
					<#if group == "">
						<optgroup label="Global">
					<#else>
						<optgroup label="${group}">
					</#if>
						<#list allPermissions[group] as permission>
							<option value="${permission._1}">${permission._2}</option>
						</#list>
					</optgroup>
				</#list>
			</select>
		</@form.labelled_row>
	</fieldset>
	
	<div class="submit-buttons">
	<input class="btn btn-large btn-primary" type="submit" value="Submit">
	</div>
</@f.form>