<#import "*/modal_macros.ftl" as modal />

<#macro identity_info key value>
  <#if value?has_content>
    <div class="row form-horizontal">
      <div class="col-sm-3 control-label">${key}</div>
      <div class="col-sm-9">
        <div class="form-control-static">${value}</div>
      </div>
    </div>
  </#if>
</#macro>

<#macro identity student>
  <fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--identity">
    <@identity_info "Name" student.fullName />
    <@identity_info "University ID" student.universityId />
    <#if student.email??><@identity_info "Email" student.email /></#if>

    <#if student.mostSignificantCourseDetails??>
      <#local studentCourseDetails = student.mostSignificantCourseDetails />
      <@identity_info "Course" studentCourseDetails.course.name />

      <#if studentCourseDetails.latestStudentCourseYearDetails??>
        <#local studentCourseYearDetails = studentCourseDetails.latestStudentCourseYearDetails />

        <#if studentCourseYearDetails.yearOfStudy??>
          <@identity_info "Year of study" studentCourseYearDetails.yearOfStudy />
        </#if>

        <#if studentCourseYearDetails.modeOfAttendance??>
          <@identity_info "Mode of study" studentCourseYearDetails.modeOfAttendance.fullNameAliased />
        </#if>
      </#if>
    </#if>

    <#if submission??>
      <@identity_info "Reference" "MIT-${submission.key}" />
    </#if>
  </fieldset>
</#macro>

<#macro checkboxesWithOther enumValues enumField otherField>
  <#list enumValues as value>
    <div class="checkbox <#if value.entryName == "Other">mitcircs-form__fields__checkbox-with-other</#if>">
      <label>
        <@f.checkbox path="${enumField}" value="${value.entryName}" /> ${value.description}
        <#if value.entryName == "Other">
          <@f.input path="${otherField}" cssClass="form-control" />
        </#if>
      </label>
    </div>
    <#if value.entryName == "Other"><@bs3form.errors path="${otherField}" /></#if>
  </#list>
  <@bs3form.errors path="${enumField}" />
</#macro>

<@identity student />

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <legend>1. What kind of mitigating circumstances are you presenting?</legend>

  <p class="mitcircs-form__fields__section__hint">(Tick all that apply, but remember that you'll need to tell us something about each item you tick,
    and upload some supporting evidence for each item.)</p>

  <@checkboxesWithOther issueTypes "issueTypes" "issueTypeDetails" />
</fieldset>

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed form-horizontal">
  <legend>2. What period do your mitigating circumstances cover?</legend>

  <p class="mitcircs-form__fields__section__hint">(If you're claiming for a period in the past, include a start and end date. If you're claiming for
    something that's ongoing, you may not know the end date at this point.)</p>

  <@bs3form.form_group "startDate">
    <@bs3form.label path="startDate" cssClass="col-xs-4 col-sm-2">Start date</@bs3form.label>

    <div class="col-xs-8 col-sm-4">
      <@spring.bind path="startDate">
        <div class="input-group">
          <@f.input path="startDate" cssClass="form-control date-picker" />
          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
      </@spring.bind>

      <@bs3form.errors path="startDate" />
    </div>
  </@bs3form.form_group>

  <@bs3form.form_group "endDate">
    <@bs3form.label path="endDate" cssClass="col-xs-4 col-sm-2">End date</@bs3form.label>

    <div class="col-xs-8 col-sm-4">
      <@spring.bind path="endDate">
        <div class="input-group">
          <@f.input path="endDate" cssClass="form-control date-picker" />
          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
      </@spring.bind>

      <div class="checkbox">
        <label>
          <@f.checkbox path="noEndDate" /> <span class="hint">Or,</span> <strong>ongoing</strong>
        </label>
      </div>

      <@bs3form.errors path="endDate" />
    </div>
  </@bs3form.form_group>
</fieldset>

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <legend>4. Details</legend>
  <@bs3form.form_group "reason">
    <@f.textarea path="reason" cssClass="form-control" rows="5" />
    <div class="help-block">Please provide further details of the mitigating circumstances and how they have affected your assessments</div>
  </@bs3form.form_group>
</fieldset>


<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <legend>6. Have you contacted anyone about your mitigating circumstances?</legend>
  <div class="radio">
    <@bs3form.radio_inline>
      <@f.radiobutton path="contacted" value="true" /> Yes
    </@bs3form.radio_inline>
    <@bs3form.radio_inline>
      <@f.radiobutton path="contacted" value="false" /> No
    </@bs3form.radio_inline>
  </div>
  <div class="mitcircs-form__fields__contact-subfield mitcircs-form__fields__contact-subfield--yes" style="display: none;">
    <@checkboxesWithOther possibleContacts "contacts" "contactOther" />
  </div>
  <div class="mitcircs-form__fields__contact-subfield mitcircs-form__fields__contact-subfield--no" style="display: none;">
    <@bs3form.form_group "noContactReason">
      <@f.textarea path="noContactReason" cssClass="form-control" rows="5" />
      <div class="help-block">Please tell us why you haven't yet contacted anyone about your mitigating circumstances</div>
    </@bs3form.form_group>
  </div>
  <script type="text/javascript">
    (function ($) {
      $('input[name="contacted"]').on('input change', function () {
        const val = $('input[name="contacted"]:checked').val();
        if(val) {
          $('.mitcircs-form__fields__contact-subfield--yes').toggle(val === 'true');
          $('.mitcircs-form__fields__contact-subfield--no').toggle(val !== 'true');
        }
      }).trigger('change');
    })(jQuery);
  </script>
</fieldset>

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <legend>5. Which assessments have been affected?</legend>

  <p class="mitcircs-form__fields__section__hint">(Blah blah some hint text here)</p>

  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation" class="active"><a href="#assessment-table-assignments" aria-controls="assessment-table-assignments" role="tab" data-toggle="tab">Assignments</a></li>
    <li role="presentation"><a href="#assessment-table-exams" aria-controls="assessment-table-exams" role="tab" data-toggle="tab">Exams</a></li>
  </ul>

  <table class="table table-striped table-condensed table-hover table-checkable mitcircs-form__fields__section__assessments-table tab-content" data-endpoint="<@routes.mitcircs.affectedAssessments student />">
    <colgroup>
      <col class="col-sm-1">
      <col class="col-sm-3">
      <col class="col-sm-5">
      <col class="col-sm-3">
    </colgroup>
    <thead>
      <tr>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__checkbox"></th>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__module">Module</th>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__name">Title</th>
        <th scope="col" class="mitcircs-form__fields__section__assessments-table__deadline">Deadline / exam date</th>
      </tr>
    </thead>
    <tfoot>
      <tr>
        <td><button type="button" class="btn btn-default btn-sm">Add</button></td>
        <td>
          <label class="control-label sr-only" for="new-assessment-module">Module</label>
          <select id="new-assessment-module" class="form-control input-sm">
            <option></option>
            <#list registeredModules?keys as year>
              <optgroup label="${year.toString}">
                <#list mapGet(registeredModules, year) as module>
                  <option value="${module.code}" data-name="${module.name}"><@fmt.module_name module=module withFormatting=false /></option>
                </#list>
              </optgroup>
            </#list>
          </select>
        </td>
        <td>
          <label class="control-label sr-only" for="new-assessment-name">Title</label>
          <input id="new-assessment-name" type="text" class="form-control input-sm">
        </td>
        <td>
          <label class="control-label sr-only" for="new-assessment-deadline">Deadline or examination date</label>
          <div class="input-group">
            <input id="new-assessment-deadline" type="text" class="form-control input-sm date-picker">
            <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
          </div>
        </td>
      </tr>
    </tfoot>

    <#macro affectedAssessment item index>
      <@spring.nestedPath path="affectedAssessments[${index}]">
        <tr>
          <td class="mitcircs-form__fields__section__assessments-table__checkbox">
            <@f.hidden path="moduleCode" />
            <@f.hidden path="sequence" />
            <@f.hidden path="academicYear" />
            <@f.hidden path="assessmentType" />
            <input type="checkbox" checked>
          </td>
          <td class="mitcircs-form__fields__section__assessments-table__module">
            <@fmt.module_name item.module />
          </td>
          <td class="mitcircs-form__fields__section__assessments-table__name">
            <@f.hidden path="name" />
            ${item.name}
          </td>
          <td class="mitcircs-form__fields__section__assessments-table__deadline">
            <div class="input-group">
              <@f.input path="deadline" cssClass="form-control input-sm date-picker" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </div>
          </td>
        </tr>
      </@spring.nestedPath>
    </#macro>

    <tbody id="assessment-table-assignments" role="tabpanel" class="tab-pane active">
      <#if command.affectedAssessments?has_content>
        <#list command.affectedAssessments as item>
          <#if item.assessmentType?? && item.assessmentType.code == 'A'>
            <@affectedAssessment item item_index />
          </#if>
        </#list>
      </#if>
    </tbody>
    <tbody id="assessment-table-exams" role="tabpanel" class="tab-pane">
      <#if command.affectedAssessments?has_content>
        <#list command.affectedAssessments as item>
          <#if item.assessmentType?? && item.assessmentType.code == 'E'>
            <@affectedAssessment item item_index />
          </#if>
        </#list>
      </#if>
    </tbody>
  </table>
</fieldset>

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <#if command.attachedFiles?has_content >
    <@bs3form.labelled_form_group path="attachedFiles" labelText="Supporting documentation">
      <ul class="unstyled">
        <#list command.attachedFiles as attachment>
          <#assign url></#assign>
          <li id="attachment-${attachment.id}" class="attachment">
            <i class="fa fa-file-o"></i>
            <a target="_blank" href="<@routes.mitcircs.renderAttachment submission attachment />"><#compress> ${attachment.name} </#compress></a>&nbsp;
            <@f.hidden path="attachedFiles" value="${attachment.id}" />
            <a href="" data-toggle="modal" data-target="#confirm-delete-${attachment.id}"><i class="fa fa-times-circle"></i></a>
            <div class="modal fade" id="confirm-delete-${attachment.id}" tabindex="-1" role="dialog" aria-hidden="true">
              <@modal.wrapper>
                <@modal.body>Are you sure that you want to delete ${attachment.name}?</@modal.body>
                <@modal.footer>
                  <a class="btn btn-danger remove-attachment" data-dismiss="modal">Delete</a>
                  <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                </@modal.footer>
              </@modal.wrapper>
            </div>
          </li>
        </#list>
      </ul>
      <div class="help-block">
        This is a list of all supporting documents that have been attached to this mitigating circumstances submission.
        Click the remove link next to a document to delete it.
      </div>
    </@bs3form.labelled_form_group>
  </#if>

  <@bs3form.filewidget
  basename="file"
  labelText="Upload new supporting documentation relevant to your submission"
  types=[]
  multiple=true
  required=false
  />
</fieldset>