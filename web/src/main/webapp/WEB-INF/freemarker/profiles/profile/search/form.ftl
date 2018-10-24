<#if searchProfilesCommand?has_content>
	<section class="profile-search profile-search-results">
		<@f.form method="get" action="${url('/profiles/search')}" commandName="searchProfilesCommand">
			<div class="input-group">
				<@f.input path="query" placeholder="Find people by name or University ID" cssClass="form-control" />
				<div class="input-group-btn">
					<button class="btn btn-default" aria-label="search" type="submit"><i class="fa fa-search"></i></button>
				</div>
			</div>
			<@bs3form.form_group>
				<@bs3form.radio>
					<input type="radio" name="searchAllDepts" checked value="false">
					People in my department
				</@bs3form.radio>
				<@bs3form.radio>
					<input type="radio" name="searchAllDepts" value="true">
					Everyone
				</@bs3form.radio>
			</@bs3form.form_group>

			<#if features.profilesSearchPast>
				<@bs3form.form_group checkbox=true>
					<@bs3form.checkbox>
						<input type="checkbox" name="includePast" value="true">
						Include past students and staff
					</@bs3form.checkbox>
				</@bs3form.form_group>
			</#if>
		</@f.form>
	</section>
</#if>
