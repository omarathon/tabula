<#import "../related_students/related_students_macros.ftl" as tutee_macros />

<#escape x as x?html>
<div id="tutors">
	<h1>My personal tutees</h1>
	
	<#if tutees?has_content>

		<@tutee_macros.table tutees=tutees is_relationship=true />

	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutees are currently visible for you in Tabula.</p>
	</#if>
</div>

</#escape>