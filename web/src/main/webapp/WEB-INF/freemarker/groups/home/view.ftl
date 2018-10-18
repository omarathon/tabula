<#import "*/group_components.ftl" as components />
<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@routes.groups.departmenthome department />">
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
		This is a service for managing small group teaching (e.g. seminars, tutorials and lab groups).
	</p>

	<#if IS_SSO_PROTECTED!true>
		<p class="alert">
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
	</#if>
<#else>
	<#include "_admin.ftl" />
	<#include "_student.ftl" />

	<#if todaysModules.moduleItems?has_content>
		<h2>Today's events</h2>
		<@components.module_info todaysModules />
	</#if>

	<#assign is_student=user.student /> <#-- Non-students may also have groups, but we still show them the intro text -->
	<#assign is_alumni=user.alumni />
	<#assign is_tutor=nonempty(taughtGroups) />
	<#assign is_admin=(nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)) />

	<#if !is_student && !is_tutor && !is_admin && !is_alumni> <#-- Don't just show an empty page -->
		<p class="lead muted">
			This is a service for managing small group teaching (e.g. seminars, tutorials and lab groups).
		</p>

		<p>
			You do not currently have permission to manage any small groups. If you think this is incorrect or you need assistance, please visit our <a href="/help">help page</a>.
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
