<#escape x as x?html>

<#macro row student>
<tr class="related_student">
	<td>
		<@fmt.member_photo student "tinythumbnail" />
	</td>
	<td><h6>${student.firstName}</h6></td>
	<td><h6>${student.lastName}</h6></td>
	<td><a class="profile-link" href="<@routes.profile student />">${student.universityId}</a></td>
	<td>${student.groupName}</td>
	<td>${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
	<td>${(student.mostSignificantCourseDetails.route.name)!""}</td>
</tr>
</#macro>

<#-- Print out a table of tutees/supervisees.
 is_relationship=true means each item is a StudentRelationship rather than a member-->
<#macro table tutees is_relationship>
<table class="related_students table-bordered table-striped table-condensed tabula-purple">
	<thead>
	<tr>
		<th class="photo-col">Photo</th>
		<th class="tutee-col">First name</th>
		<th class="tutee-col">Last name</th>
		<th class="id-col">ID</th>
		<th class="type-col">Type</th>
		<th class="year-col">Year</th>
		<th class="course-but-photo-col">Course</th>
	</tr>
	</thead>

	<tbody>
		<#list tutees as item>
			<#if is_relationship>
				<#if item.studentMember?has_content>
					<#assign student = item.studentMember />
					<@row student />
				</#if>
			<#else>
				<@row item />
			</#if>
		</#list>
	</tbody>
</table>

<#if !tutee_table_script_included??>
<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
    (function($) {
        $(function() {
            $(".related_students").tablesorter({
                sortList: [[2,0], [4,0], [5,0]]
            });

            $(".related_student").on("mouseover", function(e) {
                $(this).find("td").addClass("hover");
            }).on("mouseout", function(e) {
                        $(this).find("td").removeClass("hover");
                    }).on("click", function(e) {
                        if (! $(e.target).is("a") && ! $(e.target).is("img")) $(this).find("a.profile-link")[0].click();
                    });
        });
    })(jQuery);
</script>
<#assign tutee_table_script_included=true />
</#if>

</#macro>

</#escape>