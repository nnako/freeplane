package org.freeplane.features.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

public class OSKeyBindingManager {

    private static final UIDefaults systemDefaults;
    private static final Set<LookAndFeel> patchedLAFs = Collections.newSetFromMap(new WeakHashMap<>());

    private static final List<String> inputMapKeys = Arrays.asList(
        "TextField.focusInputMap", "TextArea.focusInputMap", "PasswordField.focusInputMap",
        "EditorPane.focusInputMap", "FormattedTextField.focusInputMap",
        "Spinner.editorInputMap", "ComboBox.ancestorInputMap", "Tree.focusInputMap",
        "List.focusInputMap", "Table.ancestorInputMap", "TableHeader.ancestorInputMap",
        "CheckBox.focusInputMap", "RadioButton.focusInputMap", "Button.focusInputMap",
        "ToggleButton.focusInputMap", "RootPane.defaultButtonWindowKeyBindings"
    );

    private static final List<String> actionMapKeys = Arrays.asList(
        "TextField.actionMap", "TextArea.actionMap", "PasswordField.actionMap",
        "EditorPane.actionMap", "FormattedTextField.actionMap",
        "Spinner.actionMap", "ComboBox.actionMap", "Tree.actionMap",
        "List.actionMap", "Table.actionMap", "TableHeader.actionMap",
        "CheckBox.actionMap", "RadioButton.actionMap", "Button.actionMap",
        "ToggleButton.actionMap", "RootPane.actionMap"
    );

    private static final List<String> editableComponentInputMaps = Arrays.asList(
        "Tree.focusInputMap", "List.focusInputMap", "Table.ancestorInputMap"
    );

    static {
        UIDefaults captured;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            captured = UIManager.getLookAndFeelDefaults();
        } catch (Exception e) {
            e.printStackTrace();
            captured = new UIDefaults();
        }
        systemDefaults = captured;
    }
    
    public static void initialize() {/* trigger static block */};

    public static void applyToCurrentLookAndFeel() {
        if(systemDefaults == null)
            return;
            
        LookAndFeel laf = UIManager.getLookAndFeel();

        if (laf.getClass().getName().equals(UIManager.getSystemLookAndFeelClassName()))
            return;

        if (patchedLAFs.contains(laf))
            return;

        UIDefaults targetDefaults = UIManager.getLookAndFeelDefaults();

        for (String key : inputMapKeys) {
            Object systemValue = systemDefaults.get(key);
            if (systemValue instanceof InputMap && systemValue instanceof UIResource) {
                Object targetValue = targetDefaults.get(key);
                if (targetValue instanceof UIResource) {
                    targetDefaults.put(key, systemValue);
                }
            }
        }

        for (String key : actionMapKeys) {
            Object systemValue = systemDefaults.get(key);
            if (systemValue instanceof ActionMap && systemValue instanceof UIResource) {
                Object targetValue = targetDefaults.get(key);
                if (targetValue instanceof UIResource) {
                    targetDefaults.put(key, systemValue);
                }
            }
        }

        ensureF2EditBinding(targetDefaults);

        patchedLAFs.add(laf);
    }

    private static void ensureF2EditBinding(UIDefaults defaults) {
        KeyStroke f2 = KeyStroke.getKeyStroke("F2");
        
        for (String key : editableComponentInputMaps) {
            InputMap im = (InputMap) defaults.get(key);
            if (im != null && (key.equals("Tree.focusInputMap") 
            || key.equals("Table.ancestorInputMap") 
            || key.equals("List.focusInputMap"))) {
                im.put(f2, "startEditing");
            }
        }
    }

    private OSKeyBindingManager() {
        // Utility class
    }
}

