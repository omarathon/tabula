<#import "mitcirc_form_macros.ftl" as mitcirc />

<#escape x as x?html>
  <h1>Supply pending evidence for MIT-${submission.key}</h1>
  <p>You stated that you will supply the following evidence on <@fmt.date date=submission.pendingEvidenceDue includeTime = false /></p>
  <#noescape>${submission.formattedPendingEvidence}</#noescape>

  <@f.form id="pendingEvidenceForm" method="POST" modelAttribute="command" class="dirty-check double-submit-protection" enctype="multipart/form-data">
    <@mitcirc.question_section
      question = "Please provide the pending evidence relevant to your submission"
    >
      <@bs3form.filewidget
        basename="file"
        labelText=""
        types=[]
        multiple=true
        required=false
        customHelp=" "
      />
    </@mitcirc.question_section>

    <#if allowMorePendingEvidence>
      <@mitcirc.question_section
      question = "Do you intend to supply more evidence in the future?"
      cssClass = "form-horizontal"
      >
        <div class="radio">
          <@bs3form.radio_inline>
            <@f.radiobutton path="morePending" value="true" /> Yes
          </@bs3form.radio_inline>
          <@bs3form.radio_inline>
            <@f.radiobutton path="morePending" value="false" /> No
          </@bs3form.radio_inline>
        </div>
        <div class="mitcircs-form__fields__morepending-subfield mitcircs-form__fields__morepending-subfield--yes" style="display: none;">
          <@bs3form.form_group "pendingEvidenceDue">
            <@bs3form.label path="pendingEvidenceDue" cssClass="col-xs-4 col-sm-2">Due date</@bs3form.label>
            <div class="col-xs-8 col-sm-4">
              <@spring.bind path="pendingEvidenceDue">
                <div class="input-group">
                  <@f.input path="pendingEvidenceDue" autocomplete="off" cssClass="form-control date-picker" />
                  <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
                </div>
              </@spring.bind>

              <@bs3form.errors path="pendingEvidenceDue" />
            </div>
          </@bs3form.form_group>

          <@bs3form.form_group "pendingEvidence">
            <@bs3form.label path="pendingEvidence" cssClass="col-xs-4 col-sm-2">Description</@bs3form.label>
            <div class="col-xs-8 col-sm-10">
              <@f.textarea path="pendingEvidence" cssClass="form-control" rows="5" />
              <@bs3form.errors path="pendingEvidence" />
            </div>
          </@bs3form.form_group>
        </div>
        <div class="mitcircs-form__fields__morepending-subfield mitcircs-form__fields__morepending-subfield--no" style="display: none;">
          <div class="alert alert-info">
            By submitting this form you are confirming that any evidence relating to this submission has been provided.
          </div>
        </div>
      </@mitcirc.question_section>
    <#else>
      <@f.hidden path="morePending" value="false" />
    </#if>

    <div class="fix-footer">
      <button type="submit" class="btn btn-primary">Confirm</button>
      <a class="btn btn-default dirty-check-ignore" href="<@routes.mitcircs.viewSubmission submission />">Cancel</a>
    </div>

  </@f.form>
</#escape>