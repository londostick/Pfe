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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;

@SuppressWarnings("serial")
public class HtmlPane extends JScrollPane implements HyperlinkListener {

	JEditorPane html;

	private boolean isEmpty;

	public HtmlPane(URL u) {
		super();
		// System.out.println("URL: "+u);
		this.isEmpty = false;
		try {
			this.html = new JEditorPane(u);
			this.html.setEditable(false);
			this.html.addHyperlinkListener(this);
			getViewport().add(this.html);
			this.setAutoscrolls(true);
			this.setWheelScrollingEnabled(true);
			this.validate();
		} catch (IOException e) {
			this.isEmpty = true;
			JOptionPane.showMessageDialog(null, "Exception: \n" + e);
		}
	}

	public HtmlPane(String u) {
		super();
//		 System.out.println("URL: "+u);
		this.isEmpty = false;
		try {
			this.html = new JEditorPane(u);
			this.html.setEditable(false);
			this.html.addHyperlinkListener(this);
			getViewport().add(this.html);
			this.setAutoscrolls(true);
			this.setWheelScrollingEnabled(true);
			this.validate();
		} catch (IOException e) {
			this.isEmpty = true;
			JOptionPane.showMessageDialog(null, "Exception: \n" + e);
		}
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(500, 300);
	}

	public boolean isEmpty() {
		return this.isEmpty;
	}

	public void setPage(String url) throws java.io.IOException {
		this.html.setPage(url);
	}

	public String toString() {
		return (new Integer(hashCode())).toString();
	}

	/**
	 * Notification of a change relative to a hyperlink.
	 */
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			linkActivated(e.getURL());
		}
	}

	/**
	 * Follows the reference in an link. The given url is the requested
	 * reference. By default this calls <a href="#setPage">setPage</a>, and if
	 * an exception is thrown the original previous document is restored and a
	 * beep sounded. If an attempt was made to follow a link, but it represented
	 * a malformed url, this method will be called with a null argument.
	 * 
	 * @param u
	 *            the URL to follow
	 */
	protected void linkActivated(URL u) {
		// System.out.println("HTMLPane.linkActivated...");
		Cursor c = this.html.getCursor();
		Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		this.html.setCursor(waitCursor);
		SwingUtilities.invokeLater(new PageLoader(u, c));
	}

	/**
	 * temporary class that loads synchronously (although later than the request
	 * so that a cursor change can be done).
	 */
	class PageLoader implements Runnable {

		PageLoader(URL u, Cursor c) {
			this.url = u;
			this.cursor = c;
		}

		public void run() {
			if (this.url == null) {
				// restore the original cursor
				HtmlPane.this.html.setCursor(this.cursor);

				// PENDING(prinz) remove this hack when
				// automatic validation is activated.
				Container parent = HtmlPane.this.html.getParent();
				parent.repaint();
			} else {
				Document doc = HtmlPane.this.html.getDocument();
				try {
					HtmlPane.this.html.setPage(this.url);
				} catch (IOException ioe) {
					HtmlPane.this.html.setDocument(doc);
					getToolkit().beep();
				} finally {
					// schedule the cursor to revert after
					// the paint has happended.
					this.url = null;
					SwingUtilities.invokeLater(this);
				}
			}
		}

		private URL url;

		private Cursor cursor;
	} // class PageLoader

}
