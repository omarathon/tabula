<#include "form.ftl" />

<style type="text/css">
.profile-search-results .result {
	border: 1px solid #888;
	min-height: 50px;
	padding: 1em;
}

.profile-search-results .result h3 {
	margin-bottom: 0;
}

.profile-search-results .result .photo {
	float: right;
}

.profile-search-results .result .photo img {
	max-height: 50px;
}
</style>

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