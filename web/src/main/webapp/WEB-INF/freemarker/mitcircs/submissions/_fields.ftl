<#import "*/modal_macros.ftl" as modal />

<div class="row">
  <div class="col col-md-6">
    <@bs3form.labelled_form_group labelText="University ID">
      <p class="very-subtle">${student.universityId}</p>
    </@bs3form.labelled_form_group>
  </div>
  <div class="col col-md-6">
    <#if submission??>
      <@bs3form.labelled_form_group labelText="Reference">
        <p class="very-subtle">MIT-${submission.key}</p>
      </@bs3form.labelled_form_group>
    </#if>
  </div>
</div>

<div class="row">
  <div class="col col-md-6">
    <@bs3form.labelled_form_group "startDate" "Start Date">
      <div class="input-group">
        <@f.input path="startDate" cssClass="form-control date-picker" />
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
      </div>
    </@bs3form.labelled_form_group>
  </div>
  <div class="col col-md-6">
    <@bs3form.labelled_form_group "endDate" "End Date">
      <div class="input-group">
        <@f.input path="endDate" cssClass="form-control date-picker" />
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
      </div>
    </@bs3form.labelled_form_group>
  </div>
</div>

<@bs3form.labelled_form_group path="issueType" labelText="Type">
  <@f.select path="issueType" cssClass="form-control">
    <option value="" style="display: none;">Please select one&hellip;</option>
    <#list issueTypes as type>
      <@f.option value="${type.code}" label="${type.description}" />
    </#list>
  </@f.select>
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group path="issueTypeDetails" labelText="Other" cssClass="issueTypeDetails">
  <@f.input path="issueTypeDetails" cssClass="form-control" />
</@bs3form.labelled_form_group>

<@bs3form.labelled_form_group "reason" "Details">
  <@f.textarea path="reason" cssClass="form-control" rows="5" />
  <div class="help-block">Please provide further details of the mitigating circumstances and how they have affected your assessments</div>
</@bs3form.labelled_form_group>


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
          $(this).closest(".modal").on("hidden.bs.modal", function(e){
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