<section class="tutor-search">	
	<@f.form method="get" action="${url('/tutor/search')}" commandName="searchTutorsCommand" class="form-horizontal">
		<input id="studentUniId" name="studentUniId" type="hidden" value="${student.universityId}" />
		<div class="control-group">
			<label class="control-label" for="personal-tutor"><b>Personal Tutor</b></label>
			<div class="controls">
				<@f.input type="text" id="personal-tutor" name="personal-tutor" path="query" 
					placeholder="${tutorToDisplay.firstName} ${tutorToDisplay.lastName} (${tutorToDisplay.universityId})" />
				<button type="submit" class="btn btn-primary">
					<i id="tutor-search-icon" class="icon-search icon-white"></i>
				</button>
				<a href="<@routes.tutor_edit_no_tutor studentUniId="${student.universityId}" />" class="btn">Reset</a>
			</div>
		</div>
	</@f.form>
</section>

