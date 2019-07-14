package com.paypal.butterfly.utilities.operations.pom;

import com.paypal.butterfly.extensions.api.TOExecutionResult;
import com.paypal.butterfly.extensions.api.exception.TransformationOperationException;
import com.paypal.butterfly.extensions.api.operations.ChangeOrRemoveElement;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Changes a Plugin in a Maven POM file.
 * It allows changing anything but group id and artifact id.
 * It also allows removing specific configuration, letting them
 * to have default values, or be managed by applicable.
 * <br>
 * If the plugin to be changed doesn't actually exist, it will result
 * in error
 * <br>
 * Important: no check will be done here for possible reasons to break
 * the build, like the lack of version when the plugin is not managed
 *
 * @author praveesingh
 */
public class PomChangePlugin extends AbstractArtifactPomOperation<PomChangePlugin> implements ChangeOrRemoveElement<PomChangePlugin> {

    private static final String DESCRIPTION = "Change Plugin %s:%s in POM file %s";

    private String version;
    private String extensions;
    private List<PluginExecution> executions;
    private List<Dependency> pluginDependencies;
    private Object goals;

    // Removable properties, letting them to have default values, or be managed when applicable.
    private boolean removeVersion = false;
    private boolean removeExtensions = false;
    private boolean removeExecutions = false;
    private boolean removePluginDependencies = false;
    private boolean removeGoals = false;

    // What to do if the dependency that is supposed to be changed is not present
    private IfNotPresent ifNotPresent = IfNotPresent.Fail;

    public PomChangePlugin() {
    }

    /**
     * Operation to change a plugin in a Maven POM file.
     * It allows changing anything but group id and artifact id.
     * It also allows removing specific configuration, letting them
     * to have default values, or be managed by applicable.
     * <br>
     * If the plugin to be changed doesn't actually exist, it will result
     * in error
     * <br>
     * Important: no check will be done here for possible reasons to break
     * the build, like the lack of version when the plugin is not managed
     *
     * @param groupId dependency group id
     * @param artifactId dependency artifact id
     */
    public PomChangePlugin(String groupId, String artifactId) {
        setGroupId(groupId);
        setArtifactId(artifactId);
    }

    public PomChangePlugin setVersion(String version) {
        checkForBlankString("Version", version);
        this.version = version;
        return this;
    }

    public PomChangePlugin setExtensions(String extensions) {
        checkForBlankString("Extensions", version);
        this.extensions = extensions;
        return this;
    }

    public PomChangePlugin setExecutions(List<PluginExecution> executions) {
        Map<String, PluginExecution> executionMap = new LinkedHashMap<>();
        if (executions != null) {
            for (PluginExecution exec : executions) {
                if (executionMap.containsKey(exec.getId())) {
                    throw new IllegalStateException("You cannot have two plugin executions with the same " +
                            "(or missing) <id/> elements.\nOffending execution\n\nId: '" + exec.getId()
                            + "'\nPlugin: '" + this.groupId + ":" + this.artifactId + "'\n\n");
                }
                executionMap.put(exec.getId(), exec);
            }
        }
        this.executions = executions;
        return this;
    }

    public PomChangePlugin setPluginDependencies(List<Dependency> pluginDependencies) {
        this.pluginDependencies = pluginDependencies;
        return this;
    }

    public PomChangePlugin setGoals(Object goals) {
        this.goals = goals;
        return this;
    }

    public PomChangePlugin removeVersion() {
        removeVersion = true;
        return this;
    }

    public PomChangePlugin removeExtensions() {
        removeExtensions = true;
        return this;
    }

    public PomChangePlugin removeExecutions() {
        removeExecutions = true;
        return this;
    }

    public PomChangePlugin removeDependencies() {
        removePluginDependencies = true;
        return this;
    }

    public PomChangePlugin removeGoals() {
        removeGoals = true;
        return this;
    }

    public PomChangePlugin failIfNotPresent() {
        ifNotPresent = IfNotPresent.Fail;
        return this;
    }

    public PomChangePlugin warnIfNotPresent() {
        ifNotPresent = IfNotPresent.Warn;
        return this;
    }

    public PomChangePlugin noOpIfNotPresent() {
        ifNotPresent = IfNotPresent.NoOp;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public String getExtensions() {
        return extensions;
    }

    public List<PluginExecution> getExecutions() {
        return executions;
    }

    public List<Dependency> getPluginDependencies() {
        return pluginDependencies;
    }

    public Object getGoals() {
        return goals;
    }

    public boolean isRemoveVersion() {
        return removeVersion;
    }

    public boolean isRemovExtensions() {
        return removeExtensions;
    }

    public boolean isRemoveExecutions() {
        return removeExecutions;
    }

    public boolean isRemovePluginDependencies() {
        return removePluginDependencies;
    }

    public boolean isRemoveGoals() {
        return removeGoals;
    }

    @Override
    public String getDescription() {
        return String.format(DESCRIPTION, groupId, artifactId, getRelativePath());
    }

    @Override
    @SuppressWarnings("Duplicates")
    protected TOExecutionResult pomExecution(String relativePomFile, Model model) {
        TOExecutionResult result;

        Plugin plugin = getPlugin(model, groupId, artifactId);
        if (plugin != null) {
            model.getBuild().removePlugin(plugin);

            if (removeVersion) plugin.setVersion(null); else if (version != null) plugin.setVersion(version);
            if (removeExtensions) plugin.setExecutions(null); else if (extensions != null) plugin.setExtensions(extensions);
            if (removeExecutions) plugin.setExecutions(null); else if (executions != null) plugin.setExecutions(executions);
            if (removePluginDependencies) plugin.setDependencies(null); else if (pluginDependencies != null) plugin.setDependencies(pluginDependencies);
            if (removeGoals) plugin.setGoals(null); else if (goals != null) plugin.setGoals(goals);

            plugin.setExtensions(true);

            model.getBuild().addPlugin(plugin);

            String details = String.format("Plugin %s:%s has been changed in %s", groupId, artifactId, getRelativePath());
            result = TOExecutionResult.success(this, details);
        } else {
            String message = String.format("Plugin %s:%s is not present in %s", groupId, artifactId, getRelativePath());

            switch (ifNotPresent) {
                case Warn:
                    result = TOExecutionResult.warning(this, new TransformationOperationException(message));
                    break;
                case NoOp:
                    result = TOExecutionResult.noOp(this, message);
                    break;
                case Fail:
                    // Fail is the default
                default:
                    result = TOExecutionResult.error(this, new TransformationOperationException(message));
                    break;
            }
        }

        return result;
    }

}
