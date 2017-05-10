<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<#-- Do we expect this user to submit assignments, and therefore show them some text even if there aren't any? -->
<#assign expect_assignments = user.student || user.PGR || user.alumni />
<#assign is_student = expect_assignments || !studentInformation.empty />
<#assign is_marker = !markerInformation.empty />
<#assign is_admin = !adminInformation.empty />

<#if is_student>
	<#include "_student.ftl" />
</#if>

<#if is_marker>
	<#include "_marker.ftl" />
</#if>

<#if is_admin || is_marker> <#-- Markers get the activity stream -->
	<#include "_admin.ftl" />
</#if>

<#include "_marks_management_admin.ftl" />

<#if !is_student && !is_marker && !is_admin>
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