<#--
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines.
-->
<#escape x as x?html>
<#import "../cm2_macros.ftl" as cm2_macros />
<#-- Field to support redirection post-submit -->
<input type="hidden" name="action" value="submit" id="action-submit">
		<div class="control-group">
			<label class="control-label">Add students from SITS:</label>
			<div class="control">
					Add students by linking this assignment to one or more of the following assessment components in SITS for ${command.module.code?upper_case} in ${command.academicYear.label}
			</div>
		</div>

	<#import "../assignment_membership_picker_macros.ftl" as membership_picker />

	<div class="assignmentEnrolment">
		<@cm2_macros.coursework_sits_groups command />
		<div class="manualList">
			<@bs3form.labelled_form_group path="massAddUsers" labelText="Manually add students:">
				<div class="help-block">Type or paste in a list of usercodes or University numbers here, separated by white space and then click Add. </div>
				<textarea name="massAddUsers" rows="3" class="form-control">${command.originalMassAddUsers!""}</textarea>
			</@bs3form.labelled_form_group>
			<a class="btn btn-primary spinnable spinner-auto add-students-manually" data-url="<@routes.cm2.enrolment command.assignment />">Add</a>
		</div>
		<div class="assignmentEnrolmentInfo">
			<@form.row "members" "assignmentEnrolment">
				<details id="students-details">
					<summary id="students-summary" class="collapsible large-chevron">
						<span class="legend" id="student-summary-legend">Students <small>Select which students should be in this assignment</small> </span>
						<div class="alert alert-success" style="display: none;" data-display="fragment">
							The membership list for this assignment has been updated
						</div>
						<@membership_picker.header command />
					</summary>
					<#assign enrolment_url><@routes.cm2.enrolment command.assignment /></#assign>
					<@membership_picker.fieldset command enrolment_url />
				</details>
			</@form.row>
		</div>
		<@bs3form.labelled_form_group path="studentInfoAnnonymous" labelText="Set annonymity">
			<div class="help-block">If set to 'on', markers won't be able to set student ID or name.</div>
			<@bs3form.radio>
				<@f.radiobutton path="studentInfoAnnonymous" value="true" /> On
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="studentInfoAnnonymous" value="false" /> Off
			</@bs3form.radio>
		</@bs3form.labelled_form_group>
	</div>
</#escape>

