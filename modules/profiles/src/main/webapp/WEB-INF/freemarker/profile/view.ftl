<#import "profile_macros.ftl" as profile_macros />
<#escape x as x?html>

<#if user.staff>
	<#include "search/form.ftl" />

	<hr class="full-width" />
</#if>

<article class="profile">
	<#include "_personal_details.ftl" />

	<#if (features.profilesMemberNotes && can.do('MemberNotes.Read', profile)) >
			<#include "_member_notes.ftl" />
	</#if>

	<#if (studentCourseDetails)??>
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

		<@f.form method="post" action="${url('/sysadmin/import-profiles/' + profile.universityId, '/scheduling')}">
			<button class="btn btn-large" type="submit">Re-import details from Membership, SITS and about a billion other systems</button>
		</@f.form>
	</div>
</#if>
</#escape>
