<#if searchProfilesCommand?has_content>
	<section class="profile-search">
		<@f.form method="get" action="${url('/search')}" commandName="searchProfilesCommand">
			<div class="input-append">
				<@f.input path="query" placeholder="Search for a student..." /><button class="btn" type="submit"><i class="icon-search"></i></button>
			</div>
			<span style="margin-left: 10px;" class="use-tooltip" data-toggle="tooltip" data-html="true" data-placement="right" data-title="Start typing a student's name, or put their University ID in, and we'll show you a list of results. Any student who studies in your department should be included."></span>
		</@f.form>
	</section>
</#if>

