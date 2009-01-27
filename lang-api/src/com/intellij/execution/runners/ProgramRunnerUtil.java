package com.intellij.execution.runners;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunCanceledByUserException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;

/**
 * @author spleaner
 */
public class ProgramRunnerUtil {
  private static final Logger LOG = Logger.getInstance("com.intellij.execution.runners.ProgramRunnerUtil");

  private ProgramRunnerUtil() {
  }

  public static void handleExecutionError(final Project project, final RunProfile runProfile, final ExecutionException e) {
    if (e instanceof RunCanceledByUserException) {
      return;
    }

    String message = ExecutionBundle.message("error.running.configuration.with.error.error.message", runProfile.getName(), e.getMessage());
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.assertTrue(false, message);
    }
    else {
      if (message.contains("87") && e instanceof ProcessNotCreatedException) {
        final String commandLineString = ((ProcessNotCreatedException)e).getCommandLine().getCommandLineString();
        if (!commandLineString.contains("-Djava.system.class.loader") && commandLineString.length() > 1024 * 32) {
          if (Messages.showYesNoDialog(project, message + "\nCommand line is too long. In order to reduce its length classpath file can be used. Would you like to enable classpath file mode for all run configurations of your project?", ExecutionBundle.message("run.error.message.title"), Messages.getErrorIcon()) ==
              DialogWrapper.OK_EXIT_CODE) {
            PropertiesComponent.getInstance(project).setValue("dynamic.classpath", "true");
            return;
          }
        }
      }
      Messages.showErrorDialog(project, message, ExecutionBundle.message("run.error.message.title"));
    }
  }

}
