<#escape x as x?html>
  <h1>Declare mitigating circumstances</h1>
  <p>Some text about mitigating circumstances submissions Vivamus aliquet elit ac nisl. Phasellus consectetuer vestibulum elit. Vivamus consectetuer hendrerit lacus. Fusce ac felis sit amet ligula pharetra condimentum. Nullam vel sem.</p>
  <p>You can find more information about the universities mitigating circumstances policies on the <a href="https://warwick.ac.uk/services/aro/dar/quality/categories/examinations/policies/u_mitigatingcircumstances/">Teaching Quality</a> website.</p>

  <div class="fix-area">
    <@f.form
      id="editMitCircsCommand"
      method="POST"
      modelAttribute="editMitCircsCommand"
      class="dirty-check double-submit-protection"
      enctype="multipart/form-data">

      <#include "_fields.ftl" />

      <div class="fix-footer">
        <input type="submit" class="btn btn-primary" value="Submit">
        <a class="btn btn-default dirty-check-ignore"
           href="<@routes.mitcircs.studenthome submission.student />">Cancel</a>
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