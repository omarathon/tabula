<#escape x as x?html>
<fieldset>

	<#if newRecord>
		<@form.labelled_row "id" "ID">
			<@f.input path="id" cssClass="text" />
			<a class="use-popover" data-html="true"
		     data-content="A unique identifier. Set this to something descriptive, using alphanumeric chars, - and _ only, so it's easy to see in the database. e.g. 'personalTutor'">
		   	<i class="icon-question-sign"></i>
		  </a>
		</@form.labelled_row>
	<#else>
		<@form.labelled_row "id" "ID">
			<@spring.bind path="id">
				<span class="uneditable-value">${status.actualValue} <span class="hint">(can't be changed)<span></span>
			</@spring.bind>
		</@form.labelled_row>
	</#if>
	
	<#if newRecord || relationshipType.empty>
		<@form.labelled_row "urlPart" "URL string">
			<@f.input path="urlPart" cssClass="text" />
			<a class="use-popover" data-html="true"
		     data-content="A string that can be used as part of the URL. e.g. 'tutor'">
		   	<i class="icon-question-sign"></i>
		  </a>
		</@form.labelled_row>
	<#else>
		<@form.labelled_row "urlPart" "URL string">
			<@spring.bind path="urlPart">
				<span class="uneditable-value">${status.actualValue} <span class="hint">(can't be changed)<span></span>
			</@spring.bind>
		</@form.labelled_row>
	</#if>
	
	<@form.labelled_row "description" "Description">
		<@f.input path="description" cssClass="text" />
		<a class="use-popover" data-html="true"
	     data-content="A descriptive name for this type of relationship. Capitalise accordingly; e.g. 'Personal Tutor'">
	   	<i class="icon-question-sign"></i>
	  </a>
	</@form.labelled_row>
	
	<@form.labelled_row "agentRole" "Agent Role">
		<@f.input path="agentRole" cssClass="text" />
		<a class="use-popover" data-html="true"
	     data-content="How you'd refer to a single agent in this relationship. Input as if used in the middle of a sentence; it will be capitalised accordingly. e.g. 'personal tutor'">
	   	<i class="icon-question-sign"></i>
	  </a>
	</@form.labelled_row>
	
	<@form.labelled_row "studentRole" "Student Role">
		<@f.input path="studentRole" cssClass="text" />
		<a class="use-popover" data-html="true"
	     data-content="How you'd refer to a single student in this relationship. Input as if used in the middle of a sentence; it will be capitalised accordingly. e.g. 'personal tutee'">
	   	<i class="icon-question-sign"></i>
	  </a>
	</@form.labelled_row>
	
	<@form.labelled_row "defaultSource" "Default source">
		<label class="radio">
			<@f.radiobutton path="defaultSource" value="local" />
			Tabula (Local)
			<a class="use-popover" data-html="true"
		     data-content="By default, information will be input directly into Tabula.">
		   	<i class="icon-question-sign"></i>
		  </a>
		</label>
		<label class="radio">
			<@f.radiobutton path="defaultSource" value="sits" />
			SITS
			<a class="use-popover" data-html="true"
		     data-content="By default, information will be imported from SITS. Relies on code existing to know how to import this type.">
		   	<i class="icon-question-sign"></i>
		  </a>
		</label>
	</@form.labelled_row>
	
	<@form.row defaultClass="">
		<@form.field>
			<@form.label checkbox=true>
				<@f.checkbox path="defaultDisplay" id="defaultDisplay" />
					Display this relationship type by default for departments
				</@form.label>
			<@f.errors path="defaultDisplay" cssClass="error" />
		</@form.field>
	</@form.row>
	
	<@form.row defaultClass="">
		<@form.field>
			<@form.label checkbox=true>
				<@f.checkbox path="expectedUG" id="expectedUG" />
					Expect Undergraduates to always have a relationship of this type
					<a class="use-popover" data-html="true"
						 data-content="If enabled, on undergraduate student profiles that don't have this type of relationship the box will be empty rather than hidden">
						<i class="icon-question-sign"></i>
					</a>
				</@form.label>
			<@f.errors path="expectedUG" cssClass="error" />
		</@form.field>
	</@form.row>
	
	<@form.row defaultClass="">
		<@form.field>
			<@form.label checkbox=true>
				<@f.checkbox path="expectedPGT" id="expectedPGT" />
					Expect Taught Postgraduates to always have a relationship of this type
					<a class="use-popover" data-html="true"
						 data-content="If enabled, on taught postgraduate student profiles that don't have this type of relationship the box will be empty rather than hidden">
						<i class="icon-question-sign"></i>
					</a>
				</@form.label>
			<@f.errors path="expectedPGT" cssClass="error" />
		</@form.field>
	</@form.row>
	
	<@form.row defaultClass="">
		<@form.field>
			<@form.label checkbox=true>
				<@f.checkbox path="expectedPGR" id="expectedPGR" />
					Expect Research Postgraduates to always have a relationship of this type
					<a class="use-popover" data-html="true"
						 data-content="If enabled, on taught postgraduate student profiles that don't have this type of relationship the box will be empty rather than hidden">
						<i class="icon-question-sign"></i>
					</a>
				</@form.label>
			<@f.errors path="expectedPGR" cssClass="error" />
		</@form.field>
	</@form.row>
	
	<@form.labelled_row "sortOrder" "Sort order">
		<@f.input path="sortOrder" cssClass="text" />
		<a class="use-popover" data-html="true"
	     data-content="Sort order when viewing multiple relationships. Lower numbers appear first. Identical numbers are ordered alphabetically by ID.">
	   	<i class="icon-question-sign"></i>
	  </a>
	</@form.labelled_row>

</fieldset>
</#escape>