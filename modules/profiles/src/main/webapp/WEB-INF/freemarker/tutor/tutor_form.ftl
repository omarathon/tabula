<section class="tutor-search">	
	<@f.form method="get" action="${url('/tutor/tutor_search')}" commandName="tutorSearchProfilesCommand">
		Personal tutor 
		<input id="studentUniId" name="studentUniId" type="hidden" value="${student.universityId}" />
		<div class="input-box">
			<@f.input path="query" value="${tutorToDisplay}" />
		</div>
		<button type="submit" class="btn btn-primary">
			<i class="icon-search icon-white"></i>
		</button>
	</@f.form>
</section>
