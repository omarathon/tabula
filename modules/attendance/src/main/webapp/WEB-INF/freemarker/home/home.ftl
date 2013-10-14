<#escape x as x?html>

	<#if user.loggedIn && user.firstName??>
		<h1 class="with-settings">Hello, ${user.firstName}</h1>
	<#else>
		<h1 class="with-settings">Hello</h1>
	</#if>

	<p class="lead muted">
		This is a service for viewing and managing attendance monitoring points.
	</p>

	<#if hasProfile>
		<h2>
			<a href="<@routes.attendanceProfile />">My attendance profile</a>
		</h2>
	</#if>

	<#assign can_record = (permissionMap["ViewPermissions"]?size > 0) />
	<#assign can_manage = (permissionMap["ManagePermissions"]?size > 0) />

	<#if can_record || can_manage>
		<#if (permissionMap["ViewPermissions"]?size > 0)>
			<h2>View and record monitoring points</h2>
			<ul class="links">
				<#list permissionMap["ViewPermissions"] as department>
					<li>
						<a id="view-department-${department.code}" href="<@routes.viewDepartment department />">${department.name}</a>
					</li>
				</#list>
			</ul>
		</#if>
		
		<#if (permissionMap["ManagePermissions"]?size > 0)>
			<h2>Create and edit monitoring schemes</h2>
			<ul class="links">
				<#list permissionMap["ManagePermissions"] as department>
					<li>
						<a id="manage-department-${department.code}" href="<@routes.manageDepartment department />">${department.name}</a>
					</li>
				</#list>
			</ul>
		</#if>
	<#else>
		<#if user.staff>
			<p>
				You do not currently have permission to view or manage any monitoring points. Please contact your
				departmental access manager for Tabula, or email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
			</p>
					
			<script type="text/javascript">
				jQuery(function($) {
					$('#email-support-link').on('click', function(e) {
						e.stopPropagation();
						e.preventDefault();
						$('#app-feedback-link').click();
					});
				});
			</script>
		</#if>
	</#if>

</#escape>