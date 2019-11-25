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
    <span class="assignment-picker-input" data-target="<@routes.cm2.assignemnts_json module/>">
		<@bs3form.labelled_form_group path="prefillAssignment" labelText="Copy assignment options (optional)">
      <input id="prefillAssignment" name="prefillAssignment" type="hidden" value="" />
      <input name="query" type="text" class="form-control" value="${pHolder!''}" />
    </@bs3form.labelled_form_group>
	</span>
    <p>You can copy details from a previous assignment within your department.
      Start typing the assignment's name to see matches and click the name to choose it.</p>
  </#if>

  <@bs3form.labelled_form_group path="openDate" labelText="Open date">
    <div class="input-group">
      <@f.input type="text" path="openDate" cssClass="form-control date-picker" placeholder="Pick the date" />
      <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
    </div>
    <div class="help-block">
      The assignment will open at 9am on this date.
    </div>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="openEnded" labelText="" renderErrors=false>
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
          <@f.input type="text" path="openEndedReminderDate" cssClass="disabled form-control date-picker" placeholder="Pick the date" />
          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
        <div class="help-block">
          The reminder will be sent at 9am.
        </div>
      </@bs3form.labelled_form_group>
    </fieldset>
  </#if>
  <fieldset id="close-dt" <#if openEnd == 'true'>disabled</#if>>
    <@bs3form.labelled_form_group path="closeDate" labelText="Closing date">
      <div class="input-group">
        <@f.input type="text" path="closeDate" cssClass="form-control date-picker" placeholder="Pick the date" />
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
      </div>
      <div class="help-block">
        The assignment will close at 12 noon on this date.
        <#if showIntro("assignment-openclose-date-restrictions", "anywhere")>
          <#assign introText>
            <p>University regulation 36 requires all assignments to close at 12 noon (mid-day) on a University working day.</p>

            <p>Assignments open at 9am on the open date, which must also be a University working day.
              This makes it more likely that the first submission attempts will be made while staff members are available to deal with any issues.</p>

            <p>Read <a href="https://warwick.ac.uk/services/its/servicessupport/web/tabula/forum?topic=8a1785d86dde8017016e26acf3e75bab" target="_blank">this Tabula noticeboard post</a> for more information</p>
          </#assign>
          <a href="#"
             id="assignment-openclose-date-restrictions"
             class="use-introductory<#if showIntro("assignment-openclose-date-restrictions", "anywhere")> auto</#if>"
             data-hash="${introHash("assignment-openclose-date-restrictions", "anywhere")}"
             data-title="Open and close date restrictions"
             data-placement="top"
             data-html="true"
             aria-label="Help"
             data-content="${introText}"><i class="fa fa-question-circle"></i></a>
        </#if>
      </div>
    </@bs3form.labelled_form_group>
  </fieldset>

  <@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
    <@spring.bind path="academicYear">
      <p class="form-control-static">${status.actualValue.label}<#if !newRecord> <span class="very-subtle">(can't be changed)</span></#if></p>
    </@spring.bind>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="resitAssessment" labelText="" renderErrors=false>
      <@bs3form.checkbox path="resitAssessment">
          <@f.checkbox path="resitAssessment" id="resitAssessment" /> Resit assignment
          <#assign popoverText>
            <p>
              Check this box to mark the assignment as a resit.
            </p>
            <ul>
              <li>Only students that are expected to resit will be added to this assignment.</li>
              <li>When uploading marks to SITS, these will be uploaded as resit marks.</li>
            </ul>
          </#assign>
          <@fmt.help_popover id="isResitInfo" content="${popoverText}" html=true />
      </@bs3form.checkbox>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="workflowCategory" labelText="Marking workflow use">
    <#if canEditWorkflowType>
      <@f.select path="workflowCategory" id="workflowCategory" class="form-control">
        <@f.options items=command.workflowCategories itemLabel="displayName" itemValue="code" />
      </@f.select>
      <div class="help-block">
        A marking workflow defines the marking method and who the markers are. You can reuse an existing workflow, create a single use workflow or choose not to
        have one.
        <span class="workflow-fields single-use-workflow-fields">
					Single use workflows are only used once and aren't saved in Tabula. To create a reusable workflow, go to <a
                  href="<@routes.cm2.reusableWorkflowsHome department academicYear />">marking workflows</a>.
				</span>
      </div>
    <#else>
      <select id="workflowCategory" name="workflowCategory" class="form-control" disabled="disabled">
        <option selected="selected" value="${command.workflowCategory.code}">${command.workflowCategory.displayName}</option>
      </select>
      <input type="hidden" name="workflowCategory" value="${command.workflowCategory.code}" />
      <div class="help-block">
        You cannot change or remove the marking workflow used on this assignment once marking has started. If you need to make changes then stop any current
        marking first.
      </div>
    </#if>
  </@bs3form.labelled_form_group>


</#escape>
