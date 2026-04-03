import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class RestaurantQueueSystem {

    public static void main(String[] args) {
        // Ensure UI created on EDT
        SwingUtilities.invokeLater(MainMenu::new);
    }

    // ---------- Global Data ----------
    static class Globals {
        static final int TABLE_COUNT = 10;
        static final Queue<Order> cashierToChef = new LinkedList<>();
        static final Queue<Order> chefToWaiter = new LinkedList<>();
        static final Queue<Order> waitingOrders = new LinkedList<>();
        static final Table[] tables = new Table[TABLE_COUNT];

        static {
            for (int i = 0; i < TABLE_COUNT; i++) tables[i] = new Table(i + 1);
        }

        // simple food catalog (name -> price)
        static final LinkedHashMap<String, Double> FOOD_CATALOG = new LinkedHashMap<>();
        static {
			FOOD_CATALOG.put("Dinuguan", 99.00);
			FOOD_CATALOG.put("Pork BBQ", 99.00);
			FOOD_CATALOG.put("Pork Sisig", 99.00);
			FOOD_CATALOG.put("Creamy Bangus Sisig", 99.00);
			FOOD_CATALOG.put("Porkchop", 99.00);
			FOOD_CATALOG.put("Daing na Bangus", 99.00);
			FOOD_CATALOG.put("Fried Chicken", 99.00);
			FOOD_CATALOG.put("Pork Liempo Sinigang", 159.00);
			FOOD_CATALOG.put("Boneless Bangus", 159.00);
			FOOD_CATALOG.put("Sinigang", 159.00);
			FOOD_CATALOG.put("Shrimp Sinigang", 159.00);
			FOOD_CATALOG.put("Beef Brocolli", 159.00);
			FOOD_CATALOG.put("Beef Caldereta", 159.00);
			FOOD_CATALOG.put("Grilled Liempo", 159.00);
			FOOD_CATALOG.put("Grilled Quarter Leg", 159.00);
			FOOD_CATALOG.put("Tapa", 88.00);
			FOOD_CATALOG.put("Pork Tonkatsu", 88.00);
			FOOD_CATALOG.put("Pork Chop", 78.00);
			FOOD_CATALOG.put("Longganisa", 68.00);
			FOOD_CATALOG.put("Tocino", 68.00);
			FOOD_CATALOG.put("Hotdog", 48.00);
			FOOD_CATALOG.put("Sinigang na Hipon", 300.00);
			FOOD_CATALOG.put("Sinigang na Boneless Bangus", 350.00);
			FOOD_CATALOG.put("Sinigang na Pork", 350.00);
			FOOD_CATALOG.put("Pinangat (Individual)", 99.00);
			FOOD_CATALOG.put("Pinangat (Family)", 150.00);
			FOOD_CATALOG.put("Chicken Wings (4pcs)", 99.00);
			FOOD_CATALOG.put("Chicken Wings (8pcs)", 159.00);
        }
    }

    // ---------- Models ----------
    static class Order {
        private static int COUNTER = 1;
        final int orderNumber;
        int tableNumber; // 0 = waiting/unassigned
        final List<String> items;
        final double total;

        Order(int tableNumber, List<String> items, double total) {
            this.orderNumber = COUNTER++;
            this.tableNumber = tableNumber;
            this.items = new ArrayList<>(items);
            this.total = total;
        }

        String compactString() {
            String tableTxt = (tableNumber == 0) ? "WAITING" : "Table " + tableNumber;
            return "#" + orderNumber + " → [" + tableTxt + "] " + items.size() + " item(s) — ₱" + fmt(total);
        }

        @Override
        public String toString() {
            String tableTxt = (tableNumber == 0) ? "WAITING" : "Table " + tableNumber;
            return "#" + orderNumber + " → [" + tableTxt + "] " + String.join(", ", items) + " — ₱" + fmt(total);
        }
    }

    static class Table {
        final int number;
        boolean occupied = false; // assigned
        boolean served = false;   // served (red)
        Order assignedOrder = null;

        Table(int number) { this.number = number; }
    }

    // ---------- Utilities ----------
    private static String fmt(double v) {
        return new DecimalFormat("#,##0.00").format(v);
    }

    // ---------- Main Menu ----------
    static class MainMenu extends JFrame {
        MainMenu() {
            setTitle("Pugon Garden's Restaurant Queue System");
            setSize(420, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(12, 12, 12, 12);
            gbc.fill = GridBagConstraints.HORIZONTAL;
			
			JPanel topPanel = new JPanel();
			topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
			topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

			// Logo
			ImageIcon logoIcon = new ImageIcon("gardenlogoasset2.jpg");
			Image img = logoIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
			logoIcon = new ImageIcon(img);
			JLabel logoLabel = new JLabel(logoIcon);
			logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

			// Title
			JLabel title = new JLabel("Pugon Garden's Restaurant", SwingConstants.CENTER);
			title.setFont(new Font("SansSerif", Font.BOLD, 24));
			title.setAlignmentX(Component.CENTER_ALIGNMENT);

			// Add both to top panel
			topPanel.add(logoLabel);
			topPanel.add(Box.createRigidArea(new Dimension(0, 10))); // small spacing
			topPanel.add(title);

			// Add top panel to the frame
			add(topPanel, gbc);

            JButton cashier = new JButton("Cashier");
            JButton chef = new JButton("Chef / Cook");
            JButton waiter = new JButton("Waiter / Server");

            cashier.addActionListener(e -> new CashierWindow());
            chef.addActionListener(e -> new ChefWindow());
            waiter.addActionListener(e -> new WaiterWindow());

            gbc.gridy = 1; add(cashier, gbc);
            gbc.gridy = 2; add(chef, gbc);
            gbc.gridy = 3; add(waiter, gbc);

            setVisible(true);
        }
    }

	// ---------- Cashier Window ----------
	static class CashierWindow extends JFrame {
		private final DefaultListModel<String> catalogModel = new DefaultListModel<>();
		private final DefaultListModel<String> cartModel = new DefaultListModel<>();
		private final DefaultListModel<String> waitingModel = new DefaultListModel<>(); // NEW
		private final JLabel totalLabel = new JLabel("Total: ₱0.00");
		private final java.util.List<String> cartItems = new ArrayList<>();
		private double cartTotal = 0.0;

		private final JButton[] tableButtons = new JButton[Globals.TABLE_COUNT];
		private JList<String> waitingList; // make waiting list a field so refresh can restore selection

		CashierWindow() {
			setTitle("Cashier Panel");
			setSize(960, 600);
			setLocationRelativeTo(null);
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setLayout(new BorderLayout(10,10));
			((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

			// Top: header
			JLabel header = new JLabel("Cashier", SwingConstants.LEFT);
			header.setFont(new Font("SansSerif", Font.BOLD, 18));
			add(header, BorderLayout.NORTH);

			// Left: catalog and add/remove
			JPanel left = new JPanel(new BorderLayout(6,6));
			JList<String> catalogList = new JList<>(catalogModel);
			catalogList.setVisibleRowCount(10);
			JScrollPane catScroll = new JScrollPane(catalogList);
			catScroll.setBorder(BorderFactory.createTitledBorder("Food Catalog (click to add)"));
			left.add(catScroll, BorderLayout.CENTER);

			// populate catalog
			for (Map.Entry<String, Double> e : Globals.FOOD_CATALOG.entrySet()) {
				catalogModel.addElement(e.getKey() + " — ₱" + fmt(e.getValue()));
			}

			JButton addBtn = new JButton("Add Selected");
			addBtn.addActionListener(e -> {
				int idx = catalogList.getSelectedIndex();
				if (idx >= 0) {
					String line = catalogModel.get(idx);
					String name = line.split(" — ")[0];
					cartItems.add(name);
					cartModel.addElement(name + " — ₱" + fmt(Globals.FOOD_CATALOG.get(name)));
					recalcTotal();
				} else {
					JOptionPane.showMessageDialog(this, "Select a food to add.");
				}
			});

			left.add(addBtn, BorderLayout.SOUTH);
			add(left, BorderLayout.WEST);

			// Center: cart list and controls
			JPanel center = new JPanel(new BorderLayout(6,6));
			JList<String> cartList = new JList<>(cartModel);
			cartList.setBorder(BorderFactory.createTitledBorder("Current Order / Cart"));
			center.add(new JScrollPane(cartList), BorderLayout.CENTER);

			JPanel cartControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JButton removeBtn = new JButton("Remove Selected");
			removeBtn.addActionListener(e -> {
				int s = cartList.getSelectedIndex();
				if (s >= 0) {
					cartModel.remove(s);
					cartItems.remove(s);
					recalcTotal();
				}
			});

			cartControls.add(removeBtn);
			cartControls.add(totalLabel);
			center.add(cartControls, BorderLayout.SOUTH);
			add(center, BorderLayout.CENTER);

			// Right: table grid + waiting queue below
			JPanel right = new JPanel(new BorderLayout(6,6));
			JPanel grid = new JPanel(new GridLayout(2,5,8,8));
			for (int i = 0; i < Globals.TABLE_COUNT; i++) {
				JButton tb = new JButton("<html><b>Table " + (i+1) + "</b><br/>(VACANT)</html>");
				tb.setOpaque(true);
				tb.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
				tb.setBackground(new Color(0x4CAF50));
				final int tn = i + 1;
				tb.addActionListener(ev -> {
					// If there's a selected waiting order, assign that one; otherwise assign head of queue
					if (!Globals.waitingOrders.isEmpty() && !Globals.tables[tn-1].occupied) {
						Order chosen = null;
						int sel = waitingList.getSelectedIndex();
						if (sel >= 0) {
							// parse order number from selected string (starts with #<num>)
							String selText = waitingModel.getElementAt(sel);
							int onum = parseOrderNumberFromListString(selText);
							if (onum != -1) chosen = findAndRemoveWaitingByOrderNumber(onum);
						}
						if (chosen == null) {
							// fallback: poll first waiting order
							chosen = Globals.waitingOrders.poll();
							if (chosen != null) removeWaitingFromModel(chosen.orderNumber);
						} else {
							// we already removed chosen inside findAndRemove...
							removeWaitingFromModel(chosen.orderNumber);
						}

						if (chosen != null) {
							chosen.tableNumber = tn;
							Globals.cashierToChef.add(chosen);
							Globals.tables[tn-1].occupied = true;
							Globals.tables[tn-1].served = false;
							Globals.tables[tn-1].assignedOrder = chosen;
							JOptionPane.showMessageDialog(this, "Assigned waiting order " + chosen.orderNumber + " to Table " + tn);
							refreshTablesAndWaiting(); // update both table buttons and waiting UI
						}
					} else {
						// Show table info
						Table t = Globals.tables[tn-1];
						String msg = "Table " + tn + "\nOccupied: " + t.occupied + "\nServed: " + t.served;
						if (t.assignedOrder != null) msg += "\nAssigned: " + t.assignedOrder.compactString();
						JOptionPane.showMessageDialog(this, msg);
					}
				});
				tableButtons[i] = tb;
				grid.add(tb);
			}
			right.add(grid, BorderLayout.NORTH);

			// Waiting queue UI (below the table grid)
			waitingList = new JList<>(waitingModel);
			waitingList.setBorder(BorderFactory.createTitledBorder("Waiting Orders (no table available)"));
			waitingList.setVisibleRowCount(6);
			JScrollPane waitingScroll = new JScrollPane(waitingList);
			waitingScroll.setPreferredSize(new Dimension(240, 180));
			right.add(waitingScroll, BorderLayout.CENTER);

			JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
			legend.add(legendLabel(new Color(0x4CAF50), "VACANT"));
			legend.add(legendLabel(new Color(0xF7DC6F), "ASSIGNED (not served)"));
			legend.add(legendLabel(new Color(0xE74C3C), "SERVED / OCCUPIED"));
			right.add(legend, BorderLayout.SOUTH);

			add(right, BorderLayout.EAST);

			// Bottom: checkout
			JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JButton checkoutBtn = new JButton("Checkout (assign or waiting)");
			checkoutBtn.addActionListener(e -> doCheckout());
			bottom.add(checkoutBtn);
			add(bottom, BorderLayout.SOUTH);

			// Timer to refresh table buttons and waiting list (uses explicit javax.swing.Timer)
			javax.swing.Timer timer = new javax.swing.Timer(400, e -> refreshTablesAndWaiting());
			timer.start();

			setVisible(true);
		}

		private JLabel legendLabel(Color c, String text) {
			JLabel l = new JLabel(" " + text + " ");
			l.setOpaque(true);
			l.setBackground(c);
			l.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			return l;
		}

		private void recalcTotal() {
			cartTotal = 0;
			for (String name : cartItems) cartTotal += Globals.FOOD_CATALOG.getOrDefault(name, 0.0);
			totalLabel.setText("Total: ₱" + fmt(cartTotal));
		}

		private void doCheckout() {
			if (cartItems.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Cart is empty.");
				return;
			}
			// Check first for a vacant table
			int vacant = -1;
			for (int i = 0; i < Globals.TABLE_COUNT; i++) {
				if (!Globals.tables[i].occupied) { vacant = i + 1; break; }
			}

			Order order;
			if (vacant != -1) {
				// assign immediately
				order = new Order(vacant, cartItems, cartTotal);
				Globals.cashierToChef.add(order);
				Globals.tables[vacant - 1].occupied = true;
				Globals.tables[vacant - 1].served = false;
				Globals.tables[vacant - 1].assignedOrder = order;
				JOptionPane.showMessageDialog(this, "Order " + order.orderNumber + " assigned to Table " + vacant + " and queued for cooking.");
			} else {
				// add to waiting queue
				order = new Order(0, cartItems, cartTotal);
				Globals.waitingOrders.add(order);
				// update waiting list UI
				waitingModel.addElement(order.compactString());
				JOptionPane.showMessageDialog(this, "No vacant table — Order " + order.orderNumber + " added to waiting queue.");
			}

			// clear cart
			cartItems.clear();
			cartModel.clear();
			recalcTotal();
			refreshTablesAndWaiting();
		}

    // Rebuild table buttons and waiting list (keeps UI consistent)
    private void refreshTablesAndWaiting() {
        // refresh table buttons
        for (int i = 0; i < Globals.TABLE_COUNT; i++) {
            Table t = Globals.tables[i];
            JButton b = tableButtons[i];
            if (!t.occupied) {
                b.setText("<html><b>Table " + (i+1) + "</b><br/>(VACANT)</html>");
                b.setBackground(new Color(0x4CAF50));
            } else if (!t.served) {
                String label = (t.assignedOrder != null) ? ("Order " + t.assignedOrder.orderNumber) : "ASSIGNED";
                b.setText("<html><b>Table " + (i+1) + "</b><br/>(" + label + ")</html>");
                b.setBackground(new Color(0xF7DC6F));
            } else {
                String label = (t.assignedOrder != null) ? ("Order " + t.assignedOrder.orderNumber) : "OCCUPIED";
                b.setText("<html><b>Table " + (i+1) + "</b><br/>(" + label + ")</html>");
                b.setBackground(new Color(0xE74C3C));
            }
        }

        // rebuild waiting list model to reflect Globals.waitingOrders
        // preserve selection
        int sel = waitingList.getSelectedIndex();
        waitingModel.clear();
        for (Order w : Globals.waitingOrders) {
            waitingModel.addElement(w.compactString());
        }
        if (sel >= 0 && sel < waitingModel.size()) waitingList.setSelectedIndex(sel);
    }

    // Remove waiting item from waitingModel by order number (used when assigning from waiting)
    private void removeWaitingFromModel(int orderNumber) {
        for (int i = 0; i < waitingModel.size(); i++) {
            String s = waitingModel.getElementAt(i);
            if (s.startsWith("#" + orderNumber + " ")) {
                waitingModel.remove(i);
                return;
            }
        }
    }

    // Parse an order number from a list string like "#12 → [WAITING] ..."
    private int parseOrderNumberFromListString(String s) {
        try {
            if (s.startsWith("#")) {
                int space = s.indexOf(' ');
                if (space > 1) {
                    String num = s.substring(1, space);
                    return Integer.parseInt(num);
                }
            }
        } catch (Exception ex) { /* ignore */ }
        return -1;
    }

    // Find and remove a waiting order by orderNumber from Globals.waitingOrders, returns it (or null)
    private Order findAndRemoveWaitingByOrderNumber(int orderNumber) {
        Order found = null;
        Iterator<Order> it = Globals.waitingOrders.iterator();
        while (it.hasNext()) {
            Order o = it.next();
            if (o.orderNumber == orderNumber) {
                found = o;
                it.remove();
                break;
            }
        }
        return found;
    }
}


    // ---------- Chef Window ----------
    static class ChefWindow extends JFrame {
        private final DefaultListModel<String> pendingModel = new DefaultListModel<>();
        private final JList<String> pendingList = new JList<>(pendingModel);

        ChefWindow() {
            setTitle("Chef Panel");
            setSize(480, 520);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout(8,8));
            ((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JLabel header = new JLabel("Chef / Cook", SwingConstants.LEFT);
            header.setFont(new Font("SansSerif", Font.BOLD, 18));
            add(header, BorderLayout.NORTH);

            pendingList.setBorder(BorderFactory.createTitledBorder("Pending Orders "));
            add(new JScrollPane(pendingList), BorderLayout.CENTER);

            JButton cookBtn = new JButton("Cook / Dequeue Selected");
            cookBtn.addActionListener(e -> cookSelected());
            add(cookBtn, BorderLayout.SOUTH);

            javax.swing.Timer timer = new javax.swing.Timer(400, e -> refreshPending());
            timer.start();

            setVisible(true);
        }

		void refreshPending() {
			int selected = pendingList.getSelectedIndex(); // remember selection

			pendingModel.clear();
			for (Order o : Globals.cashierToChef) {
				pendingModel.addElement(o.toString());
			}

			// restore selection if possible
			if (selected >= 0 && selected < pendingModel.size()) {
				pendingList.setSelectedIndex(selected);
			}
		}


        private void cookSelected() {
            int sel = pendingList.getSelectedIndex();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select a pending order to cook.");
                return;
            }
            // find order by matching displayed string to queue element
            Order target = null;
            int idx = 0;
            for (Order o : Globals.cashierToChef) {
                if (idx == sel) { target = o; break; }
                idx++;
            }
            if (target != null) {
                Globals.cashierToChef.remove(target);
                Globals.chefToWaiter.add(target);
                JOptionPane.showMessageDialog(this, "Order " + target.orderNumber + " cooked and moved to cooked queue.");
                refreshPending();
            }
        }
    }

    // ---------- Waiter Window ----------
    static class WaiterWindow extends JFrame {
        private final DefaultListModel<String> cookedModel = new DefaultListModel<>();
        private final JList<String> cookedList = new JList<>(cookedModel);
        private final JButton[] tableButtons = new JButton[Globals.TABLE_COUNT];

        WaiterWindow() {
            setTitle("Waiter / Server Panel");
            setSize(980, 600);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(10,10));
            ((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JLabel header = new JLabel("Waiter / Server", SwingConstants.LEFT);
            header.setFont(new Font("SansSerif", Font.BOLD, 18));
            add(header, BorderLayout.NORTH);

            // Left: cooked orders list
            JPanel left = new JPanel(new BorderLayout(6,6));
            cookedList.setBorder(BorderFactory.createTitledBorder("Cooked Orders "));
            left.add(new JScrollPane(cookedList), BorderLayout.CENTER);
            JButton serveBtn = new JButton("Serve Selected Order");
            serveBtn.addActionListener(e -> serveSelected());
            left.add(serveBtn, BorderLayout.SOUTH);
            add(left, BorderLayout.WEST);

			// Right: table grid (click to clean)
			JPanel right = new JPanel(new BorderLayout(6,6));

			// ---- NEW: Label above the grid ----
			JLabel cleanLabel = new JLabel("Click a table to clean it", SwingConstants.CENTER);
			cleanLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
			cleanLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			right.add(cleanLabel, BorderLayout.NORTH);

			// ---- Table Grid ----
			JPanel grid = new JPanel(new GridLayout(2,5,8,8));

			for (int i = 0; i < Globals.TABLE_COUNT; i++) {
				final int tn = i + 1;

				JButton tb = new JButton("<html><b>Table " + tn + "</b><br/>(VACANT)</html>");
				tb.setOpaque(true);
				tb.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
				tb.setBackground(new Color(0x4CAF50));

				tb.addActionListener(e -> {
					// Clean this table when clicked
					if (Globals.tables[tn - 1].occupied) {
						Globals.tables[tn - 1].occupied = false;
						Globals.tables[tn - 1].served = false;
						Globals.tables[tn - 1].assignedOrder = null;

						// auto assign waiting order if available
						autoAssignWaiting();

						JOptionPane.showMessageDialog(this, 
							"Table " + tn + " cleaned (now VACANT).");
					} else {
						JOptionPane.showMessageDialog(this, 
							"Table " + tn + " is already vacant.");
					}
				});

				tableButtons[i] = tb;
				grid.add(tb);
			}

			// Put the grid in the CENTER
			right.add(grid, BorderLayout.CENTER);


            JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
            legend.add(legendLabel(new Color(0x4CAF50), "VACANT"));
            legend.add(legendLabel(new Color(0xF7DC6F), "ASSIGNED (not served)"));
            legend.add(legendLabel(new Color(0xE74C3C), "SERVED / OCCUPIED"));
            right.add(legend, BorderLayout.SOUTH);

            add(right, BorderLayout.CENTER);

            // Timer to refresh cooked list and table states
            javax.swing.Timer timer = new javax.swing.Timer(400, e -> refreshAll());
            timer.start();

            setVisible(true);
        }

        private JLabel legendLabel(Color c, String text) {
            JLabel l = new JLabel(" " + text + " ");
            l.setOpaque(true);
            l.setBackground(c);
            l.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            return l;
        }

		private void refreshAll() {
			// --- remember selection ---
			int selected = cookedList.getSelectedIndex();

			// --- rebuild cooked orders list ---
			cookedModel.clear();
			for (Order o : Globals.chefToWaiter) {
				cookedModel.addElement(o.toString());
			}

			// --- restore selection if possible ---
			if (selected >= 0 && selected < cookedModel.size()) {
				cookedList.setSelectedIndex(selected);
			}

			// --- refresh table buttons ---
			for (int i = 0; i < Globals.TABLE_COUNT; i++) {
				Table t = Globals.tables[i];
				JButton b = tableButtons[i];
				if (!t.occupied) {
					b.setText("<html><b>Table " + (i+1) + "</b><br/>(VACANT)</html>");
					b.setBackground(new Color(0x4CAF50));
				} else if (!t.served) {
					String label = (t.assignedOrder != null) ? ("Order " + t.assignedOrder.orderNumber) : "ASSIGNED";
					b.setText("<html><b>Table " + (i+1) + "</b><br/>(" + label + ")</html>");
					b.setBackground(new Color(0xF7DC6F));
				} else {
					String label = (t.assignedOrder != null) ? ("Order " + t.assignedOrder.orderNumber) : "OCCUPIED";
					b.setText("<html><b>Table " + (i+1) + "</b><br/>(" + label + ")</html>");
					b.setBackground(new Color(0xE74C3C));
				}
			}
		}


        private void serveSelected() {
            int sel = cookedList.getSelectedIndex();
            if (sel < 0) {
                JOptionPane.showMessageDialog(this, "Select a cooked order to serve.");
                return;
            }
            // Find the order at that index in the queue
            Order target = null;
            int idx = 0;
            for (Order o : Globals.chefToWaiter) {
                if (idx == sel) { target = o; break; }
                idx++;
            }
            if (target != null) {
                Globals.chefToWaiter.remove(target);
                if (target.tableNumber != 0) {
                    Table t = Globals.tables[target.tableNumber - 1];
                    t.served = true;
                    t.assignedOrder = target;
                } else {
                    // If it's WAITING and somehow reached here, we don't mark a table
                    JOptionPane.showMessageDialog(this, "Order " + target.orderNumber + " has no assigned table.");
                }
            }
        }

        private void autoAssignWaiting() {
            for (Table t : Globals.tables) {
                if (!t.occupied && !Globals.waitingOrders.isEmpty()) {
                    Order w = Globals.waitingOrders.poll();
                    w.tableNumber = t.number;
                    Globals.cashierToChef.add(w);
                    t.occupied = true;
                    t.served = false;
                    t.assignedOrder = w;
                    // notify (optional)
                    JOptionPane.showMessageDialog(this, "Waiting order " + w.orderNumber + " assigned to Table " + t.number);
                }
            }
        }
    }
}
