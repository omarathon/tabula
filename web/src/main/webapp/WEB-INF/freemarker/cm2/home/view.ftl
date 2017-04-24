<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<#-- Do we expect this user to submit assignments, and therefore show them some text even if there aren't any? -->
<#assign expect_assignments = user.student || user.PGR || user.alumni />
<#assign is_marker = false /> <#-- TODO -->
<#assign is_admin = nonempty(moduleManagerDepartments) || nonempty(adminDepartments) />

<#if expect_assignments || !studentInformation.empty>
	<#include "_student.ftl" />
</#if>

<#if is_admin>
	<#include "_admin.ftl" />
</#if>

<#if !expect_assignments && studentInformation.empty && !is_marker && !is_admin>
	<h1>Coursework Management</h1>

	<p class="lead muted">
		This is a service for managing coursework assignments and feedback.
	</p>

	<p>
		<#if homeDepartment??>
			<#assign uams = usersWithRole('UserAccessMgrRoleDefinition', homeDepartment) />
		</#if>
		You do not currently have permission to manage any assignments or feedback. If you believe this is an error then please
		<#if uams?has_content>
			contact your department's <a href="mailto:${uams?first.email}">User Access Manager</a> for Tabula, or
		<#else>
			contact your departmental access manager for Tabula, or
		</#if>
		email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
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

</#escape>