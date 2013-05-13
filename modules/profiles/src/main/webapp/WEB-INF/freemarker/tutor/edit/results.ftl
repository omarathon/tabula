<h3>Personal Tutee: ${student.firstName} ${student.lastName} (${student.universityId})</h3>
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
						<#if tutorToDisplay??>
							<@routes.tutor_edit_replace student=student.universityId currentTutor=tutorToDisplay newTutor=result />
						<#else>
							<@routes.tutor_edit_set student=student.universityId newTutor=result />
						</#if>
					</#compress>"><@fmt.profile_name result /></a></h3>
					<@fmt.profile_description result />
				</article>
			</#list>
		</div>
	</#if>
</section>
