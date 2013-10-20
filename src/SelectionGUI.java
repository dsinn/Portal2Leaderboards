import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SelectionGUI extends JFrame {
	private static final long serialVersionUID = 1L;
	final protected String[][] spChapters = { { "The Courtesy Call", "Portal Gun" },
			{ "The Cold Boot", "Laser Stairs" }, { "The Return", "Ceiling Catapult" },
			{ "The Surprise", "Column Blocker" }, { "The Escape", "Turret Factory" }, { "The Fall", "Underground" },
			{ "The Reunion", "Propulsion Intro" }, { "The Itch", "Funnel Intro" },
			{ "The Part Where He Kills You", "Finale 2" } };
	final protected String[] coopChapters = { "Team Building", "Mass and Velocity", "Hard-Light Surfaces",
			"Excursion Funnels", "Mobility Gels", "Art Therapy" };
	final protected LinkedHashMap<String, String> lbs;
	final protected String subpage;
	final protected ModePanel mp;

	public SelectionGUI(LinkedHashMap<String, String> lbs, String subpage) {
		super("Portal 2 Leaderboards Selection");
		this.lbs = lbs;
		this.subpage = subpage;
		final CheckNode all = new CheckNode("All");
		final Object[] keys = lbs.keySet().toArray();

		String title;
		int i;
		final CheckNode sp = new CheckNode("Single-player");
		all.add(sp);
		int spIndex = 0;
		CheckNode currentChapter = new CheckNode();
		for (i = 0; !(title = lbs.get(keys[i])).matches("\\d+.+"); i++) {
			if (spIndex < spChapters.length && title.startsWith(spChapters[spIndex][1])) {
				currentChapter = new CheckNode(spChapters[spIndex++][0]);
				sp.add(currentChapter);
			}
			currentChapter.add(new CheckNode(title, keys[i].toString()));
		}
		final CheckNode coop = new CheckNode("Cooperative");
		all.add(coop);
		int mpIndex = 0;
		for (; i < keys.length; i++) {
			title = lbs.get(keys[i]);
			if (title.startsWith("01 ") && title.endsWith("Portals")) {
				currentChapter = new CheckNode(coopChapters[mpIndex++]);
				coop.add(currentChapter);
			}
			currentChapter.add(new CheckNode(title, keys[i].toString()));
		}

		final JTree tree = new JTree(all);
		tree.expandPath(new TreePath(sp.getPath()));
		tree.expandPath(new TreePath(coop.getPath()));
		tree.setCellRenderer(new CheckRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.putClientProperty("JTree.lineStyle", "Angled");
		tree.addMouseListener(new NodeSelectionListener(tree));
		final JScrollPane jsp = new JScrollPane(tree);

		final JButton button = new JButton("Submit");
		button.addActionListener(new ButtonActionListener(all, this));
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(button, BorderLayout.SOUTH);

		mp = new ModePanel();
		getContentPane().add(jsp, BorderLayout.CENTER);
		getContentPane().add(mp, BorderLayout.NORTH);
		getContentPane().add(panel, BorderLayout.SOUTH);
	}

	class NodeSelectionListener extends MouseAdapter {
		final JTree tree;

		NodeSelectionListener(JTree tree) {
			this.tree = tree;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			final int row = tree.getRowForLocation(e.getX(), e.getY());
			final TreePath path = tree.getPathForRow(row);
			if (path != null) {
				final CheckNode node = (CheckNode) path.getLastPathComponent();
				final boolean isSelected = !node.isSelected();
				node.setSelected(isSelected);

				((DefaultTreeModel) tree.getModel()).nodeChanged(node);
				tree.revalidate();
				tree.repaint();
			}
		}
	}

	class ButtonActionListener implements ActionListener {
		final private JFrame frame;
		final private CheckNode root;

		ButtonActionListener(CheckNode root, JFrame frame) {
			this.root = root;
			this.frame = frame;
		}

		public void actionPerformed(ActionEvent ev) {
			final List<String> subset = new LinkedList<String>();
			@SuppressWarnings("unchecked")
			final Enumeration<CheckNode> e = root.breadthFirstEnumeration();
			while (e.hasMoreElements()) {
				final CheckNode node = e.nextElement();
				if (node.isSelected() && node.id != null) {
					final String title = lbs.get(node.id);
					if (title.endsWith("Portals") && !mp.onlyTime.isSelected() || title.endsWith("Time")
							&& !mp.onlyPortals.isSelected()) {
						subset.add(node.id);
					}
				}
			}
			frame.setVisible(false);
			Main.outputLBs(lbs, subpage, subset);
		}
	}
}

class CheckRenderer extends JPanel implements TreeCellRenderer {
	private static final long serialVersionUID = 1L;
	protected JCheckBox check;
	protected TreeLabel label;

	public CheckRenderer() {
		setLayout(null);
		add(check = new JCheckBox());
		add(label = new TreeLabel());
		check.setBackground(UIManager.getColor("Tree.textBackground"));
		label.setForeground(UIManager.getColor("Tree.textForeground"));
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);
		setEnabled(tree.isEnabled());
		check.setSelected(((CheckNode) value).isSelected());
		label.setFont(tree.getFont());
		label.setText(stringValue);
		label.setSelected(isSelected);
		label.setFocus(hasFocus);
		return this;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d_check = check.getPreferredSize();
		Dimension d_label = label.getPreferredSize();
		return new Dimension(d_check.width + d_label.width, (d_check.height < d_label.height ? d_label.height
				: d_check.height));
	}

	@Override
	public void doLayout() {
		Dimension d_check = check.getPreferredSize();
		Dimension d_label = label.getPreferredSize();
		int y_check = 0;
		int y_label = 0;
		if (d_check.height < d_label.height) {
			y_check = (d_label.height - d_check.height) / 2;
		} else {
			y_label = (d_check.height - d_label.height) / 2;
		}
		check.setLocation(0, y_check);
		check.setBounds(0, y_check, d_check.width, d_check.height);
		label.setLocation(d_check.width, y_label);
		label.setBounds(d_check.width, y_label, d_label.width, d_label.height);
	}

	@Override
	public void setBackground(Color color) {
		if (color instanceof ColorUIResource)
			color = null;
		super.setBackground(color);
	}

	public class TreeLabel extends JLabel {
		private static final long serialVersionUID = 1L;
		boolean isSelected;
		boolean hasFocus;

		public TreeLabel() {
		}

		@Override
		public void setBackground(Color color) {
			if (color instanceof ColorUIResource)
				color = null;
			super.setBackground(color);
		}

		@Override
		public void paint(Graphics g) {
			String str;
			if ((str = getText()) != null) {
				if (0 < str.length()) {
					if (isSelected) {
						g.setColor(UIManager.getColor("Tree.selectionBackground"));
					} else {
						g.setColor(UIManager.getColor("Tree.textBackground"));
					}
					Dimension d = getPreferredSize();
					int imageOffset = 0;
					Icon currentI = getIcon();
					if (currentI != null) {
						imageOffset = currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
					}
					g.fillRect(imageOffset, 0, d.width - 1 - imageOffset, d.height);
					if (hasFocus) {
						g.setColor(UIManager.getColor("Tree.selectionBorderColor"));
						g.drawRect(imageOffset, 0, d.width - 1 - imageOffset, d.height - 1);
					}
				}
			}
			super.paint(g);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension retDimension = super.getPreferredSize();
			if (retDimension != null) {
				retDimension = new Dimension(retDimension.width + 3, retDimension.height);
			}
			return retDimension;
		}

		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}

		public void setFocus(boolean hasFocus) {
			this.hasFocus = hasFocus;
		}
	}
}

class CheckNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;
	protected int selectionMode;
	protected boolean isSelected;
	final public String id;

	public CheckNode() {
		this(null);
	}

	public CheckNode(Object userObject) {
		this(userObject, true, false, null);
	}

	public CheckNode(Object userObject, String id) {
		this(userObject, true, false, id);
	}

	public CheckNode(Object userObject, boolean allowsChildren, boolean isSelected, String id) {
		super(userObject, allowsChildren);
		this.isSelected = isSelected;
		this.id = id;
	}

	public void setSelectionMode(int mode) {
		selectionMode = mode;
	}

	public int getSelectionMode() {
		return selectionMode;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
		if (children != null) {
			@SuppressWarnings("unchecked")
			final Enumeration<CheckNode> e = children.elements();
			while (e.hasMoreElements()) {
				e.nextElement().setSelected(isSelected);
			}
		}
	}

	public boolean isSelected() {
		return isSelected;
	}
}

class ModePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	final JRadioButton onlyPortals, onlyTime, both;

	ModePanel() {
		setLayout(new GridLayout(2, 2));
		setBorder(new TitledBorder("Selection mode"));
		ButtonGroup group = new ButtonGroup();
		add(onlyPortals = new JRadioButton("Only Portals"));
		add(onlyTime = new JRadioButton("Only Time"));
		add(both = new JRadioButton("Both"));
		group.add(onlyPortals);
		group.add(onlyTime);
		group.add(both);
		both.setSelected(true);
	}
}
