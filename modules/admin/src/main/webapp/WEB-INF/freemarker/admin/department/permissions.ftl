<#compress>
<#escape x as x?html>

<#macro useractions user_id role>
	<form action="${deptperms_url}" method="post" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
		<input type="hidden" name="_command" value="remove">
		<input type="hidden" name="roleDefinition" value="${role}">
		<input type="hidden" name="usercodes" value="${user_id}">
		<a class="btn btn-danger btn-mini removeUser"><i class="icon-white icon-remove"></i></a>
	</form>
</#macro>

<div id="department-permissions-page">
	<h1>Departmental permissions</h1>
	<h5>for ${department.name}</h5>

	<#assign deptperms_url><@routes.deptperms department/></#assign>

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

	<div class="row-fluid">
		<div class="span6">
			<h3 class="permissionTitle">Senior tutors</h3> <a class="use-popover" id="popover-seniortutors" data-html="true"
			   data-original-title="Senior tutors"
			   data-content="A senior tutor can see everything that a personal tutor can, for every student in the department."><i class="icon-question-sign"></i></a>

			<table class="table table-bordered table-condensed permission-list tutor-form">
				<tbody>
					<tr>
						<td>
							<div class="form-inline">
								<@form.flexipicker cssClass="pickedUser" path="addCommand.usercodes" />
							</div>
							<@f.errors path="addCommand.usercodes" cssClass="error help-inline" />
						</td>
						<td class="actions">
							<@f.form action="${deptperms_url}" method="post" commandName="addCommand" cssClass="department-permissions">
								<input type="hidden" name="_command" value="add">
								<input type="hidden" name="roleDefinition" value="PersonalTutorRoleDefinition">
								<input type="hidden" name="usercodes">
								<button class="btn btn-mini" type="submit"><i class="icon-plus"></i></button>
							</@f.form>
						</td>
					</tr>

					<#assign seniorTutors = usersWithRole('PersonalTutorRoleDefinition', department) />
					<#if seniorTutors?size gt 0>
						<#list seniorTutors as u>
							<tr>
								<td class="user">
									${u.fullName} <span class="muted">${u.userId}</span>
								</td>
								<td class="actions">
									<@useractions u.userId "PersonalTutorRoleDefinition" />
								</td>
							</tr>
						</#list>
					<#else>
						<tr>
							<td colspan="2" class="empty-list">
								<i class="icon-info-sign"></i> There are no senior tutors yet.
							</td>
						</tr>
					</#if>
				</tbody>
			</table>
		</div>

		<div class="span6">
			<h3 class="permissionTitle">Senior supervisors</h3> <a class="use-popover" id="popover-seniorsupervisors" data-html="true"
			   data-original-title="Senior supervisors"
			   data-content="A senior supervisor can see everything that a supervisor can, for every student in the department."><i class="icon-question-sign"></i></a>

			<table class="table table-bordered table-condensed permission-list supervisor-form">
				<tbody>
					<tr>
						<td>
							<div class="form-inline">
								<@form.flexipicker cssClass="pickedUser" path="addCommand.usercodes" />
							</div>
							<@f.errors path="addCommand.usercodes" cssClass="error help-inline" />
						</td>
						<td class="actions">
							<@f.form action="${deptperms_url}" method="post" commandName="addCommand" cssClass="department-permissions">
								<input type="hidden" name="_command" value="add">
								<input type="hidden" name="roleDefinition" value="SupervisorRoleDefinition">
								<input type="hidden" name="usercodes">
								<button class="btn btn-mini" type="submit"><i class="icon-plus"></i></button>
							</@f.form>
						</td>
					</tr>

					<#assign seniorSupervisors = usersWithRole('SupervisorRoleDefinition', department) />
					<#if seniorSupervisors?size gt 0>
						<#list seniorSupervisors as u>
							<tr>
								<td class="user">
									${u.fullName} <span class="muted">${u.userId}</span>
								</td>
								<td class="actions">
									<@useractions u.userId "SupervisorRoleDefinition" />
								</td>
							</tr>
						</#list>
					<#else>
						<tr>
							<td colspan="2" class="empty-list">
								<i class="icon-info-sign"></i> There are no senior supervisors yet.
							</td>
						</tr>
					</#if>
				</tbody>
			</table>
		</div>
	</div>
</div>

<script>
	jQuery(function($) {
		$('.removeUser').click(function() {
			$(this).parent("form").submit();
		});

		$('.pickedUser').change(function() {
			$(this).closest('table').find('[name=usercodes]').val($(this).val());
		});

		$('.removeUser').hover(function() {
				$(this).closest("tr").find("td").addClass("highlight");
			}, function() {
				$(this).closest("tr").find("td").removeClass("highlight");
			}
		);
	});
</script>

</#escape>
</#compress>
