<#escape x as x?html>
	<fieldset>
		<@form.labelled_row "format" "Type">
			<@f.select path="format" id="format">
				<@f.options items=allFormats itemLabel="description" itemValue="code" />
			</@f.select>
		</@form.labelled_row>

		<#if newRecord>
			<@form.labelled_row "academicYear" "Academic year">
				<@f.select path="academicYear" id="academicYear">
					<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
				</@f.select>
			</@form.labelled_row>

			<script type="text/javascript">
				jQuery(function($) {
					$('#academicYear').on('change', function(e) {
						var $form = $(this).closest('form');
						$('#action-input').val('refresh');

						$form.submit();
					});
				});
			</script>
		<#else>
			<@form.labelled_row "academicYear" "Academic year">
				<@spring.bind path="academicYear">
					<span class="uneditable-value">${status.actualValue.label} <span class="hint">(can't be changed)</span></span>
				</@spring.bind>
			</@form.labelled_row>
		</#if>

		<@form.labelled_row "name" "Set name">
			<@f.input path="name" cssClass="text" />
			<a class="use-popover" data-html="true"
			 data-content="Give this set of groups an optional name to distinguish it from any other sets of the same type - eg. Term 1 seminars and Term 2 seminars">
			<i class="icon-question-sign"></i>
		  </a>
		</@form.labelled_row>

		<#if features.smallGroupTeachingStudentSignUp || features.smallGroupTeachingRandomAllocation>
			<@form.labelled_row "allocationMethod" "Allocation method">
				<label class="radio">
					<@f.radiobutton path="allocationMethod" value="Manual" />
					Manual allocation
					<a class="use-popover" data-html="true"
					 data-content="Allocate students by drag and drop or spreadsheet upload">
					<i class="icon-question-sign"></i>
				  </a>
				</label>
				<#if features.smallGroupTeachingStudentSignUp>
					<label class="radio">
						<@f.radiobutton path="allocationMethod" value="StudentSignUp" selector=".student-sign-up-options" />
						Self sign-up
						<a class="use-popover" data-html="true"
						 data-content="Allow students to sign up for groups (you can edit group allocation later)">
						<i class="icon-question-sign"></i>
					  </a>
					</label>
				</#if>
				<#if features.smallGroupCrossModules>
					<label class="radio">
						<@f.radiobutton path="allocationMethod" value="Linked" selector=".linked-options" />
						Linked
						<a class="use-popover" data-html="true"
						   data-content="Link these groups to a reusable set of small groups">
							<i class="icon-question-sign"></i>
						</a>
						to
						<span class="linked-options">
							<@f.select path="linkedDepartmentSmallGroupSet" id="academicYear">
								<@f.options items=departmentSmallGroupSets itemLabel="name" itemValue="id" />
							</@f.select>
						</span>
					</label>
				</#if>
				<#if features.smallGroupTeachingRandomAllocation>
					<label class="radio">
						<@f.radiobutton path="allocationMethod" value="Random" />
						Randomly allocate students to groups
						<a class="use-popover" data-html="true"
						 data-content="Students in the allocation list are randomly assigned to groups. Administrators can still assign students to groups. There may be a delay between students being added to the allocation list and being allocated to a group.">
						<i class="icon-question-sign"></i>
					  </a>
					</label>
				</#if>
			</@form.labelled_row>
		</#if>

		<#if features.smallGroupTeachingStudentSignUp>
				<@form.row defaultClass="">
					<@form.field>
						<@form.label checkbox=true>
							<@f.checkbox path="studentsCanSeeTutorName" id="studentsCanSeeTutorName" />
								Students can see tutor name
								<a class="use-popover" data-html="true"
												 data-content="Students can see tutor names when deciding which group to sign up for">
												<i class="icon-question-sign"></i>
								</a>
						</@form.label>
						<@f.errors path="studentsCanSeeTutorName" cssClass="error" />
					</@form.field>
				</@form.row>
		</#if>

		<#if features.smallGroupTeachingStudentSignUp>
				<@form.row defaultClass="">
					<@form.field>
						<@form.label checkbox=true>
							<@f.checkbox path="studentsCanSeeOtherMembers" id="studentsCanSeeOtherMembers" />
								Students can see student names
								<a class="use-popover" data-html="true"
												 data-content="Students can see the names of any other students in the group when deciding which group to sign up for">
												<i class="icon-question-sign"></i>
								</a>
						</@form.label>
						<@f.errors path="studentsCanSeeOtherMembers" cssClass="error" />
					</@form.field>
				</@form.row>
		</#if>

		<#if features.smallGroupTeachingSelfGroupSwitching>
			<@form.row path="allowSelfGroupSwitching" cssClass="student-sign-up-options">
				<@form.field>
					<@form.label checkbox=true >
						<@f.checkbox path="allowSelfGroupSwitching" />
						Allow students to switch groups
						<a class="use-popover" data-html="true"
							data-content="When self sign up is enabled students will be able to switch groups.">
								<i class="icon-question-sign"></i>
						</a>
					</@form.label>
				</@form.field>
			</@form.row>
		</#if>

		<#if features.smallGroupTeachingLectures>
			<@form.row path="collectAttendance">
				<@form.field>
					<@form.label checkbox=true>
						<@f.checkbox path="collectAttendance" />
						Collect attendance at events
					</@form.label>
				</@form.field>
			</@form.row>
		</#if>
	</fieldset>

	<script type="text/javascript">
		jQuery(function($) {
			// Set up radios to enable/disable self-sign up options fields.
			$("input:radio[name='allocationMethod']").radioControlled();
		});
	</script>
</#escape>
