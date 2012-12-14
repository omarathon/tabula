<#include "form.ftl" />

<section class="results">
	<h2>Results</h2>
	
	<#if results?size = 0>
		<p>No profiles were found.</p>
	<#else>
		<div class="profile-search-results">
			<#list results as profile>
				<article class="result" data-id="${profile.universityId}">
					<div class="photo">
						<img src="<@routes.photo profile />" />
					</div>
				
					<h3><a href="<@routes.profile profile />"><@fmt.profile_name profile /></a></h3>
					<@fmt.profile_description profile />
				</article>
			</#list>
		</div>
	</#if>
</section>