<#if field.name == "notes">
  <#assign rows = "3" />
  <#assign helpText>
    Notes are visible to markers, moderators and administrators for this assignment. Notes are not normally shared with students but could be disclosed at the request of the student concerned.
  </#assign>
  <#assign previewable = false />
  <#assign cssClass = "" />
<#else>
  <#assign rows = "6" />
  <#assign helpText>
    You can use Markdown <i class="fab fa-markdown"></i> syntax <a target="_blank" href="https://warwick.ac.uk/tabula/manual/cm2/markers/markdown/"><i class="icon-question-sign fa fa-question-circle"></i></a>
  </#assign>
  <#assign previewable = true />
  <#assign cssClass = "md-previewable" />
</#if>

<@bs3form.labelled_form_group path="fields[${field.id}].value" labelText=field.label help=help cssClass=cssClass >
  <@form.field>
    <@f.textarea id="fields[${field.id}].value" cssClass="form-control user-input" path="fields[${field.id}].value" rows=rows />
  </@form.field>
  <#if showHelpText?? && showHelpText>
    <div class="help-block">${helpText}</div>
  </#if>
  <#if previewable>
    <div class="preview-container" data-field-id="${field.id}">
      <label>Preview</label>
      <div class="well">
        <div class="preview-text"></div>
      </div>
    </div>
  </#if>
</@bs3form.labelled_form_group>