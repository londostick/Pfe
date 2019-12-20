/*******************************************************************************
 * <copyright>
 * Copyright (c) 1995, 2015 Technische Universität Berlin. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 *******************************************************************************/
package agg.gui.help;

import javax.swing.JFrame;
import javax.swing.ImageIcon;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("serial")
public class HtmlBrowser extends JFrame {

	private HtmlPane html;

	/* The initial width and height of the frame */
//	private static int FRAME_WIDTH = 500;
//
//	private static int FRAME_HEIGHT = 650;

	private final static String HELPPATH = "agg/gui/help/";

	public HtmlBrowser(String htmlFileName, int width, int height) {
		super("AGG Help");

		/* get icon image of the AGG application */
		java.net.URL url = ClassLoader.getSystemClassLoader()
									.getResource("agg/lib/icons/AGG_ICON64.gif");
		if (url != null) {
			final ImageIcon icon = new ImageIcon(url);
			if (icon.getImage() != null) {
				setIconImage(icon.getImage());
			}
		} else {
			System.out.println("AGG_ICON64.gif not found!");
		}
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				setVisible(false);
				dispose();
			}
		});
		
		url = ClassLoader.getSystemResource(HELPPATH + htmlFileName);
//		System.out.println("URL: "+url);
		if (url != null) {
			this.html = new HtmlPane(url);
			if (!this.html.isEmpty()) {
				getContentPane().add(this.html);
				this.pack();
				setSize(width, height);
				setLocation(50, 50);
			}
		}
	}

	public HtmlBrowser(String htmlFileName) {
		super("AGG Help");

		/* get icon image of the AGG application */
		java.net.URL url = ClassLoader.getSystemClassLoader()
								.getResource("agg/lib/icons/AGG_ICON64.gif");
		if (url != null) {
			final ImageIcon icon = new ImageIcon(url);
			if (icon.getImage() != null) {
				setIconImage(icon.getImage());
			}
		} else {
			System.out.println("AGG_ICON64.gif not found!");
		}
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				setVisible(false);
				dispose();
			}
		});
	
		url = ClassLoader.getSystemResource(HELPPATH + htmlFileName);
//		System.out.println("URL: "+url);
		if (url != null) {		
			this.html = new HtmlPane(url);
			if (!this.html.isEmpty()) {
				getContentPane().add(this.html);
				this.pack();
				setLocation(50, 50);
			}
		} else {
			System.out.println(HELPPATH + htmlFileName+"   not found!");
		}
	}

	public void setPage(String url) throws java.io.IOException {
		this.html.setPage(url);
		setVisible(true);
	}

	public String toString() {
		return this.html.toString();
	}
}
