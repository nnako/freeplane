package org.freeplane.plugin.markdown;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Hashtable;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.ExtendedClassLoader;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.format.ContentTypeFormat;
import org.freeplane.features.format.FormatController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.ConditionalContentTransformer;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.main.application.CommandLineOptions;
import org.freeplane.main.mindmapmode.stylemode.SModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.github.gitbucket.markedj.extension.Extension;

public class Activator implements BundleActivator {

	private static final String PREFERENCES_RESOURCE = "preferences.xml";
	static final String TOGGLE_PARSE_MARKDOWN = "parse_markdown";

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		registerMindMapModeExtension(context);
	}

	private void registerMindMapModeExtension(final BundleContext context) {
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
		props.put("mode", new String[] { MModeController.MODENAME, SModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(),
		    new IModeControllerExtensionProvider() {
			    @Override
				public void installExtension(final ModeController modeController, CommandLineOptions options) {
					MTextController textController = (MTextController) modeController.getExtension(TextController.class);
                    MarkdownRenderer markdown = new MarkdownRenderer();
                    addPlantUmlExtension(markdown);
					textController.addTextTransformer(//
							new ConditionalContentTransformer(markdown, Activator.TOGGLE_PARSE_MARKDOWN));
                    textController.addDetailContentType(MarkdownRenderer.MARKDOWN_CONTENT_TYPE);
					MNoteController noteController = (MNoteController) modeController.getExtension(NoteController.class);
					noteController.addNoteContentType(MarkdownRenderer.MARKDOWN_CONTENT_TYPE);
					modeController.getController().getExtension(FormatController.class).addPatternFormat(new ContentTypeFormat(MarkdownRenderer.MARKDOWN_FORMAT));
					if (modeController.getModeName().equals("MindMap")) {
						addPreferencesToOptionPanel();
					}
			    }

				private void addPlantUmlExtension(MarkdownRenderer markdown) {
					URL plantUml = ResourceController.getResourceController().getResource("plantuml.jar");
					if(plantUml != null) {
						try {
							@SuppressWarnings("resource")
							ExtendedClassLoader plantUmlClassLoader = new ExtendedClassLoader(
									new URL[] {plantUml}, Activator.class);
							plantUmlClassLoader.preload("org.freeplane.plugin.markdown.markedj.PlantUMLExtension");
							plantUmlClassLoader.preload("org.freeplane.plugin.markdown.markedj.PlantUMLToken");
							Extension extension = (Extension) plantUmlClassLoader.loadClass("org.freeplane.plugin.markdown.markedj.PlantUMLExtension").getConstructor().newInstance();
							markdown.addExtension(extension);
						} catch (InstantiationException | IllegalAccessException
								| IllegalArgumentException | InvocationTargetException
								| NoSuchMethodException | SecurityException | ClassNotFoundException e) {
							LogUtils.warn(e);
						}
					}
				}

				private void addPreferencesToOptionPanel() {
					final URL preferences = this.getClass().getResource(PREFERENCES_RESOURCE);
					if (preferences == null)
						throw new RuntimeException("cannot open preferences");
					final Controller controller = Controller.getCurrentController();
					MModeController modeController = (MModeController) controller.getModeController();
					modeController.getOptionPanelBuilder().load(preferences);
				}
		    }, props);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
	}
}
