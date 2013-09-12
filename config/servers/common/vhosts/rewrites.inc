## Populate request header X-Requested-URI with requested URI (including query string)
# If query string is empty, don't add a question mark
RewriteCond %{QUERY_STRING} ^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}]
RewriteCond %{QUERY_STRING} !^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}?%{QUERY_STRING}]
RequestHeader set X-Requested-URI "%{REQ_URI}e" env=!no-requested-uri
####

RewriteRule ^/favicon\.ico - [G,L]
RewriteRule \.php$ [G,L]

# static content suffixes
RewriteRule ^(/static/.*?)\.[0-9]{10,16}$ $1$2 [QSA]

# TAB-46 Rewrite legacy /courses to /coursework
RewriteRule ^/courses(.*)$ /coursework$1 [QSA,L,R=301]

# Add trailing slash because otherwise ensadness occurs
RewriteRule ^/coursework$ /coursework/ [QSA,L,R=301]
RewriteRule ^/profiles$ /profiles/ [QSA,L,R=301]
RewriteRule ^/scheduling$ /scheduling/ [QSA,L,R=301]
RewriteRule ^/admin$ /admin/ [QSA,L,R=301]
RewriteRule ^/groups$ /groups/ [QSA,L,R=301]
RewriteRule ^/attendance$ /attendance/ [QSA,L,R=301]

# Remove /home context
RewriteRule ^/home(.*)$ $1 [QSA,L,R=301]

# Rewrite everything for coursework into the courses module
RewriteRule ^/coursework/(.*)$ http://127.0.0.1:${proxy:port}/courses/$1 [QSA,P,L]

# Rewrite scheduling directly to the scheduler jboss
RewriteCond %{REQUEST_URI} ^/scheduling/(.*)
RewriteRule (.*) http://127.0.0.1:${scheduler:port}$1 [QSA,P,L]

# Rewrite everything that isn't in a module to the home module
RewriteCond %{REQUEST_URI} !^/coursework/(.*)
RewriteCond %{REQUEST_URI} !^/home/(.*)
RewriteCond %{REQUEST_URI} !^/profiles/(.*)
RewriteCond %{REQUEST_URI} !^/scheduling/(.*)
RewriteCond %{REQUEST_URI} !^/admin/(.*)
RewriteCond %{REQUEST_URI} !^/groups/(.*)
RewriteCond %{REQUEST_URI} !^/attendance/(.*)
RewriteRule (.*) http://127.0.0.1:${proxy:port}/home$1 [QSA,P,L]

# Else, go straight to Jboss
RewriteRule (.*) http://127.0.0.1:${proxy:port}$1 [QSA,P,L]