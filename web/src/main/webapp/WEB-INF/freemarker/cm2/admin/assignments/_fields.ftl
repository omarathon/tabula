<#escape x as x?html>

<#-- Field to support redirection post-submit -->
<input type="hidden" name="action" value="submit" id="action-submit">

	<@bs3form.labelled_form_group path="name" labelText="Assignment title">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>
	<#if newRecord>
		<#if command.prefillAssignment??>
			<#assign pHolder = "${command.prefillAssignment.name} - ${command.prefillAssignment.module.code}">
		</#if>
    <span class ="assignment-picker-input"  data-target="<@routes.cm2.assignemnts_json module/>">
		<@bs3form.labelled_form_group path="prefillAssignment" labelText="Copy assignment options (optional)">
            <input id="prefillAssignment" name="prefillAssignment" type="hidden" value=""/>
			<input name="query" type="text" class="form-control"  value="${pHolder!''}"/>
		</@bs3form.labelled_form_group>
	</span>
    <p>You can copy details from a previous assignment within your department.
		Start typing the assignment's name to see matches and click the name to choose it.</p>
	</#if>

	<@bs3form.labelled_form_group path="openDate" labelText="Open date">
    <div class="input-group">
		<@f.input type="text" path="openDate" cssClass="form-control date-time-minute-picker" placeholder="Pick the date" />
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
    </div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="openEnded" labelText="">
		<@bs3form.checkbox path="openEnded">
			<@f.checkbox path="openEnded" id="openEnded" /> Open-ended
			<#assign popoverText>
            <p>
                Check this box to mark the assignment as open-ended.
            </p>

            <ul>
                <li>Any close date previously entered will have no effect.</li>
                <li>Allowing extensions and submission after the close date will have no effect.</li>
                <li>No close date will be shown to students.</li>
                <li>There will be no warnings for lateness, and no automatic deductions to marks.</li>
                <li>You will be able to publish feedback individually at any time.</li>
            </ul>
			</#assign>
			<@fmt.help_popover id="openEndedInfo" content="${popoverText}" html=true/>
		</@bs3form.checkbox>
	</@bs3form.labelled_form_group>

<#assign openEnd =  command.openEnded?string('true','false') />
<#if features.openEndedReminderDateCM2>
	<fieldset id="open-reminder-dt" <#if openEnd == 'false'>disabled</#if>>
		<@bs3form.labelled_form_group path="openEndedReminderDate" labelText="Open-ended reminder date">
					<div class="input-group disabled">
				<@f.input type="text" path="openEndedReminderDate" cssClass="disabled form-control date-time-minute-picker" placeholder="Pick the date" />
							<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
					</div>
		</@bs3form.labelled_form_group>
	</fieldset>
</#if>
<fieldset id="close-dt" <#if openEnd == 'true'>disabled</#if>>
	<@bs3form.labelled_form_group path="closeDate" labelText="Closing date">
        <div class="input-group">
			<@f.input type="text" path="closeDate" cssClass="form-control date-time-minute-picker" placeholder="Pick the date" />
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
	</@bs3form.labelled_form_group>
</fieldset>

	<@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
		<@spring.bind path="academicYear">
      <p class="form-control-static">${status.actualValue.label}<#if !newRecord> <span class="very-subtle">(can't be changed)</span></#if></p>
		</@spring.bind>
	</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group path="workflowCategory" labelText="Marking workflow use">
	<#if canEditWorkflowType>
			<@f.select path="workflowCategory" id="workflowCategory" class="form-control">
				<@f.options items=command.workflowCategories itemLabel="displayName" itemValue="code" />
			</@f.select>
			<div class="help-block">
				A marking workflow defines the marking method and who the markers are. You can reuse an existing workflow, create a single use workflow or choose not to have one.
				<span class="workflow-fields single-use-workflow-fields">
					Single use workflows are only used once and aren't saved in Tabula. To create a reusable workflow, go to <a href="<@routes.cm2.reusableWorkflowsHome department academicYear />">marking workflows</a>.
				</span>
			</div>
	<#else>
		<select id="workflowCategory" name="workflowCategory" class="form-control" disabled="disabled">
			<option selected="selected" value="${command.workflowCategory.code}">${command.workflowCategory.displayName}</option>
		</select>
		<input type="hidden" name="workflowCategory" value="${command.workflowCategory.code}" />
		<div class="help-block">
			You cannot change or remove the marking workflow used on this assignment once marking has started. If you need to make changes then stop any current marking first.
		</div>
	</#if>
</@bs3form.labelled_form_group>


</#escape>
