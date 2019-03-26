<#escape x as x?html>
  <h1>Declare mitigating circumstances</h1>
  <p>Some text about mitigating circumstances submissions Vivamus aliquet elit ac nisl. Phasellus consectetuer vestibulum elit. Vivamus consectetuer hendrerit lacus. Fusce ac felis sit amet ligula pharetra condimentum. Nullam vel sem.</p>
  <p>You can find more information about the universities mitigating circumstances policies on the <a href="https://warwick.ac.uk/services/aro/dar/quality/categories/examinations/policies/u_mitigatingcircumstances/">Teaching Quality</a> website.</p>

  <div class="fix-area">
    <@f.form id="newMitCircStudent" method="POST" modelAttribute="createMitCircsCommand" class="dirty-check">

      <div class="row">
        <div class="col col-md-6">
          <@bs3form.labelled_form_group "startDate" "Start Date">
            <div class="input-group">
              <@f.input path="startDate" cssClass="form-control date-time-picker" />
              <#if endOffset?has_content><input class="endoffset" type="hidden" data-end-offset="1209600000" /></#if>
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </div>
          </@bs3form.labelled_form_group>
        </div>
        <div class="col col-md-6">
          <@bs3form.labelled_form_group "endDate" "End Date">
            <div class="input-group">
              <@f.input path="endDate" cssClass="form-control date-time-picker" />
              <#if endOffset?has_content><input class="endoffset" type="hidden" data-end-offset="1209600000" /></#if>
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
        <@f.textarea path="reason" cssClass="form-control" rows="5" cssStyle="height: 150px;" />
        <div class="help-block">Please provide further details of the mitigating circumstances and how they have affected your assessments</div>
      </@bs3form.labelled_form_group>

      <div class="fix-footer">
        <input type="submit" class="btn btn-primary" value="Submit">
        <a class="btn btn-default dirty-check-ignore"
           href="<@routes.mitcircs.studenthome student />">Cancel</a>
      </div>

    </@f.form>
  </div>

  <script type="text/javascript">
    (function ($) {
      $('select[name=issueType]').on('change', function(){
        $('.issueTypeDetails').toggle($(this).val() === "Other");
      });

      $('select[name=issueType]').trigger('change');
    })(jQuery);
  </script>
</#escape>