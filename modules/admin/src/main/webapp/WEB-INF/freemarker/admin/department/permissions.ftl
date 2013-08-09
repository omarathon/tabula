<#compress>
<#escape x as x?html>

<#macro useractions user_id role>
	<form action="${deptperms_url}" method="post" class="remove-permissions" onsubmit="return confirm('Are you sure you want to remove permission for this user?');">
		<input type="hidden" name="_command" value="remove">
		<input type="hidden" name="roleDefinition" value="${role}">
		<input type="hidden" name="usercodes" value="${user_id}">
		<a class="btn btn-danger btn-mini removeUser"><i class="icon-white icon-remove"></i></a>
	</form>
</#macro>

<#macro roleTable cssClass roleDefinition roleNamePlural>
	<table class="table table-bordered table-condensed permission-list ${cssClass}">
		<tbody>
			<tr>
				<td>
					<div class="form-inline">
						<@form.flexipicker cssClass="pickedUser" name="usercodes" />
					</div>
				</td>
				<td class="actions">
					<form action="${deptperms_url}" method="post" class="add-permissions">
						<input type="hidden" name="_command" value="add">
						<input type="hidden" name="roleDefinition" value="${roleDefinition}">
						<input type="hidden" name="usercodes">
						<button class="btn btn-mini" type="submit"><i class="icon-plus"></i></button>
					</form>
				</td>
			</tr>

			<#assign users = usersWithRole('${roleDefinition}', department) />
			<#if users?size gt 0>
				<#list users as u>
					<tr>
						<td class="user">
							${u.fullName} <span class="muted">${u.userId}</span>
						</td>
						<td class="actions">
							<@useractions u.userId "${roleDefinition}" />
						</td>
					</tr>
				</#list>
			<#else>
				<tr>
					<td colspan="2" class="empty-list">
						<i class="icon-info-sign"></i> There are no ${roleNamePlural} yet.
					</td>
				</tr>
			</#if>
		</tbody>
	</table>
</#macro>

<div id="department-permissions-page">
	<h1>Departmental permissions</h1>
	<h5>for ${department.name}</h5>

	<#assign deptperms_url><@routes.deptperms department/></#assign>

	<#assign bindingError><@f.errors path="addCommand.*" /></#assign>
	<#if bindingError?has_content>
		<p class="alert alert-error">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<i class="icon-warning-sign"></i> <#noescape>${bindingError}</#noescape>
		</p>
	</#if>

	<#if users?? && role??>
		<div id="permissionsMessage" class="alert alert-success">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<p><i class="icon-ok"></i>
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

			<@roleTable "tutor-table" "PersonalTutorRoleDefinition" "senior tutors" />
		</div>

		<div class="span6">
			<h3 class="permissionTitle">Senior supervisors</h3> <a class="use-popover" id="popover-seniorsupervisors" data-html="true"
			   data-original-title="Senior supervisors"
			   data-content="A senior supervisor can see everything that a supervisor can, for every student in the department."><i class="icon-question-sign"></i></a>

			<@roleTable "supervisor-table" "SupervisorRoleDefinition" "senior supervisors" />
		</div>
	</div>
</div>

<script>
	jQuery(function($) {
		$('.removeUser').click(function() {
			$(this).parent("form").submit();
		});

		// copy to hidden field to avoid breaking table/form DOM hierarchy
		$('input.pickedUser').change(function() {
			$(this).closest('table').find('.add-permissions input[name=usercodes]').val($(this).val());
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
