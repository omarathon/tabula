<#compress>
<#escape x as x?html>

<h1>Department permissions</h1>

<h2>${department.name}</h2>

<div class="permission-group">

	<h3>Department admins</h3>
	
	<div class="description">
		Over-arcing administrators for departments and everything in them. 
	</div>
	
	<#assign deptperms_url><@url page="/sysadmin/departments/${department.code}/permissions" /></#assign>
	
	<@f.form action="${deptperms_url}" method="post" commandName="addCommand" cssClass="form-inline">
		<@form.row path="usercodes">
			<input type="hidden" name="_command" value="add">
			<input type="hidden" name="department" value="${department.code}">
			Add user <@form.userpicker path="usercodes" />
			<input class="btn" type="submit" value="Add">
			<@f.errors path="usercodes" cssClass="error help-inline" />
		</@form.row>
	</@f.form>
	
	<#macro useractions user_id>
		<form action="${deptperms_url}" class="form-tiny" method="post" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
			<input type="hidden" name="_command" value="remove">
			<input type="hidden" name="department" value="${department.code}">
			<input type="hidden" name="usercodes" value="${user_id}">
			<input class="btn" type="submit" value="Remove" >
		</form>
	</#macro>
	
	<#if department.owners.includeUsers?size gt 0>
	
		<table class="permission-list">
		<@userlookup ids=department.owners.includeUsers>
		<#list missing_ids as missing_id>
			<tr class="anon-user">
				<td>${missing_id}</td>
				<td>(user not found)</td>
				<td class="actions">
					<@useractions missing_id />
				</td>
			</tr>
		</#list>
		<#list department.owners.includeUsers as id>
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
			There are no department admins yet.
		</p>
	
	</#if>

</div>

</#escape>
</#compress>
