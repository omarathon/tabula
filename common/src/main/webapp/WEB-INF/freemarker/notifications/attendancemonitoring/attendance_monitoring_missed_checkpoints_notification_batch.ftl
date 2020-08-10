The following ${students?size} students have missed at least ${level} monitoring points in the academic year ${academicYear.toString}:

<#list students as student>
- ${student.fullName!'Unknown'} (${student.universityId}) - ${(student.mostSignificantCourse.course.name)!""}
</#list>

Please refer to the Education Policy and Quality website for guidance on the appropriate action to take:

http://warwick.ac.uk/studentattendanceguide/principles
