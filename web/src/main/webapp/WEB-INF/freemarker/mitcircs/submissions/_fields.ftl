<div class="row">
  <div class="col col-md-6">
    <@bs3form.labelled_form_group "startDate" "Start Date">
      <div class="input-group">
        <@f.input path="startDate" cssClass="form-control date-time-picker" />
        <#if endOffset?has_content><input class="endoffset" type="hidden" /></#if>
        <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
      </div>
    </@bs3form.labelled_form_group>
  </div>
  <div class="col col-md-6">
    <@bs3form.labelled_form_group "endDate" "End Date">
      <div class="input-group">
        <@f.input path="endDate" cssClass="form-control date-time-picker" />
        <#if endOffset?has_content><input class="endoffset" type="hidden" /></#if>
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
          <i class="fa fa-times-circle remove-attachment"></i>
        </li>
      </#list>
    </ul>
    <script>
      jQuery(function ($) {
        $(".remove-attachment").on("click", function (e) {
          e.preventDefault();
          $(this).closest("li.attachment").remove();
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