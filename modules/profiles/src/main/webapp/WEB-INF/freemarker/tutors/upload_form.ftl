<#assign introText>
	<p>The spreadsheet must be in <samp>.xlsx</samp> format (created in Microsoft Excel 2007 or newer, or another compatible spreadsheet application). You can download a template spreadsheet which is correctly formatted, ready for completion.<p>
	<p>The spreadsheet must contain two columns, headed:<p>
	<ul>
	<li><b>student_id</b> - contains the student's University ID number (also known as the library card number)</li>
	<li><b>tutor_id</b> - contains the tutor's University ID number</li>
	</ul>
	<p>You may need to <a href='http://office.microsoft.com/en-gb/excel-help/format-numbers-as-text-HA102749016.aspx?CTT=1'>format these columns</a> as text to avoid Microsoft Excel removing 0s from the start of ID numbers.</p>
	<p>The spreadsheet may also contain other columns and information for your own reference (these will be ignored by Tabula).</p>
</#assign>
<@f.form method="post" enctype="multipart/form-data" action="${submitUrl}" commandName="allocateStudentsToTutorsCommand">
	<p>You can set personal tutors for many students at once by uploading a spreadsheet.
		<a href="#"
			id="tutor-intro"
			class="use-introductory"
			data-hash="${introHash("tutor-intro")}"
			data-title="Personal tutor spreadsheet"
			data-trigger="click"
			data-placement="bottom"
			data-html="true"
			data-content="${introText}"><i class="icon-question-sign"></i></a></p>

	<ol>
		<li><strong><a href="<@routes.tutor_template department />">Download a template spreadsheet</a></strong>. This will be prefilled with the names and University ID numbers of students and their personal tutor (if they have one) in ${department.name}. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
		</li>
		<li><strong>Allocate students</strong> to tutors using the dropdown menu in the <strong>Tutor name</strong> column or by typing a tutor's University ID into the <strong>tutor_id</strong> column. The <strong>tutor_id</strong> field will be updated with the University ID for that tutor if you use the dropdown.
			Any students with an empty <strong>tutor_id</strong> field will have their personal tutor removed, if they have one.</li>
		<li><strong>Save</strong> your updated spreadsheet.</li>
	    <li><@form.labelled_row "file.upload" "Choose your updated spreadsheet" "step-action" ><input type="file" name="file.upload"  /> </@form.labelled_row></li>
	</ol>

	<input name="isfile" value="true" type="hidden"/>


	<div class="submit-buttons">
		<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
	</div>
</@f.form>