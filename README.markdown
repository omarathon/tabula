Coursework submission
==========

This is the assignment management application. It has a [JIRA project](https://bugs.elab.warwick.ac.uk/browse/HFC).

Setting up for development
----------

Set up an empty JBoss 5 server.

The conf directory should contain:

- tabula-sso-config.xml modified to your details.
- tabula.properties modified to your details
- tabula-private.properties (may be empty but must exist)

You can get versions of these from the servers directory

Obtain the datasource files for the app and ADS, and place in the deploy directory.

You will need to get this configuration added to Web Sign-on for SSO to work.

Set up an Apache vhost referencing the include files in `servers/common/vhosts`.
You need an HTTPS vhost for SSO so if you're only going to set up one vhost,
it should be the HTTPS one. The include files reference a map to get the port to use,
so you may need to define yours with a line such as

    RewriteMap proxy txt:/etc/apache2/coursesport.txt

The above line should point to a file containing this line (assuming default JBoss port 8080):

    port 8080

Copy `local-example.properties` as `local.properties` and edit the properties in there
to match your system. `local.properties` will be ignored by Git.

Run `ant dev-deploy-unpacked` to build the app and copy an exploded WAR to the
location you specified in your properties file.

Other useful Ant tasks are:

- `test` - runs Unit tests
- `test -Dtest.name=TestClassName` - runs a specific test
- `dev-deploy-views` - syncs the latest Freemarker templates into the deployed app - no need to redeploy
- `dev-deploy-static` - syncs the latest static content, including compiled assets like LessCSS
- `list-runtime-jars` - lists all the jars that get bundled in the app. Useful to check for duplicates/conflicts.

Directory structure
----------

- `src`
    - `main` 
        - `scala` - Scala source files
        - `java` - Java source files
        - `resources` - non-code files that will be available in the app classpath
        - `webapp` - other non-code files that make up the WAR.
        - `artwork` - source graphics not included in the app, but used to generate static images. Usually SVG/Inkscape.
    - `test`
    - `console`
- `lib`
    - `build` - libraries used for compilation only
    - `runtime` - libraries included in the app package
    - `test` - libraries used for testing
    - `src` - source bundles for any library, to attach in your IDE
- `servers`
    - `common` - stuff most servers use for Apache config etc.
- `scripts`
    - `schema` - SQL migration scripts for any database schema changes.
    - `varsrc` - Deploy scripts, usually found in /var/src/courses on servers
    
IDE setup
---------

If you want to run unit tests in Eclipse, you'll need to be weaving classes with Load-Time Weaving.
All you need to do is pass the weaving agent to Java when you launch, e.g.

    -javaagent:/home/nick/code/aspectjweaver.jar
    
With the actual path of aspectjweaver.jar there. You can even go to Installed JREs in Eclipse and
add the option there, so that all commands are run with the weaver. It shouldn't affect any other
things you run, since it does nothing unless there's an aop.xml file.

Database schema changes
---------

Any SQL for changing the database schema should go in `scripts/schema/migrations`. Migrations are done
manually so you need to run it on dev, test and production separately. The recommended route is to
migrate dev first to get it working, and then update test and production _at the same time_. If you
don't do these at the same time then you run the risk of having everything working on test so you do
a deploy and it's broken on live because of the old schema.

There is also a `create-all.sql` which should define all the tables from scratch - if your migration
adds or alters a column then it should do this in the initial `CREATE TABLE` command here.
