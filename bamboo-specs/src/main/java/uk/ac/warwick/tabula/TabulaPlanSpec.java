package uk.ac.warwick.tabula;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.Variable;
import com.atlassian.bamboo.specs.api.builders.deployment.Deployment;
import com.atlassian.bamboo.specs.api.builders.notification.Notification;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.api.builders.requirement.Requirement;
import com.atlassian.bamboo.specs.builders.notification.DeploymentFailedNotification;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.TestParserTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.builders.trigger.AfterSuccessfulBuildPlanTrigger;
import com.atlassian.bamboo.specs.builders.trigger.ScheduledTrigger;
import com.atlassian.bamboo.specs.model.task.ScriptTaskProperties;
import com.atlassian.bamboo.specs.model.task.TestParserTaskProperties;
import uk.ac.warwick.bamboo.specs.AbstractWarwickBuildSpec;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Plan configuration for Bamboo.
 * Learn more on: <a href="https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs">https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs</a>
 */
@BambooSpec
public class TabulaPlanSpec extends AbstractWarwickBuildSpec {

    private static final Project PROJECT =
        new Project()
            .key("TAB")
            .name("Tabula");

    private static final String LINKED_REPOSITORY = "Tabula";

    private static final String SLACK_CHANNEL = "#tabula";

    public static void main(String[] args) throws Exception {
        new TabulaPlanSpec().publish();
    }

    private static Stage buildStage() {
        Job job =
            new Job("Build and check", "BUILD")
                .tasks(
                    new VcsCheckoutTask()
                        .description("Checkout source from default repository")
                        .checkoutItems(new CheckoutItem().defaultRepository()),
                    new ScriptTask()
                        .description("gradlew clean check war")
                        .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                        .location(ScriptTaskProperties.Location.FILE)
                        .fileFromPath("gradlew")
                        .argument("clean check war")
                        .environmentVariables("JAVA_OPTS=\"-Xmx256m -Xms128m\""),
                    new ScriptTask()
                        .description("Touch test files so Bamboo doesn't ignore them")
                        .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                        .location(ScriptTaskProperties.Location.INLINE)
                        .inlineBody("find . -type f -name 'TEST-*.xml' -exec touch {} +")
                )
                .requirements(
                    new Requirement("system.jdk.JDK 1.8")
                );

        job.finalTasks(
            new TestParserTask(TestParserTaskProperties.TestType.JUNIT)
                .description("Parse test results")
                .resultDirectories("**/test-results/**/*.xml")
        );

        job.artifacts(
            new Artifact()
                .name("ROOT.war")
                .copyPattern("ROOT.war")
                .location("web/build/libs")
                .shared(true),
            new Artifact()
                .name("api.war")
                .copyPattern("api.war")
                .location("api/build/libs")
                .shared(true)
        );

        return new Stage("Build Stage").jobs(job);
    }

    @Override
    protected Collection<Plan> builds() {
        return Arrays.asList(
            build(PROJECT, "ALL", "Tabula")
                .linkedRepository(LINKED_REPOSITORY)
                .description("Run checks and build WARs")
                .stage(buildStage())
                .slackNotifications(SLACK_CHANNEL, false)
                .build(),
            build(PROJECT, "FUNC", "Tabula Functional Tests")
                .linkedRepository(LINKED_REPOSITORY).noBranches()
                .description("Run functional tests with a headless browser against tabula-test.warwick.ac.uk")
                .triggers(
                    new ScheduledTrigger()
                        .name("Daily morning build")
                        .description("5:45am run")
                        .scheduleOnceDaily(LocalTime.of(5, 45)),
                    new ScheduledTrigger()
                        .name("Daily afternoon build")
                        .description("1pm run")
                        .scheduleOnceDaily(LocalTime.of(13, 0))
                )
                .customConfig(plan -> plan.variables(new Variable("functionaltestserver", "tabula-test")))
                .gradle(
                    "Run functional tests",
                    "Test",
                    "TEST",
                    "clean -PintegrationTest test -Dtoplevel.url=https://${bamboo.functionaltestserver}.warwick.ac.uk --no-build-cache",
                    "**/test-results/**/*.xml",
                    new Artifact()
                        .name("Failed test screenshots")
                        .copyPattern("*.png")
                        .location("integrationTest/build/integrationTest-screenshots")
                )
                .build(),
            specsPlan(
                "CUKEPROD",
                "Tabula Specs Live",
                "production",
                "Run Cucumber specifications against tabula.warwick.ac.uk",
                LocalTime.of(8, 0)
            )
        );
    }

    private Plan specsPlan(String key, String name, String environment, String description, LocalTime schedule) {
        return build(PROJECT, key, name)
            .linkedRepository(LINKED_REPOSITORY).noBranches()
            .description(description)
            .triggers(new ScheduledTrigger().scheduleOnceDaily(schedule))
            .gradle("Run tests", "Cucumber", "SPECS", "cucumber -Dserver.environment=" + environment, "**/build/cucumber/results/*.xml")
            .build();
    }

    @Override
    protected Collection<Deployment> deployments() {
        return Collections.singleton(
            deployment(PROJECT, "ALL", "Tabula")
                .autoTomcatEnvironment("Development", "tabula-dev.warwick.ac.uk", "tabula", "dev", SLACK_CHANNEL)
                .autoTomcatEnvironment("Test", "tabula-test.warwick.ac.uk", "tabula", "test", SLACK_CHANNEL)
                .tomcatEnvironment("Sandbox", "tabula-sandbox.warwick.ac.uk", "tabula", "sandbox", env -> env
                    .triggers(new AfterSuccessfulBuildPlanTrigger().triggerByBranch("master"))
                    .notifications(
                        new Notification()
                            .type(new DeploymentFailedNotification())
                            .recipients(slackRecipient(SLACK_CHANNEL))
                    )
                )
                .productionTomcatEnvironment("Production", "tabula.warwick.ac.uk", "tabula", SLACK_CHANNEL)
                .build()
        );
    }

}
