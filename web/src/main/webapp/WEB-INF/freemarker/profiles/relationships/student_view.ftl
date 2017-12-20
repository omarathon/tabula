<#escape x as x?html>

<h1><#if member?has_content>${relationshipType.studentRole?cap_first}s<#else>My ${relationshipType.studentRole}s</#if></h1>
<#if studentCourseDetails?has_content>
	<#if member?has_content>
		<#assign submitUrl>/profiles/${relationshipType.urlPart}/${member.universityId}/students</#assign>
	<#else>
		<#assign submitUrl>/profiles/${relationshipType.urlPart}/students</#assign>
	</#if>
	<#assign filterCommand = viewRelatedStudentsCommand />
	<#assign filterCommandName = "viewRelatedStudentsCommand" />
	<#assign filterResultsPath = "/WEB-INF/freemarker/profiles/relationships/student_view_results.ftl" />
	<#include "/WEB-INF/freemarker/filter_bar.ftl" />
<#else>
	<p class="alert alert-info">No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
</#if>
</#escape>
