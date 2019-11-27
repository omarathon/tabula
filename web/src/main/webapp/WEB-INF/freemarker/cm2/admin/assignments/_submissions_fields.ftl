<#escape x as x?html>
  <@bs3form.labelled_form_group path="" labelText="Set submission options">
    <@bs3form.checkbox path="collectSubmissions">
      <@f.checkbox path="collectSubmissions" id="collectSubmissions" /> Collect submissions
    </@bs3form.checkbox>
    <span class="help-block">Select this option to enable students to submit coursework for this assignment.</span>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="" labelText="Plagiarism check">
    <@bs3form.checkbox path="automaticallySubmitToTurnitin">
      <@f.checkbox path="automaticallySubmitToTurnitin" id="automaticallySubmitToTurnitin" /> Automatically check submissions for plagiarism
    </@bs3form.checkbox>
    <span class="help-block">
      Select this option to check for plagiarism when a submission is received.
	  </span>
    <@f.hidden path="displayPlagiarismNotice" value="true" />
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="" labelText="Turnitin options">
    <#if assignment?? && assignment.turnitinId?has_content>
      <p>It isn't possible to edit these options because the assignment has already been submitted to Turnitin.</p>

      <@f.hidden path="turnitinStoreInRepository" />
      <@f.hidden path="turnitinExcludeBibliography" />
      <@f.hidden path="turnitinExcludeQuoted" />

      <ul class="fa-ul">
        <li><span class="fa-li"><i class="fal fa-${assignment.turnitinStoreInRepository?string('check', 'times')}"></i></span> Submit submissions to the Turnitin repository</li>
        <li><span class="fa-li"><i class="fal fa-${assignment.turnitinExcludeBibliography?string('check', 'times')}"></i></span> Exclude bibliographies from similarity reports</li>
        <li><span class="fa-li"><i class="fal fa-${assignment.turnitinExcludeQuoted?string('check', 'times')}"></i></span> Exclude quoted material from similarity reports</li>
      </ul>
    <#else>
      <@bs3form.checkbox path="turnitinStoreInRepository">
        <@f.checkbox path="turnitinStoreInRepository" /> Submit submissions to the Turnitin repository
      </@bs3form.checkbox>
      <span class="help-block">
        Select this option to store submissions in the standard Turnitin repository. Note that if you uncheck
        this option, submissions won't be stored by Turnitin and won't be available for plagiarism checking
        against any other assignments, whether at Warwick or otherwise.
      </span>

      <@bs3form.checkbox path="turnitinExcludeBibliography">
        <@f.checkbox path="turnitinExcludeBibliography" /> Exclude bibliographies from similarity reports
      </@bs3form.checkbox>
      <span class="help-block">
        If this option is selected, any content appearing after the following keywords or phrases on a line on its own
        will be ignored: "references", "references cited", "references and notes", "resources", "bibliography", "works cited".
        If this option isn't selected, it's still possible to exclude bibliographies from the "Filters and Settings" settings
        in the report viewer.
      </span>

      <@bs3form.checkbox path="turnitinExcludeQuoted">
        <@f.checkbox path="turnitinExcludeQuoted" /> Exclude quoted material from similarity reports
      </@bs3form.checkbox>
      <span class="help-block">
        If this option is selected, any content appearing between double quotation marks or indented will be ignored.
        If this option isn't selected, it's still possible to exclude quoted material from the "Filters and Settings" settings
        in the report viewer.
      </span>
    </#if>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="restrictSubmissions" labelText="Submission scope">
    <@bs3form.radio>
      <@f.radiobutton path="restrictSubmissions" value="true" />
      Only students enrolled on this assignment can submit coursework
    </@bs3form.radio>
    <@bs3form.radio>
      <@f.radiobutton path="restrictSubmissions" value="false" />
      Anyone with a link to the assignment can submit coursework
    </@bs3form.radio>
    <span class="help-block">
		If anyone with the assignment link can submit coursework, you can't use a marking workflow.<br />
		When you restrict submissions to students enrolled on this assignment, other students who visit the assignment page can still request access.
	</span>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="">
    <@bs3form.checkbox path="allowResubmission">
      <@f.checkbox path="allowResubmission" id="allowResubmission" /> Allow students to resubmit work
    </@bs3form.checkbox>
    <span class="help-block">
		Select this option to allow students to submit new work, which replaces any previous submission.
		Students cannot resubmit work after the close date.
	</span>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="" labelText="Late submission">
    <@bs3form.checkbox path="allowLateSubmissions">
      <@f.checkbox path="allowLateSubmissions" id="allowLateSubmissions" /> Allow new submissions after the close date
    </@bs3form.checkbox>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="" labelText="Extensions">
    <@bs3form.checkbox path="allowExtensions">
      <@f.checkbox path="allowExtensions" id="allowExtensions" /> Allow extensions
    </@bs3form.checkbox>
    <span class="help-block">
		You can grant extensions for an assignment to individual students<#if department.allowExtensionRequests> and students can request extensions via Tabula</#if>.
		<#if can.do("Department.ManageExtensionSettings", department)>
      <a class="btn btn-default btn-xs use-tooltip" title="Department extension request settings (opens in a new window/tab)"
         href="<@routes.cm2.extensionSettings department />" target="_blank">Review</a>
    <#else>
      Departmental administrators control whether extension requests are allowed across a department.
    </#if>
	</span>
  </@bs3form.labelled_form_group>

  <#if department.allowExtensionRequests>
    <@bs3form.labelled_form_group path="">
      <@bs3form.checkbox path="extensionAttachmentMandatory">
        <@f.checkbox path="extensionAttachmentMandatory" id="extensionAttachmentMandatory" /> Students must attach at least one file to an extension request
      </@bs3form.checkbox>
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="">
      <@bs3form.checkbox path="allowExtensionsAfterCloseDate">
        <@f.checkbox path="allowExtensionsAfterCloseDate"  id="allowExtensionsAfterCloseDate" /> Allow extensions after close date
      </@bs3form.checkbox>
      <span class="help-block">Select this option to permit students to request an extension for late or unsubmitted work.</span>

    </@bs3form.labelled_form_group>
  </#if>
</#escape>
