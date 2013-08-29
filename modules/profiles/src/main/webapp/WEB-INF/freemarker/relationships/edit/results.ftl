<h3>${relationshipType.studentRole?cap_first}: ${student.firstName} ${student.lastName} (${student.universityId})</h3>
<#include "form.ftl" />
<section class="results">
	<h2>Results</h2>

	<#if results?size = 0>
		<p>No profiles were found.</p>
	<#else>
		<div class="profile-search-results">
			<#list results as result>
				<article class="result" data-id="${result.universityId}">
					<h3><a href="<#compress>
						<#if agentToDisplay??>
							<@routes.relationship_edit_replace scjCode=studentCourseDetails.urlSafeId currentAgent=agentToDisplay newAgent=result relationshipType=relationshipType />
						<#else>
							<@routes.relationship_edit_set scjCode=studentCourseDetails.urlSafeId newAgent=result relationshipType=relationshipType />
						</#if>
					</#compress>"><@fmt.profile_name result /></a></h3>
					<@fmt.profile_description result />
				</article>
			</#list>
		</div>
	</#if>
</section>
