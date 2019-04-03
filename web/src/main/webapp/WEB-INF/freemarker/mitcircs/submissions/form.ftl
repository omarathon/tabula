<#escape x as x?html>
  <h1>Declare mitigating circumstances</h1>
  <p>Please complete this form so that your department can assess your mitigating circumstances. Examples of mitigating circumstances include (but aren’t limited to) situations that you couldn’t have predicted and have no control over, such as a serious illness or accident, the death of someone close, being the victim of crime, family difficulties and financial hardship. If your circumstances might affect your ability to undertake assessments or examinations then you should complete this form. Normally, independent evidence relating to your circumstances, such as a doctor’s note, should be provided as part of your application. You can find more information about the university’s mitigating circumstances policies on the <a href="https://warwick.ac.uk/services/aro/dar/quality/categories/examinations/policies/u_mitigatingcircumstances/">Teaching Quality</a> website.</p>

  <div class="fix-area">
    <@f.form
      method="POST"
      modelAttribute="command"
      class="dirty-check double-submit-protection"
      enctype="multipart/form-data">

      <#include "_fields.ftl" />

      <div class="fix-footer">
        <#assign submitLabel><#if submission??>Update<#else>Submit</#if></#assign>
        <input type="submit" class="btn btn-primary" value="${submitLabel}">
        <a class="btn btn-default dirty-check-ignore"
           href="<@routes.mitcircs.studenthome command.student />">Cancel</a>
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