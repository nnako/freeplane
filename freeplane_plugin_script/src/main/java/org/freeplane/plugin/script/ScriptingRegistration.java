/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file author is Christian Foltin
 *  It is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.plugin.script;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.script.ScriptEngineFactory;
import javax.swing.ComboBoxEditor;

import org.apache.commons.lang.StringUtils;
import org.freeplane.core.io.xml.XMLLocalParserFactory;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.resources.components.IValidator;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.script.IScriptEditorStarter;
import org.freeplane.features.script.IScriptStarter;
import org.freeplane.main.addons.AddOnInstaller;
import org.freeplane.main.addons.AddOnsController;
import org.freeplane.main.application.ApplicationLifecycleListener;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.n3.nanoxml.IXMLParser;
import org.freeplane.n3.nanoxml.IXMLReader;
import org.freeplane.n3.nanoxml.StdXMLReader;
import org.freeplane.n3.nanoxml.XMLElement;
import org.freeplane.plugin.script.ScriptEditorPanel.IScriptModel;
import org.freeplane.plugin.script.ScriptEditorPanel.ScriptHolder;
import org.freeplane.plugin.script.addons.ManageAddOnsAction;
import org.freeplane.plugin.script.addons.ManageAddOnsDialog;
import org.freeplane.plugin.script.addons.ScriptAddOnProperties;
import org.freeplane.plugin.script.filter.ScriptConditionController;
import org.freeplane.plugin.script.help.OpenApiDocumentationAction;

class ScriptingRegistration {
	final private class ScriptModel implements IScriptModel {
		final private String mOriginalScript;
		private String mScript;

		public ScriptModel(final String pScript) {
			mScript = pScript;
			mOriginalScript = pScript;
		}

		@Override
        public int addNewScript() {
			return 0;
		}

		@Override
        public ScriptEditorWindowConfigurationStorage decorateDialog(final ScriptEditorPanel pPanel,
		                                                             final String pWindow_preference_storage_property) {
			final String marshalled = ResourceController.getResourceController().getProperty(
			    pWindow_preference_storage_property);
			return ScriptEditorWindowConfigurationStorage.decorateDialog(marshalled, pPanel);
		}

		@Override
        public void endDialog(final boolean pIsCanceled) {
			if (pIsCanceled) {
				mScript = mOriginalScript;
			}
		}

		@Override
        public Object executeScript(final int pIndex, final PrintStream pOutStream, final IFreeplaneScriptErrorHandler pErrorHandler) {
			final ModeController modeController = Controller.getCurrentModeController();
			// the script is completely in the hand of the user -> no security issues.
			final ScriptingPermissions restrictedPermissions = ScriptingPermissions.getPermissiveScriptingPermissions();
			return ScriptingEngine.executeScript(modeController.getMapController().getSelectedNode(), mScript,
			    pErrorHandler, pOutStream, null, restrictedPermissions);
		}

		@Override
        public int getAmountOfScripts() {
			return 1;
		}

		public String getScript() {
			return mScript;
		}

		@Override
        public ScriptHolder getScript(final int pIndex) {
			return new ScriptHolder("Script", mScript);
		}

		@Override
        public boolean isDirty() {
			return !StringUtils.equals(mScript, mOriginalScript);
		}

		@Override
        public void setScript(final int pIndex, final ScriptHolder pScript) {
			mScript = pScript.getScript();
		}

		@Override
        public void storeDialogPositions(final ScriptEditorPanel pPanel,
		                                 final ScriptEditorWindowConfigurationStorage pStorage,
		                                 final String pWindow_preference_storage_property) {
			pStorage.storeDialogPositions(pPanel, pWindow_preference_storage_property);
		}

		@Override
		public String getTitle() {
			return "";
		}
	}

	final private HashMap<String, Object> mScriptCookies = new HashMap<String, Object>();

	public ScriptingRegistration() {
	}

	public HashMap<String, Object> getScriptCookies() {
		return mScriptCookies;
	}

	void register(ModeController modeController, CommandLineOptions options) {
		modeController.addExtension(IScriptEditorStarter.class, new IScriptEditorStarter() {
			@Override
            public String startEditor(final String pScriptInput) {
				final ScriptModel scriptModel = new ScriptModel(pScriptInput);
				final ScriptEditorPanel scriptEditorPanel = new ScriptEditorPanel(scriptModel, false);
				scriptEditorPanel.setVisible(true);
				return scriptModel.getScript();
			}

			@Override
            public ComboBoxEditor createComboBoxEditor(Dimension minimumSize) {
	            final ScriptComboBoxEditor scriptComboBoxEditor = new ScriptComboBoxEditor();
	            if(minimumSize != null)
	            	scriptComboBoxEditor.setMinimumSize(minimumSize);
				return scriptComboBoxEditor;
            }
		});
		modeController.addExtension(IScriptStarter.class, new IScriptStarter() {
			@Override
            public void executeScript(NodeModel node, String script) {
				ScriptingEngine.executeScript(node, script);
			}
		});
		registerScriptAddOns();
		ScriptingConfiguration.register();
		ClasspathScriptCompiler scriptCompiler = new ClasspathScriptCompiler();
		boolean isSecurityManagerEnabled = System.getSecurityManager() != null;
		if(isSecurityManagerEnabled)
			System.setSecurityManager(null);
        scriptCompiler.compileScriptsOnPath(ScriptResources.getClasspath());
		if(isSecurityManagerEnabled)
			System.setSecurityManager(new InternationalizedSecurityManager());
		if(! GraphicsEnvironment.isHeadless()){
			registerGuiStuff(modeController);
			createUserScriptsDirectory();
			createInitScriptsDirectory();
			createUserLibDirectory();
		}
		registerInitScripts(options.getScriptsToExecute());
		FilterController.getCurrentFilterController().getConditionFactory().addConditionController(200,
			new ScriptConditionController());
		ScriptingPolicy.installRestrictingPolicy();
	}

	private void registerGuiStuff(ModeController modeController) {
        addPropertiesToOptionPanel();
        modeController.addAction(new ScriptEditor());
        modeController.addAction(new ExecuteScriptForAllNodes());
        modeController.addAction(new ExecuteScriptForSelectionAction());
        modeController.addAction(new OpenApiDocumentationAction());
        final ManageAddOnsAction manageAddOnsAction = new ManageAddOnsAction();
        modeController.addAction(manageAddOnsAction);
        modeController.addExtension(AddOnInstaller.class, new AddOnInstaller() {
        	@Override
            public void install(final URL url) {
        		final ManageAddOnsDialog dialog = manageAddOnsAction.getDialog();
        		dialog.install(url);
        	}
        });
        ScriptingGuiConfiguration configuration = new ScriptingGuiConfiguration();
		updateMenus(modeController, configuration);
    }

    private void addPropertiesToOptionPanel() {
        final URL preferences = this.getClass().getResource("preferences.xml");
        if (preferences == null)
            throw new RuntimeException("cannot open preferences");
        Controller.getCurrentController().addOptionValidator(new IValidator() {
            @Override
            public ValidationResult validate(Properties properties) {
                final ValidationResult result = new ValidationResult();
                final String readAccessString = properties
                    .getProperty(ScriptingPermissions.RESOURCES_EXECUTE_SCRIPTS_WITHOUT_READ_RESTRICTION);
                final String writeAccessString = properties
                .getProperty(ScriptingPermissions.RESOURCES_EXECUTE_SCRIPTS_WITHOUT_WRITE_RESTRICTION);
                final String classpath = properties.getProperty(ScriptResources.RESOURCES_SCRIPT_CLASSPATH);
                final boolean readAccess = readAccessString != null && Boolean.parseBoolean(readAccessString);
                final boolean writeAccess = writeAccessString != null && Boolean.parseBoolean(writeAccessString);
                final boolean classpathIsSet = classpath != null && classpath.length() > 0;
                if (classpathIsSet && !readAccess) {
                    result.addError(TextUtils.getText("OptionPanel.validate_classpath_needs_readaccess"));
                }
                if (writeAccess && !readAccess) {
                    result.addWarning(TextUtils.getText("OptionPanel.validate_write_without_read"));
                }
                return result;
            }
        });
        final MModeController modeController = (MModeController) Controller.getCurrentModeController();
        modeController.getOptionPanelBuilder().load(preferences);
    }

    private void updateMenus(ModeController modeController, final ScriptingGuiConfiguration configuration) {
		ScriptingMenuEntryVisitor builder = new ScriptingMenuEntryVisitor(configuration, modeController);
		modeController.addUiBuilder(Phase.ACTIONS, "userScripts", builder, EntryVisitor.ILLEGAL);
		modeController.getUserInputListenerFactory().addBuildPhaseListener(builder);
    }

    private void registerScriptAddOns() {
		File[] addonXmlFiles = AddOnsController.getController().getAddOnsDir().listFiles(new FilenameFilter() {
			@Override
            public boolean accept(File dir, String name) {
				return name.endsWith(".script.xml");
			}
		});
		final IXMLParser parser = XMLLocalParserFactory.createLocalXMLParser();
		for (File file : addonXmlFiles) {
			try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))){
				final IXMLReader reader = new StdXMLReader(inputStream);
				parser.setReader(reader);
				final ScriptAddOnProperties addOn = new ScriptAddOnProperties((XMLElement) parser.parse());
				addOn.setAddOnPropertiesFile(file);
				AddOnsController.getController().registerInstalledAddOn(addOn);
			}
			catch (final Exception e) {
				LogUtils.warn("error parsing " + file, e);
			}
		}
	}

	private void registerInitScripts(List<String> scripts) {
		FilenameFilter scriptFilenameFilter = createFilenameFilter(createScriptRegExp());
		final File[] initScriptFiles;
		if (ScriptResources.getInitScriptsDir().isDirectory()) {
			initScriptFiles = ScriptResources.getInitScriptsDir().listFiles(scriptFilenameFilter);
		} else {
			initScriptFiles = new File[] {};
		}
		if (initScriptFiles.length > 0 || scripts.size() > 0) {
			Controller.getCurrentController().addApplicationLifecycleListener(
					new ApplicationLifecycleListener() {
						@Override
						public void onStartupFinished() {
							IMapSelection selection = Controller.getCurrentController().getSelection();
							NodeModel selectedNode = selection != null ? selection.getSelected() : null;
							for (File scriptFile : initScriptFiles) {
								LogUtils.info("running init script " + scriptFile);
								try {
									ScriptingEngine.executeScript(selectedNode, scriptFile, null);
								} catch (Exception e) {
									LogUtils.warn(e);
								}
							}
							for (String scriptFile : scripts) {
								LogUtils.info("running init script " + scriptFile);
								try {
									ScriptingEngine.executeScript(selectedNode, new File(scriptFile), ScriptingPermissions.getPermissiveScriptingPermissions());
								} catch (Exception e) {
									LogUtils.warn(e);
								}
							}
						}

						@Override
						public void onApplicationStopped() {
						}
					});
		}
	}

	private void createUserScriptsDirectory() {
		createDirIfNotExists(ScriptResources.getUserScriptsDir(), "user scripts");
	}

	private void createInitScriptsDirectory() {
		createDirIfNotExists(ScriptResources.getInitScriptsDir(), "init scripts");
	}

	private void createDirIfNotExists(final File scriptDir, String what) {
		if (!scriptDir.exists()) {
			LogUtils.info("creating " + what + " directory " + scriptDir);
			scriptDir.mkdirs();
		}
	}

	private void createUserLibDirectory() {
		final File libDir = ScriptResources.getUserLibDir();
		if (!libDir.exists()) {
			LogUtils.info("creating user lib directory " + libDir);
			libDir.mkdirs();
		}
	}

	static FilenameFilter createFilenameFilter(final String regexp) {
		final FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				return name.matches(regexp);
			}
		};
		return filter;
	}

	static String createScriptRegExp() {
		final ArrayList<String> extensions = new ArrayList<String>();
		// extensions.add("clj");
		for (ScriptEngineFactory scriptEngineFactory : GenericScript
				.createScriptEngineFactories()) {
			extensions.addAll(scriptEngineFactory.getExtensions());
		}
		LogUtils.info("looking for scripts with the following endings: " + extensions);
		return ".+\\.(" + StringUtils.join(extensions, "|") + ")$";
	}
}
