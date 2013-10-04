<#escape x as x?html>

	<#if user.loggedIn && user.firstName??>
		<h1 class="with-settings">Hello, ${user.firstName}</h1>
	<#else>
		<h1 class="with-settings">Hello</h1>
	</#if>
	
	<#assign can_record = (permissionMap["View"]?size > 0) />
	<#assign can_manage = (permissionMap["Manage"]?size > 0) />
	
	<#if can_record || can_manage>
		<#if (permissionMap["View"]?size > 0)>
			<h2>View and record monitoring points</h2>
			<ul class="links">
				<#list permissionMap["View"] as department>
					<li>
						<a href="<@url page="/${department.code}"/>">${department.name}</a>
					</li>
				</#list>
			</ul>
		</#if>
		
		<#if (permissionMap["Manage"]?size > 0)>
			<h2>Create and edit monitoring schemes</h2>
			<ul class="links">
				<#list permissionMap["Manage"] as department>
					<li>
						<a href="<@url page="/manage/${department.code}"/>">${department.name}</a>
					</li>
				</#list>
			</ul>
		</#if>
	<#else>
		<p class="lead muted">
			This is a service for managing attendance monitoring points.
		</p>
		
		<#if user.staff>
			<p>
				You do not currently have permission to manage any monitoring points. Please contact your
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