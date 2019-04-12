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

<@identity student />

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <legend>1. What kind of mitigating circumstances are you presenting?</legend>

  <p class="mitcircs-form__fields__section__hint">(Tick all that apply, but remember that you'll need to tell us something about each item you tick,
    and upload some supporting evidence for each item.)</p>

    <#list issueTypes as type>
      <div class="checkbox <#if type.entryName == "Other">mitcircs-form__fields__checkbox-with-other</#if>">
        <label>
          <@f.checkbox path="issueTypes" value="${type.entryName}" /> ${type.description}
          <#if type.entryName == "Other">
            <@f.input path="issueTypeDetails" cssClass="form-control" />
          </#if>
        </label>
      </div>
      <#if type.entryName == "Other"><@bs3form.errors path="issueTypeDetails" /></#if>
    </#list>

  <script type="text/javascript">
    (function ($) {
      $('select[name="issueType"]').on('input change', function () {
        $('.issueTypeDetails').toggle($(this).val() === "Other");
      }).trigger('change');
    })(jQuery);
  </script>
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

  <script type="text/javascript">
    (function ($) {
      $('input[name="noEndDate"]').on('input change', function () {
        $('input[name="endDate"]').prop('disabled', $(this).is(':checked'));
      }).trigger('change');
    })(jQuery);
  </script>
</fieldset>

<fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed">
  <@bs3form.labelled_form_group "reason" "Details">
    <@f.textarea path="reason" cssClass="form-control" rows="5" />
    <div class="help-block">Please provide further details of the mitigating circumstances and how they have affected your assessments</div>
  </@bs3form.labelled_form_group>
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
      <script>
        jQuery(function ($) {
          $(".remove-attachment").on("click", function (e) {
            e.preventDefault();
            const $attachmentContainer = $(this).closest("li.attachment");
            $(this).closest(".modal").on("hidden.bs.modal", function (e) {
              $attachmentContainer.remove();
            });
          });
        });
      </script>
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