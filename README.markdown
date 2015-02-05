Tabula
==========

This is Tabula, the MyDepartment system including a number of modules. It has a [JIRA project](https://bugs.elab.warwick.ac.uk/browse/TAB).

Currently, the modules that are in Tabula are:

- coursework - Coursework Submission, the Assignment Management project that used to be [Horses for Courses](https://bugs.elab.warwick.ac.uk/browse/HFC)
- profiles - Student Profiles
- groups - Small Group Teaching management
- attendance - Monitoring point management and recording
- admin - Administration & Permissions functions
- home - Static content and homepage
- reports - Report generation for other modules
- api - API-specific details

Note: This is likely to change to being 3 modules: web, reports and api (possibly with common? commands? who knows).

Setting up for development
----------

Install Tomcat 8 from here: http://tomcat.apache.org/download-80.cgi - on Unix, it makes sense just to extract Tomcat into `/opt` and then symlink it to `/usr/local/tomcat-8` or wherever is convenient. You can then upgrade Tomcat without breaking any configuration by just changing that symlink.

Create a new base directory for Tabula's config and deployment, e.g. `/var/tomcat/servers/tabula`. The structure and contents of this directory should be as follows:

```
tabula/
├── bin/
│   └── setenv.sh
├── conf/
│   ├── context.xml
│   ├── membership-ds.xml
│   ├── server.xml
│   ├── sits-ds.xml
│   ├── tabula-ds.xml
│   └── web.xml
├── lib/
│   ├── log4j.xml
│   ├── ojdbc6.jar
│   ├── tabula.properties
│   └── tabula-sso-config.xml
├── logs/
├── temp/
├── webapps/
└── work/
```

The contents of these files are as follows:

### `bin/setenv.sh`

This is a script that's run to set environment variables for the running instance. You'll probably want to use it to set up memory management, JRebel, debugging etc.

A sample file can be found in `config/servers/augustus/bin`

### `conf/context.xml`

This contains resources that are to be included in the server - in this case, just JNDI DataSources. You can use entity XML includes to have these in separate files.

There is a sample `context.xml` and some sample datasources in `config/servers/augustus/conf`

### `conf/server.xml`

Instructions on how to run the server, including what ports to bind to. This is pretty much standard.

There is a sample `server.xml` in `config/servers/augustus/conf`

### `conf/web.xml`

This is just copied from the `conf` directory in the Tomcat 8 install. I couldn't get Tomcat to run without it being copied, which sucks a bit.

### `lib/ojdbc6.jar`

You can get this from http://pkg.elab.warwick.ac.uk/oracle.com/ojdbc6.jar

### `lib/log4j.xml`

Log4J config. An alternative would be to package up a `log4j.properties` with the WARs, but that wouldn't allow us to change config per-server.

### `lib/tabula.properties`

Properties passed into the app. You can get one of these off of tabula-test and then personalise it.

### `lib/tabula-sso-config.xml`

The usual SSO config guff. You will need to get this configuration added to Web Sign-on for SSO to work.

### ActiveMQ

Unpack activemq-ra.rar into the deploy directory from http://pkg.elab.warwick.ac.uk/activemq.apache.org/activemq-ra.zip or install ActiveMQ locally instead. Note that the repository version of ActiveMQ doesn't work on Ubuntu 14.10

### Apache

Set up an Apache vhost referencing the include files in `config/servers/common/vhosts` -
use `rewrites.inc` for full Tabula development.

You need an HTTPS vhost for SSO so if you're only going to set up one vhost,
it should be the HTTPS one. The include files reference a map to get the port to use,
so you may need to define yours with two lines such as

    RewriteMap proxy txt:/etc/apache2/tabulaport.txt
    RewriteMap scheduler txt:/etc/apache2/tabulaport.txt

The above line should point to a file containing this line (assuming default Tomcat port 8080):

    port 8080

Copy `local-example.properties` as `local.properties` and edit the properties in there
to match your system. `local.properties` will be ignored by Git.

Run `mvn -Pdeploy` to build the app and copy an exploded WAR to the
location you specified in your properties file.

Some other useful Maven commands:

- `mvn -DskipTests` - skip tests during deploy (Maven will run tests by default)
- `mvn -DskipTests -Dmaven.test.skip=true` - also skip test compilation
- `mvn --projects modules/coursework --also-make` - work on a single module, but make module dependencies like common
- `mvn -Pdeploy --projects modules/attendance --also-make -DskipTests` - Deploy a single module
    (Remember Tomcat will still deploy old wars unless you delete them!)
- `mvn test -Dsurefire.useFile=false` - run tests, showing stack trace of failures in the console
- `mvn test -Dtest=uk.ac.warwick.tabula.coursework.ApplicationTest` - run a specific test
- `mvn -Pfunctional-test --projects modules/functional-test clean test` - run functional tests (start Tomcat instance first)
- `mvn -Pfunctional-test --projects modules/functional-test clean test -Dtest=CourseworkModuleManagerTest` - run a specific functional test
- `mvn -Pdev-deploy-views` - syncs the latest Freemarker templates into the deployed app - no need to redeploy
- `mvn -Pdev-deploy-static` - syncs the latest static content, including compiled assets like LessCSS
- `mvn scala:cc` - continuously compile sources as they are saved in your IDE
- `mvn scala:cctest` - continuously compile sources and run tests as they are saved in your IDE (tests only run if it passes)
- `mvn scala:cctest -Dtest=MyTest` - continously compile and run a single test

Directory structure
----------

- `config`
  - `servers`
    - `common` - stuff most servers use for Apache config etc.
  - `scripts`
    - `schema` - SQL migration scripts for any database schema changes.
    - `varsrc` - Deploy scripts, usually found in /var/src/courses on servers
- `modules` - Tabula modules
  - `(modulename)`
    - `src`
      - `main`
        - `scala` - Scala source files
        - `java` - Java source files
        - `resources` - non-code files that will be available in the app classpath
        - `webapp` - other non-code files that make up the WAR.
        - `artwork` - source graphics not included in the app, but used to generate static images. Usually SVG/Inkscape.
      - `test`
      - `console`

Creating a new module
---------

When creating a new module, if it is a web app then you need to have an empty `.war-project` file in the WEB-INF directory.
This tells the build to do certain stuff.

Maven overlays
---------

In a module, Maven will overlay resources from other modules when it builds the WAR. In an overlay,
files that exist aren't overwritten, so you can define a file with the same name to override behaviour
that would be defined in the overlay.

- `common/.../WEB-INF` -> `WEB-INF` - default `applicationContext.xml` and some includes that can be overridden
- `home/.../WEB-INF/static-hashes.properties` -> `WEB-INF/static-hashes.properties` - the hashes of static content, for URL construction

Database schema changes
---------

Any SQL for changing the database schema should go in `config/scripts/schema/migrations`. Migrations are done
manually so you need to run it on dev, test and production separately. The recommended route is to
migrate dev first to get it working, and then update test and production _at the same time_. If you
don't do these at the same time then you run the risk of everything apparently working fine on test
until you deploy to live, which then explodes.

It is also best to do these before or soon after you've pushed the new code onto the central develop branch,
so that a deploy to tabula-test won't cause explosions.

Code style
----------

The code style for the project has developed as we've worked on it and so there are some bits of code that are in a style
that we later decided was not great. We should decide how to do certain things and fix the old code. It's possible that for
plain Scala style we can just delegate to http://docs.scala-lang.org/style/ but we also have things in our app that are best
used in a certain way, or Spring offers multiple ways of doing something and we want to document the one we've settled on.

- Should always use dots to call methods (e.g. `command.validate(errors)` rather than `command validate errors`) unless it's
  a DSL (specific example being the test framework where you can write `assignment.name should be ("Jim")`)

- Methods that have a side-effect should have parentheses. No-paren methods should only be for getters. So `form.onBind()`, not `form.onBind`.

- Preferred method of doing a foreach loop is `for (foo <- fooList)`

- A `map` operation should always use `map` instead of `for (item <- seq) yield item.mappedValue`; for-comprehensions should
  only be used where there are multiple generators

- Some controllers don't have "Controller" at the end of their name but that turned out to be confusing, so they should all end with Controller.

### Validation

- Always add the `@Valid` annotation to the controller method argument.

- Use validation annotations on your commands for simple things if you want.

- For custom code, make the command extend `SelfValidating` and in the controller body do `validatesSelf[DeleteFeedbackCommand]`

### Running code on bind (pre-validation)

- Mixin `BindListener` on your command instead of calling `onBind` from a controller