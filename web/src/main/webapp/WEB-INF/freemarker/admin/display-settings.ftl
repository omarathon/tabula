<#escape x as x?html>
  <#function route_function dept>
    <#local result><@routes.admin.displaysettings dept /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Settings" route_function "for" />

  <div class="fix-area">
    <#if department.hasChildren>
      <div class="alert alert-info">
        <i class="fa fa-info-circle"></i> Department settings do not cascade to sub-departments.
        To change settings for a sub-department, select it from the drop-down list above.
      </div>
    </#if>

    <#assign submitUrl><@routes.admin.displaysettings department /></#assign>
    <@f.form method="post" class="department-settings-form" action=submitUrl modelAttribute="displaySettingsCommand">
      <input type="hidden" name="returnTo" value="${returnTo}">

      <@bs3form.labelled_form_group labelText="Week numbering system">
        <@bs3form.radio>
          <@f.radiobutton path="weekNumberingSystem" value="term" />
          Count weeks from 1-10 for each term (the first week of the Spring term is Term 2, week 1)
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="weekNumberingSystem" value="cumulative" />
          Count term weeks cumulatively (the first week of the Spring term is Term 2, week 11)
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="weekNumberingSystem" value="academic" />
          Use academic week numbers, including vacations (the first week of the Spring term is week 15)
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="weekNumberingSystem" value="none" />
          Use dates instead of week numbers
        </@bs3form.radio>
        <@bs3form.errors path="weekNumberingSystem" />
      </@bs3form.labelled_form_group>

      <hr />

      <h2>Assignment options</h2>

      <@bs3form.labelled_form_group>
        <@bs3form.checkbox path="showStudentName">
          <@f.checkbox path="showStudentName" id="showStudentName" />
          Show student name with submission
        </@bs3form.checkbox>
        <div class="help-block">
          When this option is selected, markers and moderators see students' names and University IDs with submissions by default. You can override this setting
          at the assignment level.
        </div>
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group labelText="Assignment detail view">
        <@bs3form.radio>
          <@f.radiobutton path="assignmentInfoView" value="default" />
          Let Tabula choose the best view to display for submissions and feedback
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="assignmentInfoView" value="table" />
          Show the expanded table view of submissions and feedback first
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="assignmentInfoView" value="summary" />
          Show the summary view of submissions and feedback first
        </@bs3form.radio>
        <@bs3form.errors path="assignmentInfoView" />
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group labelText="Validate feedback grades">
        <@bs3form.radio>
          <@f.radiobutton path="assignmentGradeValidation" value="true" />
          Validate grade
          <@fmt.help_popover id="assignmentGradeValidationTrue" content="The 'Grade' text box will be replaced by a drop-down list of valid grades based on the marks scheme defined in SITS for the assessment component. Empty grades are calculated automatically when they are uploaded to SITS."/>
        </@bs3form.radio>
        <@bs3form.radio>
          <@f.radiobutton path="assignmentGradeValidation" value="false" />
          Free-form grades
          <@fmt.help_popover id="assignmentGradeValidationFalse" content="Any text can be entered for the grade. Note that an invalid grade prevents the feedback being uploaded to SITS (if and when this is requested)."/>
        </@bs3form.radio>
      </@bs3form.labelled_form_group>

      <@bs3form.checkbox path="plagiarismDetection">
        <@f.checkbox path="plagiarismDetection" id="plagiarismDetection" />
        Enable Turnitin plagiarism detection of assignment submissions
        <div class="help-block">
          If you turn this option off, it will not be possible to submit any assignment submissions in this department to Turnitin.
        </div>
      </@bs3form.checkbox>

      <hr />

      <fieldset id="small-groups-options">
        <h2>Small group options</h2>
        <#if features.smallGroupTeachingStudentSignUp>
          <@bs3form.labelled_form_group path="defaultGroupAllocationMethod" labelText="Default allocation method for small groups">
            <@bs3form.radio>
              <@f.radiobutton path="defaultGroupAllocationMethod" value="Manual" />
              Manual allocation
            </@bs3form.radio>
            <@bs3form.radio>
              <@f.radiobutton path="defaultGroupAllocationMethod" value="StudentSignUp" />
              Self sign-up
            </@bs3form.radio>
          </@bs3form.labelled_form_group>
        </#if>

        <@bs3form.checkbox path="autoGroupDeregistration">
          <@f.checkbox path="autoGroupDeregistration" id="autoGroupDeregistration" />
          Automatically deregister students from groups
          <div class="help-block">
            Students will be removed from a group when they deregister from the associated module, or are manually removed from the set of groups.
          </div>
        </@bs3form.checkbox>

      </fieldset>

      <#if features.autoMarkMissedMonitoringPoints>
        <fieldset id="monitoring-points-options">
          <h2>Monitoring point options</h2>
          <@bs3form.checkbox path="autoMarkMissedMonitoringPoints">
            <@f.checkbox path="autoMarkMissedMonitoringPoints" id="autoMarkMissedMonitoringPoints" />
            Automatically mark small group teaching monitoring points as missed
            <div class="help-block">
              If this option is selected, small group teaching monitoring points will be marked as missed when all applicable events are marked as missed for
              that student.
            </div>
          </@bs3form.checkbox>
        </fieldset>
      </#if>

      <#if features.arbitraryRelationships>
        <hr />

        <fieldset id="relationship-options">
          <h2>Student relationship options</h2>

          <@bs3form.labelled_form_group labelText="Display">
          <#list allRelationshipTypes as relationshipType>
            <div class="studentRelationshipDisplayed">
              <@bs3form.checkbox path="studentRelationshipDisplayed[${relationshipType.id}]">
                <@f.checkbox path="studentRelationshipDisplayed[${relationshipType.id}]" id="studentRelationshipDisplayed_${relationshipType.id}" />
                ${relationshipType.description}
              </@bs3form.checkbox>
              <div class="studentRelationshipExpected" style="margin-left: 32px;">
                <div class="help-block">
                  <small>Show when empty to students of type</small>
                </div>
                <#list expectedCourseTypes as courseType>
                  <@bs3form.checkbox path="studentRelationshipExpected[${relationshipType.urlPart}][${courseType.code}]">
                    <@f.checkbox
                    path="studentRelationshipExpected[${relationshipType.urlPart}][${courseType.code}]"
                    id="studentRelationshipExpected_${relationshipType.urlPart}_${courseType.code}"
                    disabled=(!displaySettingsCommand.studentRelationshipDisplayed[relationshipType.id])
                    />
                    ${courseType.description}
                  </@bs3form.checkbox>
                </#list>
              </div>
            </div>
          </#list>

            <@bs3form.errors path="studentRelationshipDisplayed" />
            <script>
              jQuery(function ($) {
                $('#relationship-options').find('input[name^=studentRelationshipDisplayed]').on('change', function () {
                  var $this = $(this);
                  var disableInput = !$this.is(':checked'), $container = $this.closest('.studentRelationshipDisplayed').find('.studentRelationshipExpected');
                  $container.find('input').prop('disabled', disableInput);
                  if (disableInput) {
                    $container.hide();
                  } else {
                    $container.show();
                  }
                }).each(function () {
                  if (!$(this).is(':checked')) {
                    $(this).closest('.studentRelationshipDisplayed').find('.studentRelationshipExpected').hide();
                  }
                });

              });
            </script>
          </@bs3form.labelled_form_group>

          <@bs3form.checkbox path="studentsCanScheduleMeetings">
            <@f.checkbox path="studentsCanScheduleMeetings" id="studentsCanScheduleMeetings" />
            Students can schedule meetings
            <div class="help-block">
              If this option is not selected, students will not be able to schedule meetings with a tutor, supervisor or other relationship agent in this
              department.
            </div>
          </@bs3form.checkbox>

          <@bs3form.labelled_form_group labelText="Meeting record approval">
            <div class="help-block">
              Meeting records with multiple tutors/supervisors require approval from:
            </div>
            <@bs3form.radio>
              <@f.radiobutton path="meetingRecordApprovalType" value="one" />
              Any of the approvers
            </@bs3form.radio>
            <@bs3form.radio>
              <@f.radiobutton path="meetingRecordApprovalType" value="all" />
              All of the approvers
            </@bs3form.radio>
          </@bs3form.labelled_form_group>
        </fieldset>
      </#if>

      <#if features.mitCircs>
        <fieldset id="mitcircs-options">
          <h2>Mitigating circumstances options</h2>

          <@bs3form.checkbox path="enableMitCircs">
            <@f.checkbox path="enableMitCircs" id="enableMitCircs" />
            Enable mitigating circumstances
          </@bs3form.checkbox>

          <div class="row">
            <div class="col-md-8">
              <@bs3form.labelled_form_group "mitCircsGuidance" "Mitigating circumstances guidance">
                <@f.textarea path="mitCircsGuidance" cssClass="form-control" rows="5" data\-preview="#mitCircsGuidance-preview" />

                <div class="help-block">
                  <p>
                    Provide your department's mitigating circumstances guidelines, which will be shown alongside the form
                    to make a mitigating circumstances declaration. You can make a new paragraph by leaving a blank line
                    (i.e. press Enter twice).
                  </p>

                  <p>
                    You might want to include the following in your departmental guidance:
                  </p>

                  <ul>
                    <li>When the mitigating circumstances panel meets</li>
                    <li>Who will be on the mitigating circumstances panel (i.e. to let the student know who will see their submission and evidence)</li>
                    <li>When the deadlines are for submitting mitigating circumstances claims</li>
                    <li>Who they can talk to to get help with creating and submitting their mitigating circumstances claim</li>
                  </ul>

                  <p>
                    You can use simple syntax to format the guidance, such as using <code>**asterisks**</code> around words to make them
                    <strong>bold</strong>, <code>_underscores_</code> to make them <em>italic</em>. To make a link, use syntax like
                    <code style="white-space: nowrap;">[text to link](https://warwick.ac.uk/your/link)</code>.
                  </p>
                </div>
              </@bs3form.labelled_form_group>
            </div>

            <div class="col-md-4">
              <div id="mitCircsGuidance-preview" class="well" style="display: none;">
              </div>
            </div>
          </div>
        </fieldset>
      </#if>

      <div class="fix-footer">
        <input type="submit" value="Save" class="btn btn-primary">
        <#if (returnTo!"")?length gt 0>
          <#assign cancelDestination=returnTo />
        <#else>
          <#assign cancelDestination><@routes.admin.departmenthome department=department /></#assign>
        </#if>
        <a class="btn btn-default" href="${cancelDestination}">Cancel</a>
      </div>

    </@f.form>
  </div>

  <script>
    jQuery(function ($) {
      $('.fix-area').fixHeaderFooter();
    })
  </script>
</#escape>
