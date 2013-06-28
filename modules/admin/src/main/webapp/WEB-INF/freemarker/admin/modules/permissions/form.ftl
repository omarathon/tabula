<#compress>
<#escape x as x?html>

<div id="module-permissions-page">
	<h1>Module permissions for ${module.code?upper_case} // ${module.name}</h1>

	<#assign moduleperms_url><@routes.moduleperms module/></#assign>

<@f.form action="${moduleperms_url}" method="post" commandName="addCommand" cssClass="module-permissions">
	<h2>Add a:</h2>

	<@form.row path="usercodes">
	<input type="hidden" name="_command" value="add">

	<@form.label checkbox=true>
		<@f.radiobutton path="roleDefinition" value="ModuleManagerRoleDefinition" cssClass="default" />
		module manager
	</@form.label>
	<@form.label checkbox=true>
		<@f.radiobutton path="roleDefinition" value="ModuleAssistantRoleDefinition" />
		module assistant
	</@form.label>

	<@f.errors path="roleDefinition" cssClass="error help-inline" />

	<div class="form-inline">
		<@form.flexipicker path="usercodes" />
		<input class="btn" type="submit" value="Add">
	</div>
	<@f.errors path="usercodes" cssClass="error help-inline" />
	</@form.row>
</@f.form>


<#if users?? && role??>
	<div id="permissionsMessage" class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		<p>
			<#list users?keys as key>
				<strong>${users[key].getFullName()}</strong> <#if users[key].getFullName()!=""> (${key})</#if>
			</#list>
			<#if action = "add">
				<#assign actionWords = "now" />
				<#else>
				<#assign actionWords = "no longer" />
			</#if>

			<#if users?size gt 1>
			  <br />
				are ${actionWords} <@fmt.role_definition_description role />s for
			<#else>
				is  ${actionWords} a <@fmt.role_definition_description role /> for
			</#if>
		${module.code?upper_case} ${module.name}</p>
	</div>
</#if>

<!--<div class="permission-group">-->

	<h3 class="permissionTitle">Module managers</h3> <a class="use-popover" id="popover-modulemanager" data-html="true"
	   data-original-title="Module managers <button type='button' onclick=&quot;jQuery('#popover-modulemanager').popover('hide')&quot; class='close'>&times;</button></span>"
	   data-content="A module manager can create and delete assignments, download submissions and publish feedback for this module."><i class="icon-question-sign"></i></a>

	<#macro useractions user_id role>
		<form action="${moduleperms_url}" class="form-tiny" method="post" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
			<input type="hidden" name="_command" value="remove">
			<input type="hidden" name="roleDefinition" value="${role}">
			<input type="hidden" name="usercodes" value="${user_id}">
			<a class="btn btn-danger btn-mini removeUser"><i class="icon-white icon-remove"></i></a>
		</form>
	</#macro>
	
	<#if module.managers.includeUsers?size gt 0>
	
		<table class="permission-list">
		<@userlookup ids=module.managers.includeUsers>
		<#list missing_ids as missing_id>
			<tr class="anon-user">
				<td class="user-fullname">User not found</td>
				<td class="user-id">(${missing_id})</td>
				<td class="actions">
					<@useractions missing_id "ModuleManagerRoleDefinition" />
				</td>
			</tr>
		</#list>
		<#list module.managers.includeUsers as id>
			<#if returned_users[id].foundUser>
			<#assign u=returned_users[id] />
			<tr>
				<td class="user-fullname">${u.fullName}</td>
				<td class="user-id">(${u.userId})</td>
				<td class="actions">
					<@useractions u.userId "ModuleManagerRoleDefinition" />
				</td>
			</tr>
			</#if>
		</#list>
		</@userlookup>
		</table>
		
	<#else>
	
		<p class="empty-list">
			There are no module managers yet.
		</p>
	
	</#if>

<!--</div> --> <!-- perm group -->


<h3 class="permissionTitle">Module assistants</h3> <a class="use-popover" id="popover-moduleassistant" data-html="true"
   data-original-title="Module assistants <button type='button' onclick=&quot;jQuery('#popover-moduleassistant').popover('hide')&quot; class='close'>&times;</button></span>"
   data-content="A module assistant can create assignments and download submissions, but cannot delete assignments or submissions, or publish feedback for this module."><i class="icon-question-sign"></i></a>

<#if module.assistants.includeUsers?size gt 0>

	<table class="permission-list">
		<@userlookup ids=module.assistants.includeUsers>
		<#list missing_ids as missing_id>
			<tr class="anon-user">
				<td class="user-fullname">User not found</td>
				<td class="user-id">(${missing_id})</td>
				<td class="actions">
					<@useractions missing_id "ModuleAssistantRoleDefinition" />
				</td>
			</tr>
		</#list>
		<#list module.assistants.includeUsers as id>
			<#if returned_users[id].foundUser>
				<#assign u=returned_users[id] />
				<tr>
					<td class="user-fullname">${u.fullName}</td>
					<td class="user-id">(${u.userId})</td>
					<td class="actions">
						<@useractions u.userId "ModuleAssistantRoleDefinition"/>
					</td>
				</tr>
			</#if>
		</#list>
	</@userlookup>
	</table>

	<#else>

		<p class="empty-list">
			There are no module assistants yet.
		</p>

</#if>

</div>
<script>
	jQuery(function($) {
		$('.removeUser').click(function() {
			$(this).parent("form").submit();
		});

		$('.removeUser').hover(
			function() {
				$(this).closest("tr").find("td").addClass("highlight");
			},
			function() {
				$(this).closest("tr").find("td").removeClass("highlight");
			}
		);

		if (!$("input[name='roleDefinition']:checked").val()) $("input[name='roleDefinition'].default").attr('checked',true);

	});
</script>

</#escape>
</#compress>
