<#compress>
<#escape x as x?html>

<h1>Department permissions</h1>

<h2>${department.name}</h2>

<div class="permission-group">

	<h3>Senior tutors</h3>
	
	<div class="description">
		People who can see everything that a personal tutor can, for every student in the department. 
	</div>
	
	<#assign deptperms_url><@routes.deptperms department/></#assign>
	
	<@f.form action="${deptperms_url}" method="post" commandName="addCommand" cssClass="form-inline">
		<@form.row path="usercodes">
			<input type="hidden" name="_command" value="add">
			<input type="hidden" name="roleDefinition" value="PersonalTutorRoleDefinition">
			Add user <@form.userpicker path="usercodes" />
			<input class="btn" type="submit" value="Add">
			<@f.errors path="usercodes" cssClass="error help-inline" />
		</@form.row>
	</@f.form>
	
	<#macro useractions user_id>
		<form action="${deptperms_url}" class="form-tiny" method="post" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
			<input type="hidden" name="_command" value="remove">
			<input type="hidden" name="roleDefinition" value="PersonalTutorRoleDefinition">
			<input type="hidden" name="usercodes" value="${user_id}">
			<input class="btn" type="submit" value="Remove" >
		</form>
	</#macro>
	
	<#assign seniorTutors=usersWithRole('PersonalTutorRoleDefinition', department) />
	<#if seniorTutors?size gt 0>
	
		<table class="permission-list">
		<tbody>
		<#list seniorTutors as u>
			<tr>
				<td>${u.userId}</td>
				<td>(${u.fullName})</td>
				<td class="actions">
					<@useractions u.userId />
				</td>
			</tr>
		</#list>
		</tbody>
		</table>
		
	<#else>
	
		<p class="empty-list">
			There are no senior tutors yet.
		</p>
	
	</#if>

</div>

</#escape>
</#compress>
