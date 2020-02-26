<@fmt.moduleHeader "Create a new exam" module "for" />
<@f.form method="post" class="exam-form fix-area" modelAttribute="command">

  <#if availableOccurrences?size == 1>
      <input type="hidden" name="occurrence" value="${availableOccurrences[0]}" />
  <#else>
    <@bs3form.labelled_form_group path="occurrences" labelText="Occurrences">
      <div class="checkbox">
        <#list availableOccurrences as occurrence>
          <@bs3form.checkbox inline=true>
            <input type="checkbox" name="occurrences" value="${occurrence}" <#if command.occurrences?seq_contains(occurrence)>checked="checked"</#if>> ${occurrence}
          </@bs3form.checkbox>
        </#list>
      </div>
      <div class="help-block">
        There is more than one occurrence of this module in ${academicYear.toString}. Please select the occurrences that this exam will relate to.
      </div>
    </@bs3form.labelled_form_group>
  </#if>

  <!-- TODO - no tables in this glorious new decade -->
  <@bs3form.labelled_form_group path="assessmentComponents" labelText="Assessment Components">
    <div class="checkbox component-filters">
      <@bs3form.checkbox inline=true>
        <@f.checkbox path="showNotInUse" /> Show not-in-use components
      </@bs3form.checkbox>
      <@bs3form.checkbox inline=true>
        <@f.checkbox path="showNonExam" /> Show non-exam components
      </@bs3form.checkbox>
    </div>
    <table id="sits-table" class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
      <thead>
        <tr>
          <th></th>
          <th>Sequence</th>
          <th>Name</th>
          <th>Assessment group</th>
          <th>Assessment Type</th>
          <th>Paper code</th>
          <th>Paper name</th>
          <th>Students</th>
        </tr>
      </thead>
      <tbody>
        <#list availableComponents as assessmentComponent>
          <tr
            class="assessment-component<#if !assessmentComponent.inUse> not-in-use</#if><#if assessmentComponent.assessmentType.subtype.entryName != "Exam"> non-exam</#if>"
            data-students="${mapGet(students, assessmentComponent)}"
          >
            <td>
              <@f.checkbox path="assessmentComponents" value="${assessmentComponent.id}"/>
            </td>
            <td>${assessmentComponent.sequence}</td>
            <td>${assessmentComponent.name}</td>
            <td>${assessmentComponent.assessmentGroup}</td>
            <td>${assessmentComponent.assessmentType.name}</td>
            <#if assessmentComponent.examPaperCode?has_content>
              <td>${assessmentComponent.examPaperCode}</td>
              <td>${assessmentComponent.examPaperTitle!""}</td>
            <#else>
              <td colspan="2">Unknown</td>
            </#if>
            <td class="students"></td>
          </tr>
        </#list>
      </tbody>
    </table>
  </@bs3form.labelled_form_group>

  <details class="form-group enrolled-students">
    <summary>
      <#assign popoverText>
        <p>The list of enrolled students will be updated automatically if there are changes to module registrations.</p>
      </#assign>
      <label class="control-label">
        Students
      </label>

        <p class="student-total">
          <span class="count"></span> enrolled <span class="muted very-subtle">from SITS</span>
          <@fmt.help_popover id="sits-help" content="${popoverText}" html=true />
        </p>
        <div class="no-students alert alert-warning" role="alert">There are no students enrolled on this exam</div>
        <div class="duplicates alert alert-warning" role="alert">
          Some students are enrolled on this exam more than once.
          <@fmt.help_popover id="duplicate-students-help"  content="If a student is registered on more than one assessment component linked to this exam then it will not be possible to upload their marks to SITS." /></span>
        </div>

    </summary>

    <table class="students table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
      <thead>
        <tr>
          <th class="sortable">First name</th>
          <th class="sortable">Last name</th>
          <th class="sortable">ID</th>
          <th class="sortable">Usercode</th>
          <th class="sortable">Seat number</th>
        </tr>
      </thead>
      <tbody></tbody>
    </table>
  </details>

  <div class="fix-footer">
    <p>
        <button type="submit" class="btn btn-primary" name="create">Create</button>
        <a class="btn btn-default dirty-check-ignore" href="">Cancel</a>
    </p>
  </div>
</@f.form>


