/*
 * Created on 24 May 2025
 *
 * author dimitry
 */
package org.freeplane.api.swing;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.ComponentUI;

/**
 * A wrapper around {@link javax.swing.JFileChooser} that works around security 
 * restrictions in environments with limited reflection permissions, such as 
 * Groovy scripts running under Freeplane's security manager.
 * 
 * <h2>Problem Description</h2>
 * When using {@link javax.swing.JFileChooser} in restricted environments (like Groovy scripts), 
 * FlatLaf's UI components may attempt to use reflection to apply styling, which can result in 
 * {@link java.security.AccessControlException}s with messages like:
 * <pre>
 * access denied ("java.lang.reflect.ReflectPermission" "suppressAccessChecks")
 * </pre>
 * 
 * This occurs because FlatLaf's styling system tries to access private fields using reflection
 * during UI component initialization, but the security manager denies these operations.
 * 
 * <h2>Solution</h2>
 * This class overrides the {@link #setUI(ComponentUI)} method to wrap the UI setting operation
 * in a {@link AccessController#doPrivileged(PrivilegedAction)} block. This allows the FlatLaf
 * styling operations to complete successfully by temporarily elevating privileges for the
 * necessary reflection operations.
 * 
 * <h2>Usage</h2>
 * Use this class as a drop-in replacement for {@link javax.swing.JFileChooser} in Groovy scripts:
 * <pre>
 * import org.freeplane.api.swing.JFileChooser
 * 
 * def fileChooser = new JFileChooser()
 * def result = fileChooser.showOpenDialog(null)
 * if (result == JFileChooser.APPROVE_OPTION) {
 *     def selectedFile = fileChooser.selectedFile
 *     // Process the selected file
 * }
 * </pre>
 * 
 * <h2>Security Considerations</h2>
 * This class uses {@code doPrivileged} only for the specific operation of setting the UI component,
 * which is necessary for proper FlatLaf functionality. No other privileged operations are performed,
 * maintaining the overall security model of the scripting environment.
 * 
 * @author dimitry
 * @since Freeplane 1.12.11
 * @see javax.swing.JFileChooser
 * @see AccessController#doPrivileged(PrivilegedAction)
 */
@SuppressWarnings("serial")
public class JFileChooser extends javax.swing.JFileChooser{

	/**
	 * Constructs a {@code JFileChooser} pointing to the user's default directory.
	 * This default depends on the operating system. It is typically the "My Documents"
	 * folder on Windows, and the user's home directory on Unix.
	 */
	public JFileChooser() {
		super();
	}

	/**
	 * Constructs a {@code JFileChooser} using the given path. Passing in a {@code null}
	 * string causes the file chooser to point to the user's default directory.
	 * This default depends on the operating system. It is typically the "My Documents"
	 * folder on Windows, and the user's home directory on Unix.
	 *
	 * @param currentDirectoryPath a {@code String} giving the path to a file or directory
	 */
	public JFileChooser(String currentDirectoryPath) {
		super(currentDirectoryPath);
	}

	/**
	 * Constructs a {@code JFileChooser} using the given {@code File} as the path.
	 * Passing in a {@code null} file causes the file chooser to point to the user's
	 * default directory. This default depends on the operating system. It is typically
	 * the "My Documents" folder on Windows, and the user's home directory on Unix.
	 *
	 * @param currentDirectory a {@code File} object specifying the path to a file or directory
	 */
	public JFileChooser(File currentDirectory) {
		super(currentDirectory);
	}

	/**
	 * Constructs a {@code JFileChooser} using the given {@code FileSystemView}.
	 *
	 * @param fsv a {@code FileSystemView}
	 */
	public JFileChooser(FileSystemView fsv) {
		super(fsv);
	}

	/**
	 * Constructs a {@code JFileChooser} using the given current directory and
	 * {@code FileSystemView}.
	 *
	 * @param currentDirectory a {@code File} object specifying the path to a file or directory
	 * @param fsv a {@code FileSystemView}
	 */
	public JFileChooser(File currentDirectory, FileSystemView fsv) {
		super(currentDirectory, fsv);
	}

	/**
	 * Constructs a {@code JFileChooser} using the given current directory path and
	 * {@code FileSystemView}.
	 *
	 * @param currentDirectoryPath a {@code String} giving the path to a file or directory
	 * @param fsv a {@code FileSystemView}
	 */
	public JFileChooser(String currentDirectoryPath, FileSystemView fsv) {
		super(currentDirectoryPath, fsv);
	}

	/**
	 * Sets the L&amp;F object that renders this component.
	 * 
	 * <p>This method is overridden to work around security restrictions in environments
	 * where reflection permissions are limited. The UI setting operation is wrapped in
	 * a {@link AccessController#doPrivileged(PrivilegedAction)} block to allow FlatLaf's
	 * styling operations to complete successfully.</p>
	 * 
	 * <p>This is necessary because FlatLaf attempts to use reflection to access private
	 * fields during component styling, which would otherwise fail with an
	 * {@link java.security.AccessControlException} in restricted environments.</p>
	 *
	 * @param newUI the {@code FileChooserUI} L&amp;F object
	 * @see javax.swing.UIDefaults#getUI
	 */
	@Override
	protected void setUI(ComponentUI newUI) {
		AccessController.doPrivileged((PrivilegedAction<Void>)()
				-> {
					super.setUI(newUI);
					return null;
				});
	}

}
