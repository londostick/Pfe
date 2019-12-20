/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;

import agg.gui.icons.*;

/**
 * The tool bar of AGG application.
 * 
 * @author $Author: olga $
 * @version $ID
 */
@SuppressWarnings("serial")
public class AGGToolBar extends JToolBar {

	public AGGToolBar(int orientation) {
		super(orientation); // 0 = horizontal orientation
		setFloatable(true);
		// putClientProperty( "JToolBar.isRollover", Boolean.FALSE );
	}

	public JButton addTool(JButton b) {
		return (JButton) super.add(b);
	}

	public JButton addTool(JButton b, int indx) {
		return (JButton) super.add(b, indx);
	}
	
	public void addSeparator( Dimension size, int indx ){
        JToolBar.Separator s = new JToolBar.Separator( size );
        super.add(s, indx);
    }
	
	public JButton createTool(JButton b, String keyStr, String iconName,
			boolean enabled) {
		if (keyStr.equals("textable")) {
			final TextIcon icon = new TextIcon(iconName, enabled);
			b.setIcon(icon);
			icon.setEnabled(enabled);
		}
		b.setEnabled(enabled);
		return b; // (JButton) this.add(b);
	}

	public JButton createTool(String keyStr, String iconName, String name,
			String command, ActionListener l, boolean enabled) {
		if (keyStr.equals("imageable")) {
			ImageIcon image = null;
			if (ClassLoader.getSystemResource("agg/lib/icons/" + iconName
					+ ".gif") != null) {
				image = new ImageIcon(
						ClassLoader.getSystemResource("agg/lib/icons/"
								+ iconName + ".gif"));
			}
			else if (ClassLoader.getSystemResource("agg/lib/icons/" + iconName
					+ ".jpg") != null) {
				image = new ImageIcon(
						ClassLoader.getSystemResource("agg/lib/icons/"
								+ iconName + ".jpg"));
			}
			else if (ClassLoader.getSystemResource("agg/lib/icons/" + iconName
					+ ".png") != null) {
				image = new ImageIcon(
						ClassLoader.getSystemResource("agg/lib/icons/"
								+ iconName + ".png"));
			}
			if (image != null) {
				final JButton b = new JButton(image);
				b.setToolTipText(name);
				b.setMargin(new Insets(0, 0, 0, 0));
				b.getAccessibleContext().setAccessibleName(name);
				b.setActionCommand(command);
				b.addActionListener(l);
				b.setEnabled(enabled);
				return b;
			}
		} else if (keyStr.equals("textable")) {
			final TextIcon icon = new TextIcon(iconName, enabled);
			final JButton b = createButton(icon, name, command, l, enabled);
			icon.setEnabled(enabled);
			return b;
		} else if (keyStr.equals("iconable")) {
			if (iconName.equals("SelectAllIcon")) {
				final SelectAllIcon icon = new SelectAllIcon(Color.green);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("SelectNodeTypeIcon")) {
				final SelectNodeTypeIcon icon = new SelectNodeTypeIcon(
						Color.green);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("SelectArcTypeIcon")) {
				final SelectArcTypeIcon icon = new SelectArcTypeIcon(
						Color.green);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeselectAllIcon")) {
				final DeselectAllIcon icon = new DeselectAllIcon();
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewGraGraIcon")) {
				final NewGraGraIcon icon = new NewGraGraIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewTypeGraphIcon")) {
				final NewTypeGraphIcon icon = new NewTypeGraphIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewGraphIcon")) {
				final NewGraphIcon icon = new NewGraphIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewRuleIcon")) {
				final NewRuleIcon icon = new NewRuleIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewAtomicIcon")) {
				final NewAtomicIcon icon = new NewAtomicIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewConclusionIcon")) {
				final NewConclusionIcon icon = new NewConclusionIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewConstraintIcon")) {
				final NewConstraintIcon icon = new NewConstraintIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewNestedACIcon")) {
				final NewNestedACIcon icon = new NewNestedACIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewNACIcon")) {
				final NewNACIcon icon = new NewNACIcon(Color.red);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("NewPACIcon")) {
				final NewPACIcon icon = new NewPACIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteGraGraIcon")) {
				final DeleteGraGraIcon icon = new DeleteGraGraIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteRuleIcon")) {
				final DeleteRuleIcon icon = new DeleteRuleIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteNestedACIcon")) {
				final DeleteNestedACIcon icon = new DeleteNestedACIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteNACIcon")) {
				final DeleteNACIcon icon = new DeleteNACIcon(Color.red);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeletePACIcon")) {
				final DeletePACIcon icon = new DeletePACIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteTypeGraphIcon")) {
				final DeleteTypeGraphIcon icon = new DeleteTypeGraphIcon(
						Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteGraphIcon")) {
				final DeleteGraphIcon icon = new DeleteGraphIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteAtomicIcon")) {
				final DeleteAtomicIcon icon = new DeleteAtomicIcon(Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;

			} else if (iconName.equals("DeleteConclusionIcon")) {
				final DeleteConclusionIcon icon = new DeleteConclusionIcon(
						Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("DeleteConstraintIcon")) {
				final DeleteConstraintIcon icon = new DeleteConstraintIcon(
						Color.blue);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("MatchIcon")) {
				final MatchIcon icon = new MatchIcon(Color.black);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("CompletionIcon")) {
				final CompletionIcon icon = new CompletionIcon(Color.black);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("StepIcon")) {
				final StepIcon icon = new StepIcon(Color.red);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("StartIcon")) {
				final StartIcon icon = new StartIcon(Color.red);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("StopIcon")) {
				final StopIcon icon = new StopIcon(Color.red);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			} else if (iconName.equals("StepBackIcon")) {
				final StepBackIcon icon = new StepBackIcon(Color.red);
				final JButton b = createButton(icon, name, command, l, enabled);
				icon.setEnabled(enabled);
				return b;
			}
		}
		return null;
	}

	public JButton createButton(Icon icon, String name, String command,
			ActionListener l, boolean enabled) {
		// JButton b = (JButton) this.add(new JButton(icon));
		final JButton b = new JButton(icon);
		b.setToolTipText(name);
		b.setMargin(new Insets(0, 0, 0, 0));
		b.getAccessibleContext().setAccessibleName(name);
		b.setActionCommand(command);
		b.addActionListener(l);
		b.setEnabled(enabled);
		return b;
	}

	public JButton getButton(String name) {
		for (int i=0; i<this.getComponentCount(); i++) {
			Object b = this.getComponent(i);
			if (b instanceof JButton) {
				if (((JButton)b).getText().equals(name)) {
					return (JButton)b;
				}
			}
		}
		return null;
	}
	
	public JButton getButton(String name, String command) {
		for (int i=0; i<this.getComponentCount(); i++) {
			Object b = this.getComponent(i);
			if (b instanceof JButton) {
				if (((JButton)b).getText().equals(name) 
						&& ((JButton)b).getActionCommand().equals(command)) {
					return (JButton)b;
				}
			}
		}
		return null;
	}
	
} // AGGToolBar
