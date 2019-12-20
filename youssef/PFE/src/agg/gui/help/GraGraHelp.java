/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
// $Id: GraGraHelp.java,v 1.3 2010/09/23 08:19:42 olga Exp $

package agg.gui.help;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;


/**
 * The GraGraHelp implements a smoll help module of the AGG application.
 * 
 * @author $Author: olga $
 * @version $Id: GraGraHelp.java,v 1.3 2010/09/23 08:19:42 olga Exp $
 */
@SuppressWarnings("serial")
public class GraGraHelp extends JMenu {

	private JFrame applFrame;

	public GraGraHelp() {
		super("Help");
		setMnemonic('H');

		JMenuItem mi = add(new JMenuItem("About AGG"));
		mi.setMnemonic('b');
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String ver = agg.xt_basis.Version.getID();
				int answer = msgAbout(ver);
				if (answer == JOptionPane.YES_OPTION) {
					final HtmlBrowser b = new HtmlBrowser("docu.html");
					b.setSize(500, 300);
					b.setLocation(50, 50);
					b.setVisible(true);
				}
			}
		});

		mi = add(new JMenuItem("Menu Guide"));
		mi.setMnemonic('G');
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final HtmlBrowser b = new HtmlBrowser("helptext.html");
				b.setSize(600, 700);
				b.setLocation(50, 50);
				b.setVisible(true);
			}
		});

		// mi = (JMenuItem) add(new JMenuItem("Type Editor"));
		// mi.setMnemonic('y');
		// mi.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// final HtmlBrowser b = new HtmlBrowser("TypeEditorHelp.html");
		// b.setSize(500, 300);
		// b.setLocation(50, 50);
		// b.setVisible(true);
		// }
		// });

		mi = add(new JMenuItem("Failures"));
		mi.setMnemonic('u');
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final HtmlBrowser b = new HtmlBrowser("Failures.html");
				b.setSize(500, 300);
				b.setLocation(50, 50);
				b.setVisible(true);
			}
		});

	}

	public void setParentFrame(JFrame f) {
		this.applFrame = f;
	}

	int msgAbout(String ver) {
		String v = ver;
		if (ver.indexOf("V") == 0)
			v = ver.substring(1, ver.length());
		final Object[] options = { "MORE", "CLOSE" };
		int answer = JOptionPane.showOptionDialog(this.applFrame,
				"AGG Version: "+ v
				+ "\nCopyright (c) 1995, 2015 AGG developers. All rights reserved."
				+ "\nAGG Software is made available under the terms of the"
				+ "\nEclipse Public License v1.0 which is available at"
				+ "\nhttp://www.eclipse.org/legal/.\n\n",
				"About AGG", JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE, null, options, options[1]);
		return answer;
	}

}
