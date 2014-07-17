<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

	<@f.form id="editGroups" method="POST" commandName="editDepartmentSmallGroupSetCommand" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right active">Properties</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddStudents}">Students</button></span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}">Groups</button></span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
		</p>

		<fieldset>
			<@form.labelled_row "name" "Set name">
				<@f.input path="name" cssClass="text" />
				<a class="use-popover" data-html="true"
				   data-content="Give this set of groups a name to distinguish it from any other sets - eg. UG Year 1 seminars and UG Year 2 seminars">
					<i class="icon-question-sign"></i>
				</a>
			</@form.labelled_row>

			<@form.labelled_row "academicYear" "Academic year">
				<@spring.bind path="academicYear">
					<span class="uneditable-value">${status.actualValue.label} <span class="hint">(can't be changed)</span></span>
				</@spring.bind>
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
		</fieldset>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddStudents}"
				value="Save and add students"
				title="Select which students are included in these groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="edit"
				value="Save and exit"
				title="Save your groups and add students and groups to it later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.crossmodulegroups smallGroupSet.department />">Cancel</a>
		</div>
	</@f.form>

	<script type="text/javascript">
		jQuery(function($) {
			// Set up radios to enable/disable self-sign up options fields.
			$("input:radio[name='allocationMethod']").radioControlled();
		});
	</script>
</#escape>