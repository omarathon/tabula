The following message was submitted via the 'Contact your administrator' form at https://tabula.warwick.ac.uk/help.

Name: ${info.name!"Not provided"}
Email: ${info.email!"Not provided"}
Usercode: ${info.usercode!"Not provided"} <#if user.loggedIn>(logged in)</#if>

---

${info.message}

---

You received this email because you hold a departmental administrator role in the user's department.

You can opt out of receiving these emails by changing your Tabula user settings and unchecking the
"Students who have requested assistance from a departmental admin" checkbox:

<@url page='/settings' />
