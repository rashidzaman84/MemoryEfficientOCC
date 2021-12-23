package org.processmining.memoryawareocc.algorithms.impl;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JTextField;



/**

 * @author Andrea Burattin
 */
public class ConfigurationDialog extends JPanel {

	private static final long serialVersionUID = -7669190410170225552L;
	private JTextField stateLimit;
	

	public int getStateLimit() {
		return Integer.parseInt(stateLimit.getText());
	}

	public void setStateLimit(JTextField stateLimit) {
		this.stateLimit = stateLimit;
	}

	
	/**
	 * Basic class constructor
	 */
	public ConfigurationDialog() {
		initComponents();
	}


	/*
	 * Graphical components initializer
	 */
	private void initComponents() {
		stateLimit = GUICustomUtils.prepareIntegerField(3);
				
		GridBagConstraints c = new GridBagConstraints();

		setOpaque(false);
		setLayout(new GridBagLayout());

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.insets = new Insets(0, 0, 15, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		add(GUICustomUtils.prepareLabel("Specify the maximum number of States to be retained by any Case."), c);

//		c.gridx = 0;
//		c.gridy = 1;
//		c.insets = new Insets(0, 0, 5, 0);
//		add(GUICustomUtils.prepareLabel("States Limitt:"), c);

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 5, 5, 0);
		add(GUICustomUtils.wrapInRoundedPanel(stateLimit), c);

	}
}
