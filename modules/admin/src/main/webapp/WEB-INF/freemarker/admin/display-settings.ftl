<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
	<@fmt.deptheader "Settings" "for" department routes "displaysettings" "" />

<@f.form method="post" class="form-horizontal department-settings-form" action="" commandName="displaySettingsCommand">
	<@form.row>
		<@form.label>Week numbering system</@form.label>
		<@form.field>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="term" />
				Count weeks from 1-10 for each term (the first week of the Spring term is Term 2, week 1)
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="cumulative" />
				Count term weeks cumulatively (the first week of the Spring term is Term 2, week 11)
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="academic" />
				Use academic week numbers, including vacations (the first week of the Spring term is week 15)
			</@form.label>
			<@form.label checkbox=true>
				<@f.radiobutton path="weekNumberingSystem" value="none" />
				Use no week numbers, displaying dates instead
			</@form.label>
			<@f.errors path="weekNumberingSystem" cssClass="error" />
		</@form.field>
	</@form.row>

	<hr />

	<fieldset id="assignment-options">
    	<h2>Assignment options</h2>

    	<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="showStudentName" id="showStudentName" />
					Show student name with submission
				</@form.label>
				<@f.errors path="showStudentName" cssClass="error" />
				<div class="help-block">
					If this option is enabled, all assignments in this department will display the student's name in place of their university ID.
				</div>
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label>Assignment detail view</@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.radiobutton path="assignmentInfoView" value="default" />
					Let Tabula choose the best view to display for submissions and feedback
				</@form.label>
				<@form.label checkbox=true>
					<@f.radiobutton path="assignmentInfoView" value="table" />
					Show the expanded table view of submissions and feedback first
				</@form.label>
				<@form.label checkbox=true>
					<@f.radiobutton path="assignmentInfoView" value="summary" />
					Show the summary view of submissions and feedback first
				</@form.label>
				<@f.errors path="assignmentInfoView" cssClass="error" />
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label>Validate feedback grades</@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.radiobutton path="assignmentGradeValidation" value="true" />
					Validate grade
					<@fmt.help_popover id="assignmentGradeValidationTrue" content="The 'Grade' text box will be removed and replaced by a drop-down of valid grades based on the marks scheme defined in SITS for the assessment component. Empty grades will be calculated automatically when uploaded to SITS"/>
				</@form.label>
				<@form.label checkbox=true>
					<@f.radiobutton path="assignmentGradeValidation" value="false" />
					Free-form grades
					<@fmt.help_popover id="assignmentGradeValidationFalse" content="Any text can be entered for the grade. Note that an invalid grade will prevent the feedback being uploaded to SITS (if and when this is requested)."/>
				</@form.label>
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="plagiarismDetection" id="plagiarismDetection" />
					Enable Turnitin plagiarism detection of assignment submissions
				</@form.label>
				<@f.errors path="plagiarismDetection" cssClass="error" />
				<div class="help-block">
					If you turn this option off, it won't be possible to submit any assignment submissions in this department to Turnitin.
				</div>
			</@form.field>
		</@form.row>

    </fieldset>

    <hr />

    <fieldset id="turnitin-options">
		<h2>Turnitin options</h2>
		<p>The following filters will be applied to all Turnitin assignments generated by Tabula</p>
		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="turnitinExcludeBibliography" id="turnitinExcludeBibliography" />
					Exclude bibliographic materials
				</@form.label>
				<@f.errors path="turnitinExcludeBibliography" cssClass="error" />
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="turnitinExcludeQuotations" id="turnitinExcludeQuotations" />
					Exclude quoted materials
				</@form.label>
				<@f.errors path="turnitinExcludeQuotations" cssClass="error" />
			</@form.field>
		</@form.row>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="turnitinExcludeSmallMatches" id="turnitinExcludeSmallMatches" />
					Exclude small matches
				</@form.label>
				<@f.errors path="turnitinExcludeSmallMatches" cssClass="error" />
			</@form.field>
		</@form.row>

		<fieldset id="small-match-options" class="internal">
			<#assign checkedMarkup><#if displaySettingsCommand.turnitinSmallMatchWordLimit!=0>checked="checked"</#if></#assign>
			<@form.row>
				<@form.label path="turnitinSmallMatchWordLimit"><input type="radio" name="disable-radio" ${checkedMarkup}/> Word limit</@form.label>
				<@form.field>
					<@f.errors path="turnitinSmallMatchWordLimit" cssClass="error" />
					<@f.input path="turnitinSmallMatchWordLimit" cssClass="input-small" /> words
				</@form.field>
			</@form.row>
			<#assign checkedMarkup><#if displaySettingsCommand.turnitinSmallMatchPercentageLimit!=0>checked="checked"</#if></#assign>
			<@form.row>
				<@form.label path="turnitinSmallMatchPercentageLimit"><input type="radio" name="disable-radio" ${checkedMarkup}/> Percentage limit</@form.label>
				<@form.field>
					<@f.errors path="turnitinSmallMatchPercentageLimit" cssClass="error" />
					<@f.input path="turnitinSmallMatchPercentageLimit" cssClass="input-small" /> %
				</@form.field>
			</@form.row>
		</fieldset>
	</fieldset>

	<hr />

	<fieldset id="small-groups-options">
		<h2>Small group options</h2>
	    <#if features.smallGroupTeachingStudentSignUp>
			<@form.row>
				<@form.label>Default allocation method for small groups</@form.label>
				<@form.field>
					<@form.label checkbox=true>
						<@f.radiobutton path="defaultGroupAllocationMethod" value="Manual" />
						Manual Allocation
					</@form.label>
					<@form.label checkbox=true>
						<@f.radiobutton path="defaultGroupAllocationMethod" value="StudentSignUp" />
						Self Sign-up
					</@form.label>
					<@f.errors path="defaultGroupAllocationMethod" cssClass="error" />
				</@form.field>
			</@form.row>
	    </#if>

    	<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<@form.label checkbox=true>
					<@f.checkbox path="autoGroupDeregistration" id="autoGroupDeregistration" />
					Automatically deregister students from groups
				</@form.label>
				<@f.errors path="autoGroupDeregistration" cssClass="error" />
				<div class="help-block">
					Students will be removed from a group when they deregister from the associated module, or are manually removed from the set of groups.
				</div>
			</@form.field>
		</@form.row>

	</fieldset>

	<#if features.arbitraryRelationships>
		<hr />

		<fieldset id="relationship-options">
			<h2>Student relationship options</h2>

			<@form.row>
				<@form.label>Display</@form.label>
				<@form.field>
					<#list allRelationshipTypes as relationshipType>
						<div class="studentRelationshipDisplayed">
							<@form.label checkbox=true>
								<@f.checkbox path="studentRelationshipDisplayed[${relationshipType.id}]" id="studentRelationshipDisplayed_${relationshipType.id}" />
								${relationshipType.description}
							</@form.label>
							<div class="studentRelationshipExpected" style="margin-left: 32px;">
								<div class="help-block">
									<small>Show when empty to students of type</small>
								</div>
								<#list expectedCourseTypes as courseType>
									<@form.label checkbox=true>
										<@f.checkbox
											path="studentRelationshipExpected[${relationshipType.urlPart}][${courseType.code}]"
											id="studentRelationshipExpected_${relationshipType.urlPart}_${courseType.code}"
											disabled=(!displaySettingsCommand.studentRelationshipDisplayed[relationshipType.id])
										/>
										${courseType.description}
									</@form.label>
								</#list>
							</div>
						</div>
					</#list>

					<@f.errors path="studentRelationshipDisplayed" cssClass="error" />
				</@form.field>
				<script>
					jQuery(function($){
						$('#relationship-options input[name^=studentRelationshipDisplayed]').on('change', function(){
							var $this = $(this);
							var disableInput = !$this.is(':checked');
							$this.closest('.studentRelationshipDisplayed').find('.studentRelationshipExpected input').prop('disabled', disableInput);
						});
					});
				</script>
			</@form.row>

			<@form.row>
				<@form.label></@form.label>
				<@form.field>
					<@form.label checkbox=true>
						<@f.checkbox path="studentsCanScheduleMeetings" id="studentsCanScheduleMeetings" />
						Students can schedule meetings
					</@form.label>
					<@f.errors path="studentsCanScheduleMeetings" cssClass="error" />
					<div class="help-block">
						If unchecked, students won't be able to schedule meetings with a tutor, supervisor or other relationship agent in this department.
					</div>
				</@form.field>
			</@form.row>
		</fieldset>
	</#if>

	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<#if (returnTo!"")?length gt 0>
			<#assign cancelDestination=returnTo />
		<#else>
			<#assign cancelDestination><@routes.departmenthome department=department /></#assign>
		</#if>
		<a class="btn" href="${cancelDestination}">Cancel</a>
	</div>
</@f.form>
</#escape>