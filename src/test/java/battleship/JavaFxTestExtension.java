package battleship;

import javafx.application.Platform;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts the real JavaFX application once for all tests in one JUnit test plan.
 * The root JUnit store closes the application only after the last test has run.
 * Attach to JavaFX tests with @ExtendWith(JavaFxTestExtension.class).
 *
 * IMPORTANT: do not invoke Platform.exit() from individual test classes.
 */
public final class JavaFxTestExtension implements BeforeAllCallback, BeforeEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(JavaFxTestExtension.class);

    private static volatile Session currentSession;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ensureStarted(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        ensureStarted(context);
    }

    private static void ensureStarted(ExtensionContext context) throws Exception {
        Session session = context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(
                "javafx-session", key -> new Session(), Session.class);
        currentSession = session;
        session.ensureStarted();
    }

    static GraphicInterface getLaunchedInstance() {
        Session session = currentSession;
        return session == null ? null : session.launchedInstance;
    }

    private static final class Session implements AutoCloseable {
        private GraphicInterface launchedInstance;
        private boolean attempted;
        private String startupFailure;

        synchronized void ensureStarted() throws Exception {
            if (launchedInstance != null) {
                return;
            }
            if (attempted) {
                Assumptions.assumeTrue(false, startupFailure);
                return;
            }
            attempted = true;
            // mainLaunch() starts Application.launch() on its own dedicated thread.
            // Never call it twice in the same JVM.
            GraphicInterface.mainLaunch();

            Field stageField = GraphicInterface.class.getDeclaredField("stage");
            stageField.setAccessible(true);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (System.nanoTime() < deadline) {
                CountDownLatch completed = new CountDownLatch(1);
                AtomicReference<GraphicInterface> candidate = new AtomicReference<>();
                try {
                    Platform.runLater(() -> {
                        try {
                            GraphicInterface instance = GraphicInterface.getInstance();
                            if (instance != null && stageField.get(instance) != null) {
                                candidate.set(instance);
                            }
                        } catch (IllegalAccessException ex) {
                            // Reported below if no initialized instance is found.
                        } finally {
                            completed.countDown();
                        }
                    });
                    if (completed.await(250, TimeUnit.MILLISECONDS)
                            && candidate.get() != null) {
                        launchedInstance = candidate.get();
                        return;
                    }
                } catch (IllegalStateException notReadyYet) {
                    // JavaFX is still starting on its dedicated thread.
                }
                Thread.sleep(25);
            }
            startupFailure = "JavaFX could not start within 15 seconds (display unavailable or launcher failed)";
            Assumptions.assumeTrue(false, startupFailure);
        }

        @Override
        public void close() {
            // JUnit closes root-store AutoCloseable resources once the engine has
            // completed ALL test classes in the test plan, not after each class.
            if (launchedInstance != null) {
                Platform.exit();
            }
        }
    }
}
