package warehouse.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import warehouse.agents.ShelfAgent;

/**
 * @author Bastian Mager <bastian.mager.2010w@informatik.h-brs.de>
 */
public class ShelfStockGUI extends JFrame {

	private static final long serialVersionUID = 1L;

	private static ShelfStockGUI instance;
	private static Map<ShelfAgent, ShelfPanel> shelfs;
	private JPanel listPane;

	static {
		instance = new ShelfStockGUI();
		shelfs = new HashMap<ShelfAgent, ShelfPanel>();
	}

	private ShelfStockGUI() {
		setTitle("Warehouse - ShelfStock");
		setSize(300, 200);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));

		JScrollPane scrollPane = new JScrollPane(listPane);
		add(scrollPane);
		setResizable(false);
		setVisible(true);
	}

	public static void register(ShelfAgent shelf) {

		ShelfPanel shelfPanel = new ShelfPanel(shelf);
		shelfs.put(shelf, shelfPanel);
		synchronized (instance) {
			instance.listPane.add(shelfPanel);
			instance.listPane.updateUI();
		}

	}

	public static void update(ShelfAgent shelf) {
		ShelfPanel shelfPanel = shelfs.get(shelf);
		shelfPanel.updateInventory(shelf.inventory);
	}

	private static class ShelfPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private Map<String, JLabel> quantityLabels;

		public ShelfPanel(ShelfAgent shelf) {
			quantityLabels = new HashMap<String, JLabel>();
			setBorder(new TitledBorder(new LineBorder(Color.gray),
					shelf.getLocalName()));
			setLayout(new GridLayout(shelf.inventory.size(), 2));
			setSize(new Dimension(250, 15 * shelf.inventory.size()));
			initInventory(shelf.inventory);
			setVisible(true);
		}

		public void updateInventory(Map<String, Integer> inventory) {
			for (String item : inventory.keySet()) {
				JLabel itemLabel = quantityLabels.get(item);
				itemLabel.setText(inventory.get(item).toString());
				if (inventory.get(item) < 1) {
					itemLabel.setForeground(Color.RED);
				} else {
					itemLabel.setForeground(Color.BLACK);
				}
			}
			updateUI();
		}

		public void initInventory(Map<String, Integer> inventory) {
			GridBagConstraints c = new GridBagConstraints();

			for (String item : inventory.keySet()) {
				JLabel itemTitle = new JLabel(item);
				JLabel itemQuantity = new JLabel(inventory.get(item).toString());

				c.gridx++;
				c.gridy = 0;
				add(itemTitle, c);
				add(itemQuantity, c);
				quantityLabels.put(item, itemQuantity);
			}
		}

	}

}
