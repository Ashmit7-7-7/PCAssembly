import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class ComputerAssemblyApplication extends JFrame {
    private JComboBox<String> motherboardBox, ramBox, hddBox, gpuBox, smpsBox, chassisBox, licenseBox;
    private JTextField totalCostField;
    private Map<String, Integer> componentPrices;
    private JPanel adminPanel;

    // MongoDB Client and Collection
    private MongoCollection<Document> componentCollection;

    public ComputerAssemblyApplication() {
        setTitle("Computer Assembly");
        setSize(800, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));

        componentPrices = new HashMap<>();

        // Initialize MongoDB connection
        initializeMongoDB();

        // Load components from MongoDB
        loadComponentsFromMongoDB();

        // Populate initial component prices
        populateComponentPrices();

        // Left panel for selecting components
        JPanel assemblyPanel = new JPanel();
        assemblyPanel.setLayout(new GridLayout(8, 3, 5, 5)); // Increased column count

        assemblyPanel.add(new JLabel("Select Motherboard:"));
        motherboardBox = createComponentComboBox("Motherboard");
        assemblyPanel.add(motherboardBox);
        JButton removeMotherboardButton = createRemoveButton(motherboardBox);
        assemblyPanel.add(removeMotherboardButton);

        assemblyPanel.add(new JLabel("Select RAM:"));
        ramBox = createComponentComboBox("RAM");
        assemblyPanel.add(ramBox);
        JButton removeRamButton = createRemoveButton(ramBox);
        assemblyPanel.add(removeRamButton);

        assemblyPanel.add(new JLabel("Select HDD/SSD:"));
        hddBox = createComponentComboBox("HDD/SSD");
        assemblyPanel.add(hddBox);
        JButton removeHddButton = createRemoveButton(hddBox);
        assemblyPanel.add(removeHddButton);

        assemblyPanel.add(new JLabel("Select GPU:"));
        gpuBox = createComponentComboBox("GPU");
        assemblyPanel.add(gpuBox);
        JButton removeGpuButton = createRemoveButton(gpuBox);
        assemblyPanel.add(removeGpuButton);

        assemblyPanel.add(new JLabel("Select SMPS:"));
        smpsBox = createComponentComboBox("SMPS");
        assemblyPanel.add(smpsBox);
        JButton removeSmpsButton = createRemoveButton(smpsBox);
        assemblyPanel.add(removeSmpsButton);

        assemblyPanel.add(new JLabel("Select Chassis:"));
        chassisBox = createComponentComboBox("Chassis");
        assemblyPanel.add(chassisBox);
        JButton removeChassisButton = createRemoveButton(chassisBox);
        assemblyPanel.add(removeChassisButton);

        assemblyPanel.add(new JLabel("Select License:"));
        licenseBox = createComponentComboBox("License");
        assemblyPanel.add(licenseBox);
        JButton removeLicenseButton = createRemoveButton(licenseBox);
        assemblyPanel.add(removeLicenseButton);

        JButton calculateButton = new JButton("Calculate Total Cost");
        assemblyPanel.add(calculateButton);

        totalCostField = new JTextField();
        totalCostField.setEditable(false);
        assemblyPanel.add(totalCostField);

        calculateButton.addActionListener(e -> calculateTotalCost());

        // Right panel for admin options
        adminPanel = new JPanel();
        adminPanel.setLayout(new GridLayout(8, 2, 5, 5));
        adminPanel.setBorder(BorderFactory.createTitledBorder("Admin - Add New Components"));

        addAdminOption("Motherboard", motherboardBox);
        addAdminOption("RAM", ramBox);
        addAdminOption("HDD/SSD", hddBox);
        addAdminOption("GPU", gpuBox);
        addAdminOption("SMPS", smpsBox);
        addAdminOption("Chassis", chassisBox);
        addAdminOption("License", licenseBox);

        add(assemblyPanel);
        add(adminPanel);
    }

    private void initializeMongoDB() {
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        var mongoClient = MongoClients.create(settings);
        MongoDatabase database = mongoClient.getDatabase("computer_assembly_db");
        componentCollection = database.getCollection("components");
    }

    private JComboBox<String> createComponentComboBox(String category) {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.addItem("Select");
        for (Map.Entry<String, Integer> entry : componentPrices.entrySet()) {
            if (entry.getKey().startsWith(category)) {
                comboBox.addItem(entry.getKey().substring(category.length() + 1)); // Add component names only
            }
        }
        return comboBox;
    }

    private JButton createRemoveButton(JComboBox<String> componentBox) {
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> {
            String selectedItem = (String) componentBox.getSelectedItem();
            if (selectedItem != null && !"Select".equals(selectedItem)) {
                String fullName = componentBox.getName() + " " + selectedItem;
                componentPrices.remove(fullName); // Remove from local map
                componentBox.removeItem(selectedItem); // Remove from dropdown
                removeComponentFromMongoDB(fullName); // Remove from MongoDB
                componentBox.setSelectedIndex(0); // Reset selection
            } else {
                JOptionPane.showMessageDialog(this, "Please select a component to remove.");
            }
        });
        return removeButton;
    }

    private void addAdminOption(String category, JComboBox<String> componentBox) {
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JButton addButton = new JButton("Add");

        addButton.addActionListener(e -> {
            String newItemName = nameField.getText();
            String priceText = priceField.getText();
            if (!newItemName.isEmpty() && !priceText.isEmpty()) {
                try {
                    int price = Integer.parseInt(priceText);
                    String fullName = category + " " + newItemName;
                    componentBox.addItem(newItemName);
                    componentPrices.put(fullName, price); // Store in local map
                    addComponentToMongoDB(category, newItemName, price); // Add to MongoDB
                    nameField.setText("");
                    priceField.setText("");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid price.");
                }
            }
        });

        adminPanel.add(new JLabel(category + " Name:"));
        adminPanel.add(nameField);

        adminPanel.add(new JLabel(category + " Price:"));
        adminPanel.add(priceField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        adminPanel.add(buttonPanel);
    }

    private void calculateTotalCost() {
        int totalCost = 0;
        totalCost += getPrice("Motherboard", motherboardBox.getSelectedItem());
        totalCost += getPrice("RAM", ramBox.getSelectedItem());
        totalCost += getPrice("HDD/SSD", hddBox.getSelectedItem());
        totalCost += getPrice("GPU", gpuBox.getSelectedItem());
        totalCost += getPrice("SMPS", smpsBox.getSelectedItem());
        totalCost += getPrice("Chassis", chassisBox.getSelectedItem());
        totalCost += getPrice("License", licenseBox.getSelectedItem());
        totalCostField.setText("Total: $" + totalCost);
    }

    private int getPrice(String category, Object selectedItem) {
        if (selectedItem == null || "Select".equals(selectedItem.toString())) return 0;
        String key = category + " " + selectedItem.toString();
        return componentPrices.getOrDefault(key, 0);
    }

    private void loadComponentsFromMongoDB() {
        for (Document doc : componentCollection.find()) {
            String category = doc.getString("category");
            String name = doc.getString("name");
            int price = doc.getInteger("price");
            componentPrices.put(category + " " + name, price);
        }
    }

    private void addComponentToMongoDB(String category, String name, int price) {
        Document componentDoc = new Document("category", category)
                .append("name", name)
                .append("price", price);
        componentCollection.insertOne(componentDoc);
    }

    private void removeComponentFromMongoDB(String fullName) {
        String[] parts = fullName.split(" ", 2);
        if (parts.length == 2) {
            String category = parts[0];
            String name = parts[1];
            Document query = new Document("category", category).append("name", name);
            componentCollection.deleteOne(query);
        }
    }

    private void populateComponentPrices() {
        // Component prices (expanded options for enthusiasts)
        componentPrices.put("Motherboard A", 150);
        componentPrices.put("Motherboard B", 200);
        componentPrices.put("Motherboard Enthusiast X570", 350);
        componentPrices.put("Motherboard High-End Z690", 500);
        componentPrices.put("RAM 8GB", 50);
        componentPrices.put("RAM 16GB", 90);
        componentPrices.put("RAM 32GB", 180);
        componentPrices.put("RAM 64GB High-Performance", 350);
        
        // Expanded HDD/SSD options
        componentPrices.put("HDD 500GB", 60);
        componentPrices.put("HDD 1TB", 100);
        componentPrices.put("HDD 2TB", 150);
        componentPrices.put("HDD 4TB", 250);
        componentPrices.put("SSD 256GB", 50);
        componentPrices.put("SSD 512GB", 80);
        componentPrices.put("SSD 1TB", 150);
        componentPrices.put("SSD 1TB NVMe", 200);
        componentPrices.put("SSD 2TB NVMe", 400);
        componentPrices.put("SSD 4TB NVMe", 800);
        
        componentPrices.put("GPU GTX 1050", 180);
        componentPrices.put("GPU RTX 3060", 350);
        componentPrices.put("GPU RTX 4090", 1600);
        componentPrices.put("GPU RX 7900 XTX", 1200);
        componentPrices.put("SMPS 500W", 60);
        componentPrices.put("SMPS 750W", 100);
        componentPrices.put("SMPS 1000W Platinum", 200);
        componentPrices.put("SMPS 1200W Titanium", 300);
        componentPrices.put("Chassis Basic", 40);
        componentPrices.put("Chassis Mid-Tower", 80);
        componentPrices.put("Chassis Full-Tower", 150);
        componentPrices.put("License Windows 10", 100);
        componentPrices.put("License Windows 11", 120);
        componentPrices.put("License Office 2021", 150);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ComputerAssemblyApplication app = new ComputerAssemblyApplication();
            app.setVisible(true);
        });
    }
}