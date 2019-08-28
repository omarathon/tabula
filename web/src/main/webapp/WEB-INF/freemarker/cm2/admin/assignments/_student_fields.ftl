<#escape x as x?html>
<#-- Field to support redirection post-submit -->
  <input type="hidden" name="action" value="submit" id="action-submit">
  <div class="control-group">
    <label class="control-label">Add students from SITS</label>
    <div class="control">
      <p>
        Add students by linking this assignment to one or more of the following assessment components in SITS for ${command.module.code?upper_case}
        in ${command.academicYear.label}.
      </p>
    </div>
  </div>

    <#if assignment.resitAssessment>
      <#assign sasPopoverText>
        The field in SITS called <code>Initial SAS status</code> on <code>Student Assessment</code> should be set to <code>R</code> if re-assessment is required. The status is set when you agree an assessment in SITS. Please seek advice from the SIS team if the values you see are different to what you expect to see.
      </#assign>
      <#assign sraPopoverText>
        Marks for this assignment will be uploaded to <code>Student re-assessment (SRA)</code> in SITS.
        Records are generated automatically from the Student Re-assessment (RAS) process.
        Marks from <code>SRA</code> will appear as resit marks on exam grids.
      </#assign>
      <div class="alert alert-info">
        <strong>This is a resit assessment</strong>
        <ul>
          <li>Only students that are expected to resit will be included. If students are missing from the assessment components below please check that they are marked as expected to resit in SITS <@fmt.help_popover id="sasPopover" content="${sasPopoverText}" html=true />)</li>
          <li>When uploading marks to SITS, these will be uploaded as resit marks. <@fmt.help_popover id="sraPopover" content="${sraPopoverText}" html=true /></li>
        </ul>
      </div>
    </#if>

  <#import "../assignment_membership_picker_macros.ftl" as membership_picker />

  <div class="assignmentEnrolment">
    <@membership_picker.coursework_sits_groups command />
    <div class="manualList">
      <@bs3form.labelled_form_group path="massAddUsers" labelText="Manually add students">
        <div class="help-block">Type or paste a list of usercodes or University IDs separated by white space (either a new line or a single space).</div>
        <textarea name="massAddUsers" rows="3" class="form-control">${command.originalMassAddUsers!""}</textarea>
      </@bs3form.labelled_form_group>
      <a class="btn btn-primary spinnable spinner-auto add-students-manually" data-url="<@routes.cm2.enrolment command.assignment />">Add</a>
    </div>
    <div class="pending-data-info hide alert alert-info"><i class="fa fa-info-sign fa fa-exclamation-triangle"></i> Your changes will not be recorded until you save this assignment.</div>
    <div class="assignmentEnrolmentInfo">
      <details id="students-details">
        <summary id="students-summary" class="collapsible large-chevron">
          <span class="legend" id="student-summary-legend">Students <small>Select which students should be in this assignment</small> </span>
          <@membership_picker.header command />
        </summary>
        <#assign enrolment_url><@routes.cm2.enrolment command.assignment /></#assign>
        <@membership_picker.fieldset command enrolment_url />
      </details>
    </div>

    <#if features.anonymousMarkingCM2>
      <@bs3form.labelled_form_group path="anonymousMarking" labelText="Set anonymity">
        <@bs3form.radio>
          <@f.radiobutton path="anonymousMarking" value="false" /> Off <span class="very-subtle">- markers can see University IDs and names</span>
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="anonymousMarking" value="true" /> On <span class="very-subtle">- markers cannot see University IDs and names</span>
        </@bs3form.radio>
      </@bs3form.labelled_form_group>
    </#if>
  </div>
</#escape>
