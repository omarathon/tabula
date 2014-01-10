<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@url page="/admin/department/${department.code}/"/>">
		Go to the ${department.name} admin page
	</a>
</#macro>

<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>

<#if !user.loggedIn>
	<p class="lead muted">
		This is a service for managing coursework assignments and feedback
	</p>	

	<p class="alert">
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
</#if>

<#assign isSelf = true />
<#include "_student.ftl" />
<#include "_markers.ftl" />
<#include "_admin.ftl" />

<#if user.loggedIn>

	<#assign is_student=user.student /> <#-- Non-students may also have assignments, but we still show them the intro text -->
	<#assign is_alumni=user.alumni />
	<#assign is_marker=nonempty(assignmentsForMarking) />
	<#assign is_admin=(nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)) />
	
	<#if !is_alumni && !is_student && !is_marker && !is_admin> <#-- Don't just show an empty page -->
		<p class="lead muted">
			This is a service for managing coursework assignments and feedback
		</p>
		
		<p>
			You do not currently have permission to manage any assignments or feedback. Please contact your
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