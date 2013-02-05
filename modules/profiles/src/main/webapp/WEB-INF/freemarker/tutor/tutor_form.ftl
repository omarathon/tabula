<section class="tutor-search">	
	<@f.form method="get" action="${url('/tutor/tutor_search')}" commandName="tutorSearchProfilesCommand">
		Personal Tutor 
		<div class="input-append">
			<input id="studentUniId" name="studentUniId" type="hidden" value="${studentUniId}" />
			<@f.input path="query" value=tutorName />
		</div>
		&nbsp;&nbsp;&nbsp;&nbsp;<input class="btn" type="submit" value="Search">
	</@f.form>
</section>
