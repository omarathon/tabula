<#escape x as x?html>
  <#assign maxFileAttachments=command.maxFileAttachments />
  <@bs3form.labelled_form_group path="minimumFileAttachmentLimit" labelText="Minimum attachments per submission">
    <@f.select path="minimumFileAttachmentLimit" cssClass="form-control">
      <@f.options items=1..maxFileAttachments />
    </@f.select>
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="fileAttachmentLimit" labelText="Maximum attachments per submission">
    <@f.select path="fileAttachmentLimit" cssClass="form-control">
      <@f.options items=1..maxFileAttachments />
    </@f.select>
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="fileAttachmentTypes" labelText="Accepted attachment file types (optional)">
    <@f.errors path="fileAttachmentTypes" cssClass="error" />
    <@f.input path="fileAttachmentTypes"  type="hidden" />

    <script type="text/javascript" nonce="${nonce()}">
      jQuery(function ($) {
        var textListController = new TextListController('#fileExtensionList', '#fileAttachmentTypes');
        textListController.transformInput = function (text) {
          var result = text.replace(new RegExp('\\.', 'g'), '');
          return result.toLowerCase();
        };
        textListController.preventDuplicates = true;
        textListController.init();
      });
    </script>
    <div id="fileExtensionList" class="textBoxListContainer">
      <ul>
        <li class="inputContainer"><input class="text form-control" type="text"></li>
      </ul>
    </div>
    <div class="help-block">
      To restrict the file types students can submit, enter the file extensions you accept separated by a single space e.g. PDF DOCX DOC.
      Leave blank to accept any file type.
    </div>
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="individualFileSizeLimit" labelText="Maximum file size">
    <div class="input-group">
      <@f.input path="individualFileSizeLimit" cssClass="form-control" />
      <span class="input-group-addon">MB</span>
    </div>
    <div class="help-block">
      Enter the maximum file size in megabytes for a single file the student can upload. If you wish to submit a file to Turnitin, it must be less
      than ${turnitinFileSizeLimit}MB.
    </div>
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="wordCountMin" labelText="Minimum word count">
    <@f.input path="wordCountMin" cssClass="form-control" id="wordCountMin" />
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="wordCountMax" labelText="Maximum word count">
    <@f.input path="wordCountMax" cssClass="form-control" id="wordCountMax" />
    <div class="help-block">
      If you specify a minimum and/or maximum word count, students must declare the word count for their submission.
      In order to submit, the word count they declare must comply with any minimum or maximum you specify.
      If you do not specify a minimum or maximum, students submit without declaring a word count.
      Note that Tabula does not calculate the number of words in submissions.
    </div>
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="wordCountConventions" labelText="Word count conventions">
    <@f.textarea path="wordCountConventions" id="wordCountConventions" rows="6" cssClass="form-control col-md-6" />
    <div class="help-block">
      <div class="enabled">
        Tell students if there are specific items that they should include in or exclude from the word count e.g. a bibliography or appendices.
        This only applies when you specify a minimum and/or maximum word count.
      </div>
      <div class="disabled">
        This only applies when you specify a minimum and/or maximum word count.
      </div>
    </div>
  </@bs3form.labelled_form_group>
  <@bs3form.labelled_form_group path="comment" labelText="Text to show on submission form">
    <@f.textarea path="comment" id="assignmentComment" rows="6" cssClass="form-control col-md-6" />
    <div class="help-block">
      You can start a new paragraph by inserting a blank line (i.e. press Enter twice).
    </div>
  </@bs3form.labelled_form_group>

  <script nonce="${nonce()}">
    jQuery(function ($) {

      var updateWordCountConventions = function () {
        if ($('input[name=wordCountMin]').val().length === 0 && $('input[name=wordCountMax]').val().length === 0) {
          $('textarea[name=wordCountConventions]').prop('disabled', true).closest('div')
            .find('.help-block .disabled').show()
            .end().find('.help-block .enabled').hide();
        } else {
          $('textarea[name=wordCountConventions]').prop('disabled', false).closest('div')
            .find('.help-block .enabled').show()
            .end().find('.help-block .disabled').hide();
        }
      };

      $('input[name=wordCountMin], input[name=wordCountMax]').on('change', function (e) {
        e.stopPropagation();
        updateWordCountConventions();
      });

      updateWordCountConventions();

    });
  </script>


</#escape>
