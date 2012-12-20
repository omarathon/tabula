<#if user.staff>
	<section class="profile-search">	
		<@f.form method="get" action="${url('/search')}" commandName="searchProfilesCommand">
			<div class="input-append">
				<@f.input path="query" cssClass="span4" placeholder="Search for a student..." /><input class="btn" type="submit" value="Search">
			</div>
		</@f.form>
	</section>
</#if>