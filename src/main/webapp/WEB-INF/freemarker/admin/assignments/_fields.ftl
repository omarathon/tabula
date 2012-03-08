<#compress>

<#macro datefield path label>
<@form.labelled_row path label>
<@f.input path=path cssClass="date-time-picker" />
</@form.labelled_row>
</#macro>

<@form.labelled_row "name" "Assignment name">
<@f.input path="name" cssClass="text" />
</@form.labelled_row>

<@datefield path="openDate" label="Open date" />
<@datefield path="closeDate" label="Close date" />

<#if newRecord>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	</@form.labelled_row>
	
<#else>

	<@form.labelled_row "academicYear" "Academic year">
	<@spring.bind path="academicYearString">
	<span class="uneditable-value">${status.value} <span class="hint">(can't be changed)<span></span>
	</@spring.bind>
	</@form.labelled_row>

</#if>

<#if features.submissions>
	<@form.labelled_row "collectSubmissions" "Collect submissions">
	<@f.checkbox path="collectSubmissions" id="collectSubmissions" />
	
	<div id="submission-options">
			<h3>Submission options</h3>
			<div>
				<label class="disabled"><@f.checkbox path="restrictSubmissions" disabled="true" />
					Restrict submissions to the students registered on the module
				</label>
				<div class="subtle">
				 	Restricting to a group of students is not currently supported, so
				 	the above option can't be changed.
				</div>
			</div>
			<div>
				<label><@f.checkbox path="allowLateSubmissions" />
					Allow submissions after the close date
				</label>
				<div class="subtle">
					If you allow submissions after the close date, you will be able
					to view who made late submissions and take the appropriate action.
				</div>
			</div>
		</div> 
	
	
	</@form.labelled_row>
	
	<@form.row>
	<@form.field>
	
		
		
	</@form.field>
	</@form.row>
	
</#if>

</#compress>