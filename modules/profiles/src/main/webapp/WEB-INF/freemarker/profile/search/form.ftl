<section class="search">
	<h2>Search for a profile</h2>
	
	<@f.form method="get" action="${url('/search')}" commandName="searchProfilesCommand" cssClass="form-search">
		<@f.input path="query" cssClass="input-large search-query" />
		<button type="submit" class="btn">Search</btn>
	</@f.form>
</section>