<#import "profile_macros.ftl" as profile_macros />
<#escape x as x?html>

<#if user.staff>
	<#include "../profile/search/form.ftl" />

	<hr class="full-width" />
</#if>

<#if profile.deceased>
	<div class="alert alert-danger">
		<button type="button" class="close" data-dismiss="alert">&times;</button>

		<h4>This student is recorded as deceased</h4>
	</div>
</#if>

<#if can.do("RolesAndPermissions.Create", profile)>
	<div class="pull-right">
		<#assign permissions_url><@routes.profiles.permissions profile /></#assign>
		<@fmt.permission_button
			permission='RolesAndPermissions.Create'
			scope=profile
			action_descr='modify permissions'
			classes='btn'
			href=permissions_url
			tooltip='Permissions'
		>
			Permissions
		</@fmt.permission_button>
	</div>
</#if>

<article class="profile">
	<#include "_personal_details.ftl" />

	<#if profile.isStaff()>
		<#include "_staff_details.ftl" />
	</#if>

	<#if (features.profilesMemberNotes && can.do('MemberNotes.Read', profile)) >
			<#include "_member_notes.ftl" />
	</#if>

	<!-- the check on profile.freshStudentCourseDetails is just a way of nailing down the permissions -->
	<#if can.do("Profiles.Read.StudentCourseDetails.Core", profile) && (studentCourseDetails)?? && (profile.freshStudentCourseDetails)??>
		<#include "_courses.ftl" />
	</#if>

	<p class="rendered-timestamp">
		Page generated ${.now}
	</p>
</article>

<#if user.sysadmin>
	<div class="alert alert-info sysadmin-only-content" style="margin-top: 2em;">
		<button type="button" class="close" data-dismiss="alert">&times;</button>

		<h4>Sysadmin-only actions</h4>

		<p>This is only shown to Tabula system administrators. Click the &times; button to see the page as a non-administrator sees it.</p>

		<@f.form method="post" action="${url('/sysadmin/import-profiles/' + profile.universityId)}">
			<button class="btn btn-large" type="submit">Re-import details from FIM and SITS</button>
		</@f.form>
	</div>
</#if>
</#escape>
