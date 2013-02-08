<#include "form.ftl" />

<section class="results">
	<h2>Results</h2>
	
	<#if results?size = 0>
		<p>No profiles were found.</p>
	<#else>
		<ul class="profile-user-list">
			<#list results as profile>
				<li data-id="${profile.universityId}">
					<div class="photo">
						<img src="<@routes.photo profile />" />
					</div>
				
					<h3><a href="<@routes.profile profile />"><@fmt.profile_name profile /></a></h3>
					<@fmt.profile_description profile />
				</li>
			</#list>
		</ul>
	</#if>
</section>