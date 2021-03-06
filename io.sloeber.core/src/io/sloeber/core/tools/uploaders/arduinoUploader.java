package io.sloeber.core.tools.uploaders;

import static io.sloeber.core.common.Const.*;

import java.io.IOException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.communication.ArduinoSerial;
import io.sloeber.core.tools.ExternalCommandLauncher;

public class arduinoUploader implements IRealUpload {

    private IProject myProject;
    private ICConfigurationDescription mycConf;

    @SuppressWarnings("unused")
    arduinoUploader(IProject Project, ICConfigurationDescription confDesc, String UploadTool) {
        myProject = Project;
        mycConf = confDesc;
    }

    @Override
    public boolean uploadUsingPreferences(IFile hexFile, BoardDescription boardDescr, IProgressMonitor monitor,
            MessageConsoleStream highStream, MessageConsoleStream outStream, MessageConsoleStream errStream) {
        String uploadPort = boardDescr.getActualUploadPort();

        IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

        if (boardDescr.usesProgrammer()) {
            highStream.println(Messages.uploader_no_reset_using_programmer);
        } else if (boardDescr.isNetworkUpload()) {
            setEnvironmentvarsForAutorizedUpload(contribEnv, mycConf);
            highStream.println(Messages.uploader_no_reset_using_network);
        } else {
            uploadPort = ArduinoSerial.makeArduinoUploadready(highStream, this.myProject, mycConf, boardDescr);
        }
        String uploadRecipoeKey = boardDescr.getUploadPatternKey();
        String command = Common.getBuildEnvironmentVariable(mycConf, uploadRecipoeKey, EMPTY);
        if (command.isEmpty()) {
            Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                    uploadRecipoeKey + " : not found in the platform.txt file")); //$NON-NLS-1$
            highStream.println(Messages.uploader_Failed_to_get_upload_recipe);
            return false;
        }
        if (!uploadPort.equals(boardDescr.getUploadPort())) {
            command = command.replace(boardDescr.getUploadPort(), uploadPort);
        }

        ExternalCommandLauncher cmdLauncher = new ExternalCommandLauncher(command);

        try {
            if (cmdLauncher.launch(monitor, highStream, outStream, errStream) < 0)
                return false;
        } catch (IOException e) {
            Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, e));
            return false;
        }

        // due needs a restart after upload
        if (boardDescr.getBoardName().startsWith("Arduino Due ")) { //$NON-NLS-1$
            ArduinoSerial.reset_Arduino_by_baud_rate(boardDescr.getUploadPort(), 115200, 100);
        }

        return true;
    }

    /**
     * given a project look in the source code for the line of code that sets the
     * password;
     *
     * @param iProject
     * @return the password string or no_pwd_found_in_code if not found
     */
    @SuppressWarnings("nls")
    private String getPasswordFromCode() {
        String parameter = IndexHelper.findParameterInFunction(this.myProject, "setup", "ArduinoOTA.setPassword",
                "no_pwd_found_in_code");
        return parameter.replaceAll("\\(.*\\)", "").trim();

    }

    @SuppressWarnings("nls")
    private String getOTAPortFromCode() {
        String parameter = IndexHelper.findParameterInFunction(this.myProject, "setup", "ArduinoOTA.setPort", "8266");
        return parameter.replaceAll("\\(.*\\)", "").trim();

    }

    private void setEnvironmentvarsForAutorizedUpload(IContributedEnvironment contribEnv,
            ICConfigurationDescription configurationDescription) {
        String passWord = getPasswordFromCode();
        String OTAPort = getOTAPortFromCode();
        IEnvironmentVariable var = new EnvironmentVariable(ENV_KEY_NETWORK_AUTH, passWord);
        contribEnv.addVariable(var, configurationDescription);
        var = new EnvironmentVariable(ENV_KEY_NETWORK_PASSWORD, passWord);
        contribEnv.addVariable(var, configurationDescription);
        var = new EnvironmentVariable(ENV_KEY_NETWORK_PORT, OTAPort);
        contribEnv.addVariable(var, configurationDescription);

    }

}
