<#escape x as x?html>
	<@form.labelled_row "allocationMethod" "Allocation method">
		<@form.label checkbox=true>
			<@f.radiobutton path="allocationMethod" value="Manual" />
			Manually allocate students to groups
			<a class="use-popover" data-html="true"
		     data-content="No automatic allocation will occur; students must all be manually allocated to groups by administrators">
		   	<i class="icon-question-sign"></i>
		  </a>
		</@form.label>
		<@form.label checkbox=true>
			<@f.radiobutton path="allocationMethod" value="StudentSignUp" />
			Allow students to sign up for groups
			<a class="use-popover" data-html="true"
		     data-content="Students are allowed to sign up for groups while registration is open. Administrators can still assign students to groups">
		   	<i class="icon-question-sign"></i>
		  </a>
		</@form.label>
		<@form.label checkbox=true>
			<@f.radiobutton path="allocationMethod" value="Random" />
			Randomly allocate students to groups
			<a class="use-popover" data-html="true"
		     data-content="Students in the allocation list are randomly assigned to groups. Administrators can still assign students to groups. There may be a delay between students being added to the allocation list and being allocated to a group.">
		   	<i class="icon-question-sign"></i>
		  </a>
		</@form.label>
	</@form.labelled_row>
</#escape>