<#compress>
<#escape x as x?html>

<h1>Module permissions</h1>

<h2>${module.name}</h2>

<div class="permission-group">

	<h3>Module managers</h3>
	
	<div class="description">
		People who can upload and list feedback for this module. 
	</div>
	
	<#assign moduleperms_url><@routes.moduleperms module/></#assign>
	
	<@f.form action="${moduleperms_url}" method="post" commandName="addCommand">
		<input type="hidden" name="_command" value="add">
		<input type="hidden" name="module" value="${module.code}">
		<@f.errors path="usercodes" cssClass="error" />
		Add user <@form.userpicker path="usercodes" />
		<span class="inline-submit-buttons"><input type="submit" value="Add"></span>
	</@f.form>
	
	<#macro useractions user_id>
		<form action="${moduleperms_url}" method="post" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
			<input type="hidden" name="_command" value="remove">
			<input type="hidden" name="module" value="${module.code}">
			<input type="hidden" name="usercodes" value="${user_id}">
			<input type="submit" value="Remove" >
		</form>
	</#macro>
	
	<#if module.participants.includeUsers?size gt 0>
	
		<table class="permission-list">
		<@userlookup ids=module.participants.includeUsers>
		<#list missing_ids as missing_id>
			<tr class="anon-user">
				<td>${missing_id}</td>
				<td>(user not found)</td>
				<td class="actions">
					<@useractions missing_id />
				</td>
			</tr>
		</#list>
		<#list module.participants.includeUsers as id>
			<#if returned_users[id].foundUser>
			<#assign u=returned_users[id] />
			<tr>
				<td>${u.userId}</td>
				<td>(${u.fullName})</td>
				<td class="actions">
					<@useractions u.userId />
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

</div>

</#escape>
</#compress>
