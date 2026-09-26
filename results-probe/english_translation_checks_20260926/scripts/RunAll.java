import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.engine.TestExecutionResult;
import java.io.PrintWriter;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/** Scratch launcher: runs every JUnit test of package hidenosp, one line per test. */
public class RunAll {
    public static void main(String[] args) {
        LauncherDiscoveryRequest req = request().selectors(selectPackage("hidenosp")).build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summary = new SummaryGeneratingListener();
        TestExecutionListener lines = new TestExecutionListener() {
            @Override public void executionFinished(TestIdentifier id, TestExecutionResult r) {
                if (id.isTest()) {
                    String where = id.getUniqueId().replaceAll(".*\\[class:([^\\]]*)\\].*\\[method:([^(\\]]*).*", "$1#$2");
                    System.out.println(r.getStatus() + "\t" + where + "\t" + id.getDisplayName()
                        + (r.getThrowable().isPresent() ? "\t" + r.getThrowable().get() : ""));
                }
            }
        };
        launcher.execute(req, summary, lines);
        TestExecutionSummary s = summary.getSummary();
        System.out.println("SUMMARY found=" + s.getTestsFoundCount() + " succeeded=" + s.getTestsSucceededCount()
            + " failed=" + s.getTestsFailedCount() + " aborted=" + s.getTestsAbortedCount() + " skipped=" + s.getTestsSkippedCount());
        System.exit(s.getTotalFailureCount() == 0 && s.getTestsFailedCount() == 0 ? 0 : 1);
    }
}
