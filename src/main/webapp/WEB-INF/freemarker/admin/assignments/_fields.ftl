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

</#compress>