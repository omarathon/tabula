<#compress>
<#escape x as x?html>

<div id="department-permissions-page">
	<h1>Departmental permissions for ${department.name}</h1>

	<#assign deptperms_url><@routes.deptperms department/></#assign>

<@f.form action="${deptperms_url}" method="post" commandName="addCommand" cssClass="department-permissions">
	<h2>Add a:</h2>

	<@form.row path="usercodes">
	<input type="hidden" name="_command" value="add">

	<@form.label checkbox=true>
		<@f.radiobutton path="roleDefinition" value="PersonalTutorRoleDefinition" cssClass="default" />
		senior tutor
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
		${department.name}</p>
	</div>
</#if>

	<h3 class="permissionTitle">Senior tutors</h3> <a class="use-popover" id="popover-seniortutors" data-html="true"
	   data-original-title="Senior tutors"
	   data-content="A senior tutor can see everything that a personal tutor can, for every student in the department."><i class="icon-question-sign"></i></a>

	<#macro useractions user_id role>
		<form action="${deptperms_url}" class="form-tiny" method="post" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
			<input type="hidden" name="_command" value="remove">
			<input type="hidden" name="roleDefinition" value="${role}">
			<input type="hidden" name="usercodes" value="${user_id}">
			<a class="btn btn-danger btn-mini removeUser"><i class="icon-white icon-remove"></i></a>
		</form>
	</#macro>

	<#assign seniorTutors=usersWithRole('PersonalTutorRoleDefinition', department) />
	<#if seniorTutors?size gt 0>

		<table class="permission-list">
		<#list seniorTutors as u>
			<tr>
				<td class="user-fullname">${u.fullName}</td>
				<td class="user-id">(${u.userId})</td>
				<td class="actions">
					<@useractions u.userId "PersonalTutorRoleDefinition" />
				</td>
			</tr>
		</#list>
		</table>

	<#else>

		<p class="empty-list">
			There are no senior tutors yet.
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
