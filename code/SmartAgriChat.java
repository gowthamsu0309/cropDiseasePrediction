import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList; 
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher; // Added for regex in calculation methods
import java.util.regex.Pattern; // Added for regex in calculation methods

// ====================================================================================
// PRIMARY CHAT FRAME CLASS
// ====================================================================================

public class SmartAgriChat extends JFrame {

    private final JPanel chatPanel;
    private final JScrollPane scrollPane;
    private final JTextField userInputField;
    private final Chatbot chatbot = new Chatbot();
    private final String loggedInUser;
    private final List<String> chatHistory = new ArrayList<>(); 

    public static void main(String[] args) {
        // Ensure that the necessary MySQL driver is loaded (if connecting to DB)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Feedback feature will not work.");
        }
        SwingUtilities.invokeLater(() -> new SmartAgriChat("TestUser").setVisible(true));
    }

    public SmartAgriChat(String username) {
        this.loggedInUser = username;
        setTitle("🌾 SmartAgriChat — Welcome, " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);

        JPanel backgroundPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                String imagePath = "G:/CropDiseasePrediction/chatbot.jpg"; 
                ImageIcon imageIcon = new ImageIcon(imagePath);
                if (imageIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
                    System.err.println("Error: Could not load background image from path: " + imagePath);
                    g.setColor(new Color(213, 232, 212));
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        setContentPane(backgroundPanel);
        backgroundPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setOpaque(false);

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        backgroundPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setOpaque(false);
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);

        userInputField = new JTextField();
        userInputField.setFont(new Font("Arial", Font.PLAIN, 16));
        userInputField.addActionListener(e -> sendMessage());

        JPanel emojiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        emojiPanel.setOpaque(false);
        String[] emojis = {"😀", "🌾", "😢", "👍"};
        for (String emoji : emojis) {
            JButton emojiButton = new JButton(emoji);
            emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            emojiButton.addActionListener(e -> userInputField.setText(userInputField.getText() + emoji));
            emojiPanel.add(emojiButton);
        }

        JButton uploadButton = new JButton("🖼️");
        uploadButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        uploadButton.setToolTipText("Upload Crop Image");
        uploadButton.addActionListener(e -> uploadImage());

        inputPanel.add(emojiPanel, BorderLayout.WEST);
        inputPanel.add(userInputField, BorderLayout.CENTER);
        inputPanel.add(uploadButton, BorderLayout.EAST);

        JPanel utilityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        utilityPanel.setOpaque(false);
        
        JButton exportButton = new JButton("⬇️ Download Chat Log");
        exportButton.addActionListener(e -> exportChatToTXT());
        utilityPanel.add(exportButton);
        
        JButton giveFeedbackButton = new JButton("Give Feedback");
        giveFeedbackButton.addActionListener(e -> {
            FeedbackDialog feedbackDialog = new FeedbackDialog(this, loggedInUser);
            feedbackDialog.setVisible(true);
        });
        utilityPanel.add(giveFeedbackButton);
        
        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(utilityPanel, BorderLayout.SOUTH); 
        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        addBotMessage("Hello! I'm SmartAgriChat. How can I assist you with your crops today? Try asking about 'water for rice' or 'calculate fertilizer 10kg N 10-26-26'.");
    }

    private void exportChatToTXT() {
        if (chatHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The chat is empty. Start a conversation first.", "No Content to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("SmartAgriChat_Log_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt"));
        fileChooser.setDialogTitle("Save Chat History");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                for (String message : chatHistory) {
                    writer.write(message + "\n\n");
                }
                writer.flush();
                JOptionPane.showMessageDialog(this, "Chat history saved successfully to:\n" + file.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving chat history: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendMessage() {
        String userText = userInputField.getText().trim();
        if (!userText.isEmpty()) {
            addUserMessage(userText, null);
            userInputField.setText("");
            String botResponse = chatbot.getResponse(userText);
            simulateTypingAndReply(botResponse);
        }
    }

    private void uploadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            ImageIcon imageIcon = new ImageIcon(selectedFile.getAbsolutePath());
            String userMsg = "Here is my crop image: " + selectedFile.getName(); 
            addUserMessage("Here is my crop image.", imageIcon);
            
            chatHistory.add("[" + new SimpleDateFormat("hh:mm a").format(new Date()) + "] USER (Image): " + userMsg);
            
            simulateTypingAndReply("Analyzing your image, please wait...");
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    return ImageAnalyzer.analyze(selectedFile);
                }
                @Override
                protected void done() {
                    try {
                        String diagnosis = get();
                        addBotMessage(diagnosis);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        addBotMessage("Sorry, an error occurred during the image analysis.");
                    }
                }
            };
            worker.execute();
        }
    }

    public void postFeedbackMessage(String formattedFeedback) {
        addUserMessage(formattedFeedback, null);
        simulateTypingAndReply("Thank you for your valuable feedback!");
    }

    private void simulateTypingAndReply(String message) {
        Timer timer = new Timer(1000, e -> addBotMessage(message));
        timer.setRepeats(false);
        timer.start();
    }

    private void addMessage(String message, ImageIcon image, boolean isUser) {
        String timestamp = new SimpleDateFormat("hh:mm a").format(new Date());
        String sender = isUser ? loggedInUser : "SmartAgriChat";
        String logEntry = "[" + timestamp + "] " + sender + ": " + message;
        
        chatHistory.add(logEntry);
        
        JTextArea messageArea = new JTextArea(message);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setOpaque(false);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        messageArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel bubble = new JPanel(new BorderLayout(5, 5));
        bubble.setBackground(isUser ? new Color(220, 248, 198) : Color.WHITE);
        bubble.setBorder(new BubbleBorder(isUser ? new Color(200, 230, 180) : Color.LIGHT_GRAY, 2, 16, 0));
        bubble.add(messageArea, BorderLayout.CENTER);

        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        timestampLabel.setForeground(Color.GRAY);
        timestampLabel.setBorder(new EmptyBorder(0, 10, 5, 10));
        bubble.add(timestampLabel, BorderLayout.SOUTH);

        if (image != null) {
            Image scaledImage = image.getImage().getScaledInstance(150, -1, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            imageLabel.setBorder(new EmptyBorder(10, 10, 0, 10));
            bubble.add(imageLabel, BorderLayout.NORTH);
        }
        
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        Component strut = Box.createHorizontalStrut(chatPanel.getWidth() / 3);

        if (isUser) {
            row.add(strut, BorderLayout.LINE_START); 
            row.add(bubble, BorderLayout.CENTER);    
        } else {
            row.add(bubble, BorderLayout.CENTER);    
            row.add(strut, BorderLayout.LINE_END);      
        }

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(10));
        chatPanel.revalidate();
        chatPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
    private void addUserMessage(String message, ImageIcon image) {
        addMessage(message, image, true);
    }
    private void addBotMessage(String message) {
        addMessage(message, null, false);
    }

    // ====================================================================================
    // INNER CLASSES (Chatbot, ImageAnalyzer, etc.)
    // ====================================================================================

    static class Chatbot {
        
        // --- DATA STRUCTURES (using static nested classes instead of records for broader compatibility) ---
        static class Disease {
             final String name; 
             final List<String> keywords;
             final String recommendation;
             Disease(String name, List<String> keywords, String recommendation) {
                this.name = name; this.keywords = keywords; this.recommendation = recommendation;
             }
        }
        static class CropWaterNeed {
            final String crop;
            final double dailyETo_mm;
            final String notes;
            CropWaterNeed(String crop, double dailyETo_mm, String notes) {
                this.crop = crop; this.dailyETo_mm = dailyETo_mm; this.notes = notes;
            }
        }
        
        // --- DISEASE DATABASE (re-mapped from record to static nested class) ---
        private static final List<Disease> DISEASE_DATABASE = List.of(
            new Disease("Early Blight", List.of("early_blight"), "**Recommendation:**\n1. **Cultural Control:** Prune lower leaves and mulch to prevent soil splash.\n2. **Chemical Control:** Apply fungicides with Chlorothalonil or copper."),
            new Disease("Sheath Blight of Rice", List.of("sheath_blight"), "**Recommendation:**\n1. **Cultural Control:** Avoid excess Nitrogen.\n2. **Chemical Control:** Apply Propiconazole at the plant base."),
            new Disease("Stripe Rust of Wheat", List.of("stripe_rust", "yellow_rust"), "**Recommendation:**\n1. **Resistant Varieties:** Plant resistant wheat varieties.\n2. **Chemical Control:** Apply Tebuconazole or Propiconazole at first sign."),
            new Disease("Gray Leaf Spot of Corn", List.of("gray_leaf_spot"), "**Recommendation:**\n1. **Cultural Control:** Rotate crops and till residue.\n2. **Chemical Control:** Apply fungicide in humid conditions."),
            new Disease("Verticillium Wilt", List.of("verticillium_wilt"), "**Recommendation:**\n1. **No Cure:** Remove infected plants.\n2. **Prevention:** Use long crop rotations and soil solarization."),
            new Disease("Rice Blast", List.of("blast"), "**Recommendation:**\n1. **Water Management:** Maintain consistent water levels.\n2. **Chemical Control:** Apply Tricyclazole at first sign of lesions."),
            new Disease("Downy Mildew", List.of("downy_mildew"), "**Recommendation:**\n1. **Distinction:** Yellow spots on top, grey fuzz underneath leaves.\n2. **Control:** Improve air circulation and use a Mancozeb fungicide."),
            new Disease("Late Blight", List.of("blight"), "**Recommendation:**\n1. **Cultural Control:** Remove infected leaves, ensure air circulation.\n2. **Chemical Control:** Apply preventative Mancozeb or copper-based sprays."),
            new Disease("Rust Fungus", List.of("rust"), "**Recommendation:**\n1. **Sanitation:** Remove infected parts.\n2. **Cultural Control:** Keep foliage dry.\n3. **Chemical Control:** Use sulfur-based fungicide or neem oil."),
            new Disease("Powdery Mildew", List.of("mildew"), "**Recommendation:**\n1. **Cultural Control:** Prune for air circulation.\n2. **Organic Treatment:** Use a baking soda spray or neem oil."),
            new Disease("Mosaic Virus", List.of("mosaic_virus", "mosaic"), "**Recommendation:**\n1. **Removal:** No cure; remove and destroy plants.\n2. **Vector Control:** Control aphids that spread the virus.\n3. **Sanitation:** Disinfect tools."),
            new Disease("Septoria Leaf Spot", List.of("leaf_spot", "leafspot"), "**Recommendation:**\n1. **Sanitation:** Remove and burn infected leaves.\n2. **Cultural Control:** Mulch to prevent splash.\n3. **Chemical Control:** Use copper-based fungicides."),
            new Disease("Bacterial Wilt", List.of("wilt"), "**Recommendation:**\n1. **No Cure:** Remove and destroy plant.\n2. **Crop Rotation:** Avoid planting susceptible crops in the same spot for 3-4 years."),
            new Disease("Leaf Miners", List.of("leaf_miner", "leafminer"), "**Recommendation:**\n1. **Mechanical Control:** Crush larvae in leaf trails.\n2. **Sanitation:** Remove infested leaves.\n3. **Control:** Use Neem oil or Spinosad-based insecticides."),
            new Disease("Brown Spot of Rice", List.of("brown_spot"), "**Recommendation:**\n1. **Nutrient Management:** Ensure balanced fertilization.\n2. **Control:** Use seed treatments and fungicides like Mancozeb."),
            new Disease("Loose Smut of Wheat", List.of("loose_smut"), "**Recommendation:**\n1. **Seed Treatment:** Treat seeds with a systemic fungicide before planting.\n2. **Sanitation:** Use certified disease-free seeds."),
            new Disease("Red Rot of Sugarcane", List.of("red_rot"), "**Recommendation:**\n1. **Prevention:** Use disease-free setts and hot water treatment.\n2. **Control:** Uproot and destroy infected clumps."),
            new Disease("Common Corn Smut", List.of("corn_smut"), "**Recommendation:**\n1. **Control:** No chemical treatment. Remove galls before they burst.\n2. **Note:** Young galls are a delicacy (huitlacoche)."),
            new Disease("Common Potato Scab", List.of("potato_scab"), "**Recommendation:**\n1. **Soil pH:** Keep soil pH below 5.2.\n2. **Water Management:** Keep soil moist when tubers form."),
            new Disease("Healthy", List.of("healthy"), "**Recommendation:** Excellent work. Continue current practices and keep monitoring.")
        );
        
        // --- WATER NEED DATABASE (re-mapped from record to static nested class) ---
        private static final List<CropWaterNeed> WATER_DATABASE = List.of(
            new CropWaterNeed("rice", 7.0, "Maintain standing water or saturated soil for optimal yield. Values can peak up to 10 mm/day."),
            new CropWaterNeed("wheat", 5.5, "Water deeply, but less frequently. Needs around 450-650 mm over the growing season."),
            new CropWaterNeed("tomato", 4.5, "Keep soil evenly moist, especially during fruiting. Avoid overhead watering to prevent disease."),
            new CropWaterNeed("maize", 6.0, "High water demand during silking and tasseling stages. Water stress at these times severely reduces yield."),
            new CropWaterNeed("potato", 5.0, "Critical need during tuber formation. Frequent, light watering is better than deep, infrequent watering.")
        );

        private String getWeatherReport() {
            Random random = new Random();
            int temp = 26 + random.nextInt(5);
            String[] conditions = {"Clear skies", "Partly cloudy", "A slight haze"};
            String condition = conditions[random.nextInt(conditions.length)];
            return String.format(
                "WEATHER REPORT for Chennai:\n" + "🌡️ Temperature: %d°C\n" + "☀️ Condition: %s\n" + "Forecast: Expect clear skies tonight. Good weather for planning tomorrow's work.",
                temp, condition
            );
        }

        private String getMarketPrices() {
            Random random = new Random();
            int tomatoPrice = 15 + random.nextInt(5);
            int ricePrice = 50 + random.nextInt(10);
            int onionPrice = 20 + random.nextInt(7);
            return String.format(
                "TODAY'S MARKET RATES (simulated):\n" + "🍅 Tomato: ₹%d/kg\n" + "🍚 Rice (Ponni): ₹%d/kg\n" + "🧅 Onion: ₹%d/kg\n" + "Please check with your local market for exact prices.",
                tomatoPrice, ricePrice, onionPrice
            );
        }

        private String getSeasonalAdvice() {
            int month = java.time.LocalDate.now().getMonthValue();
            String advice;
            switch (month) {
                case 10:
                case 11:
                    advice = "SEASONAL ADVICE (Oct-Nov):\nThis is the Rabi season. It's a great time for planting wheat, barley, and mustard. With the monsoon ending, focus on water management and prepare for cooler nights.";
                    break;
                case 4:
                case 5:
                    advice = "SEASONAL ADVICE (Apr-May):\nIt's peak summer. Ensure deep and frequent irrigation. This is the time to harvest summer crops and begin land preparation for the upcoming Kharif season.";
                    break;
                default:
                    advice = "SEASONAL ADVICE:\nCheck your soil moisture levels regularly. Monitor for pests that are common this time of year and ensure your equipment is well-maintained for the upcoming season.";
                    break;
            }
            return advice;
        }
        
        // --- FERTILIZER CALCULATION METHOD (from previous suggested feature) ---
        private String calculateFertilizer(String userInput) {
            String response = "I need two numbers: the **target amount** of a nutrient (N, P, or K) you want to apply, and the **NPK ratio** of your fertilizer. For example: 'How much 10-26-26 do I need for 10 kg of Nitrogen?'\n\n";

            try {
                // Get the fertilizer ratio (N-P-K)
                Matcher matcher = Pattern.compile("(\\d+)[\\s\\-x](\\d+)[\\s\\-x](\\d+)").matcher(userInput);
                
                int N_ratio = 0;
                int P_ratio = 0;
                int K_ratio = 0;
                if (matcher.find()) {
                    N_ratio = Integer.parseInt(matcher.group(1));
                    P_ratio = Integer.parseInt(matcher.group(2));
                    K_ratio = Integer.parseInt(matcher.group(3));
                } else {
                    return response + "Error: I couldn't find a valid NPK ratio (e.g., 10-26-26) in your request.";
                }
                
                // Get the target nutrient amount
                Matcher amountMatcher = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(kg|lb)\\s*(of|for)?\\s*(nitrogen|phosphorus|potassium|n|p|k)").matcher(userInput.toLowerCase());
                
                double targetAmount = 0.0;
                int nutrientRatio = 0;
                String unit = "";
                String targetNutrient = "";

                if (amountMatcher.find()) {
                    targetAmount = Double.parseDouble(amountMatcher.group(1));
                    unit = amountMatcher.group(3).toUpperCase();
                    targetNutrient = amountMatcher.group(5).substring(0, 1).toUpperCase(); 

                    if (targetNutrient.equals("N")) nutrientRatio = N_ratio;
                    else if (targetNutrient.equals("P")) nutrientRatio = P_ratio;
                    else if (targetNutrient.equals("K")) nutrientRatio = K_ratio;
                    else return response + "Error: Target nutrient not recognized. Please specify N, P, or K.";

                    if (nutrientRatio == 0) {
                        return String.format("The fertilizer you specified (%d-%d-%d) contains 0%% %s. You cannot meet the target with this fertilizer.", N_ratio, P_ratio, K_ratio, targetNutrient);
                    }

                    // Calculation: Total Fertilizer Needed = (Target Nutrient Amount / Nutrient Ratio) * 100
                    double fertilizerNeeded = (targetAmount / nutrientRatio) * 100.0;

                    return String.format(
                        "**FERTILIZER CALCULATION RESULT**\n" +
                        "Fertilizer Ratio: **%d-%d-%d**\n" +
                        "Target Nutrient: **%.1f %s of %s**\n" +
                        "--- \n" +
                        "You need to apply approximately **%.2f %s** of the %d-%d-%d fertilizer to provide %.1f %s of %s.",
                        N_ratio, P_ratio, K_ratio, 
                        targetAmount, unit, targetNutrient,
                        fertilizerNeeded, unit, N_ratio, P_ratio, K_ratio, 
                        targetAmount, targetNutrient
                    );
                    
                } else {
                    return response + "Error: I couldn't find a target amount and nutrient (e.g., '10kg of N') in your request.";
                }

            } catch (Exception e) {
                return response + "Sorry, I ran into an error trying to process the numbers. Please rephrase the calculation question, for example: 'calculate for 5lb of P with a 15-30-15 fertilizer'.";
            }
        }
        
        // --- WATER REQUIREMENT METHOD (from previous suggested feature) ---
private String calculateWaterRequirement(String userInput) {
    String lowerCaseInput = userInput.toLowerCase();

    // 1. Simple attempt to extract a crop name
    for (CropWaterNeed need : WATER_DATABASE) {
        if (lowerCaseInput.contains(need.crop)) { // Corrected: access field as 'need.crop'
            
            // 2. Simple calculation for weekly need (ETo * 7 days)
            double weeklyNeed = need.dailyETo_mm * 7.0; // Corrected: access field as 'need.dailyETo_mm'
            
            return String.format(
                "**💧 %s Water Requirement Estimate 💧**\n" +
                "**Daily Estimated Water Need (ETo):** %.1f mm/day\n" +
                "**Weekly Estimated Water Need:** %.1f mm/week\n\n" +
                "**Important Note:** %s\n" +
                "This is a general estimate. Actual needs depend on local weather, soil type, and plant stage.",
                need.crop.toUpperCase(), // Corrected: access field as 'need.crop'
                need.dailyETo_mm,
                weeklyNeed,
                need.notes // Corrected: access field as 'need.notes'
            );
        }
    }
    
    // 3. Fallback if crop is not found
    return "Sorry, I can calculate the water requirement for common crops like **Rice, Wheat, Tomato, Maize, and Potato**. Please specify the crop you are interested in.";
}
        
        public String getResponse(String userInput) {
            String lowerCaseInput = userInput.toLowerCase();
            
            // --- NEW FUNCTIONAL CHECKS ---
            if (lowerCaseInput.contains("calculate") && (lowerCaseInput.contains("fertilizer") || lowerCaseInput.contains("npk"))) {
                return calculateFertilizer(userInput);
            }
            if (lowerCaseInput.contains("water") && (lowerCaseInput.contains("need") || lowerCaseInput.contains("requirement") || lowerCaseInput.contains("calculate"))) {
                return calculateWaterRequirement(userInput);
            }
            
            // --- ORIGINAL CHECKS ---
            if (lowerCaseInput.contains("weather") || lowerCaseInput.contains("forecast")) return getWeatherReport();
            if (lowerCaseInput.contains("price") || lowerCaseInput.contains("market rate")) return getMarketPrices();
            if (lowerCaseInput.contains("advice") || lowerCaseInput.contains("what should i do") || lowerCaseInput.contains("seasonal")) return getSeasonalAdvice();
            if (lowerCaseInput.contains("hello") || lowerCaseInput.contains("hi") || lowerCaseInput.contains("hey")) return getGreeting();
            if (lowerCaseInput.contains("dap") || lowerCaseInput.contains("di-ammonium phosphate")) return "DAP is an excellent source of Phosphorus (P) and Nitrogen (N). It's very popular to use at the time of sowing to promote strong root development.";
            if (lowerCaseInput.contains("mop") || lowerCaseInput.contains("muriate of potash")) return "Muriate of Potash (MOP) is a great source of Potassium (K), which is vital for plant strength, disease resistance, and improving the quality and size of fruits and grains.";
            if (lowerCaseInput.contains("urea")) return "Urea is a very concentrated source of Nitrogen (N). It's used to promote vigorous leafy growth. Be careful not to over-apply, as it can burn plants.";
            if (lowerCaseInput.contains("how to apply fertilizer")) return "Common methods are: \n1. Broadcasting: Spreading evenly over the soil. \n2. Banding: Applying in a strip next to the seed row. \n3. Top Dressing: Applying around the base of the plant later in its growth.";
            if (lowerCaseInput.contains("npk") || lowerCaseInput.contains("10-10-10")) return "NPK stands for Nitrogen (N), Phosphorus (P), and Potassium (K). A balanced 10-10-10 is a good general-purpose choice for many crops.";
            if (lowerCaseInput.contains("organic fertilizer") || lowerCaseInput.contains("compost") || lowerCaseInput.contains("manure")) return "Organic options like compost and aged manure are excellent! They release nutrients slowly and improve soil structure over time, reducing the risk of fertilizer burn.";
            if (lowerCaseInput.contains("nitrogen")) return "Nitrogen (N) is key for lush, green leafy growth. Use nitrogen-rich fertilizers like Urea or Ammonium Nitrate for leafy vegetables (spinach, lettuce) or during the early growth stage of crops.";
            if (lowerCaseInput.contains("phosphorus")) return "Phosphorus (P) is vital for strong root development, flowering, and fruit production. Bone meal and superphosphates are good sources. It's crucial for crops like tomatoes, peppers, and root vegetables.";
            if (lowerCaseInput.contains("potassium")) return "Potassium (K), often from Potash, is like a multivitamin for plants. It regulates water, improves disease resistance, and strengthens the plant overall. Very important for fruit and vegetable quality.";
            if (lowerCaseInput.contains("micronutrients")) return "Micronutrients like iron, zinc, and manganese are needed in small amounts but are critical. Deficiency can cause yellowing between leaf veins (chlorosis). A soil test can identify deficiencies.";
            if (lowerCaseInput.contains("liquid fertilizer") || lowerCaseInput.contains("granular fertilizer")) return "Granular fertilizers are slow-release, feeding plants over time. Liquid fertilizers are fast-acting and absorbed quickly, which is good for a quick boost but requires more frequent application.";
            if (lowerCaseInput.contains("caterpillar") || lowerCaseInput.contains("armyworm") || lowerCaseInput.contains("borer")) return "For caterpillars and borers, biological pesticides like Bacillus thuringiensis (Bt) or Spinosad are very effective and safer for the environment. Neem oil can also deter them.";
            if (lowerCaseInput.contains("whitefly") || lowerCaseInput.contains("white flies")) return "Whiteflies are tough. Use yellow sticky traps to monitor and reduce their numbers. Spraying with insecticidal soap or neem oil can help, but repeated applications are necessary.";
            if (lowerCaseInput.contains("ipm") || lowerCaseInput.contains("integrated pest management")) return "IPM is a smart approach to pest control. It combines multiple strategies like using pest-resistant crops and natural predators, using pesticides only when absolutely necessary.";
            if (lowerCaseInput.contains("systemic insecticide") || lowerCaseInput.contains("contact insecticide")) return "Contact insecticides kill pests on direct contact. Systemic insecticides are absorbed by the plant, making the plant itself toxic to feeding insects. Systemics are longer-lasting but should be used carefully.";
            if (lowerCaseInput.contains("biological control")) return "Biological control uses natural enemies to manage pests. For example, releasing ladybugs to eat aphids or using Bt, a bacterium that targets caterpillars. 🐞";
            if (lowerCaseInput.contains("herbicide") || lowerCaseInput.contains("weed killer")) return "Herbicides control weeds. Selective herbicides target specific weeds without harming the crop. Non-selective herbicides (like glyphosate) kill any plant they touch and must be applied very carefully.";
            if (lowerCaseInput.contains("fungicide") || lowerCaseInput.contains("powdery mildew") || lowerCaseInput.contains("blight")) return "Fungicides treat diseases caused by fungi, like blight, mildew, and rust. Copper and sulfur-based fungicides are common for prevention. Always apply according to the label.";
            if (lowerCaseInput.contains("neem oil")) return "Neem oil is a fantastic organic, broad-spectrum pesticide and fungicide. It disrupts insect hormones and deters feeding. It's effective against aphids, mites, and powdery mildew.";
            if (lowerCaseInput.contains("crop rotation")) return "Crop rotation is planting different crop types in the same area in sequential seasons. It helps break pest and disease cycles, improves soil fertility, and reduces the need for chemicals.";
            if (lowerCaseInput.contains("soil test")) return "A soil test is essential! It tells you the pH and nutrient levels of your soil. This helps you apply the exact type and amount of fertilizer needed, saving money and improving yield.";
            if (lowerCaseInput.contains("pruning")) return "Pruning removes dead or overgrown branches to improve air circulation, reduce disease risk, and encourage the plant to put energy into producing more fruit or flowers. Essential for fruit trees and plants like tomatoes.";
            if (lowerCaseInput.contains("mulch")) return "Mulching is covering the soil around plants with a layer of material like straw or wood chips. It's great for retaining soil moisture, suppressing weeds, and regulating soil temperature.";
            if (lowerCaseInput.contains("drip irrigation")) return "Drip irrigation is a highly efficient watering method. It delivers water slowly and directly to the plant's roots, minimizing evaporation and water waste. It can reduce weed growth and water usage by up to 70%.";
            if (lowerCaseInput.contains("cover crops") || lowerCaseInput.contains("green manure")) return "Cover crops, like clover or rye, are planted during the off-season to protect and enrich the soil. They prevent erosion, suppress weeds, and add organic matter and nutrients when tilled back into the soil ('green manure').";
            if (lowerCaseInput.contains("tool cleaning")) return "Cleaning and disinfecting your tools is critical to prevent the spread of diseases. Use a solution of 1 part bleach to 9 parts water or rubbing alcohol.";
            if (lowerCaseInput.contains("harvest")) return "The best time to harvest most vegetables and fruits is in the cool of the early morning. The plants are less stressed and the produce is at its peak freshness and hydration.";
            if (lowerCaseInput.contains("curing")) return "Curing is a process of letting produce like potatoes, onions, and garlic dry in a warm, ventilated area for a week or two. This toughens their skin, heals any cuts, and allows them to be stored for much longer.";
            if (lowerCaseInput.contains("grain storage")) return "For proper grain storage, ensure the grain is dried to the correct moisture level to prevent mold. The storage bin or silo must be thoroughly cleaned and sealed to protect against insects and rodents.";
            if (lowerCaseInput.contains("soil ph")) return "Soil pH is crucial for nutrient absorption. Most crops prefer a neutral pH (6.0-7.0). Use a test kit. Add lime to raise pH (make less acidic) or sulfur to lower pH (make more acidic).";
            if (lowerCaseInput.contains("water")) return "It's best to water plants deeply but infrequently to encourage deep root growth. Check the top inch of soil; if it's dry, it's time to water. Early morning is the best time.";
            if (lowerCaseInput.contains("tomato")) return "Tomatoes need full sun (6-8 hours) and well-drained, nutrient-rich soil. Use stakes or cages for support. Fertilize with a phosphorus-rich blend when they start flowering.";
            if (lowerCaseInput.contains("rice")) return "Rice cultivation requires significant water. Fields are typically flooded. It prefers clay or loam soil. The main challenge is managing water levels and controlling pests like the brown planthopper.";
            if (lowerCaseInput.contains("aphids")) return "Aphids can be controlled by spraying with soapy water or introducing natural predators like ladybugs. 🐞";
            if (lowerCaseInput.contains("bye") || lowerCaseInput.contains("thank you") || lowerCaseInput.contains("thanks")) return "You're welcome! Happy farming! 🌱";
            
            return "I'm sorry, I don't have information on that. Can you ask in a different way? You can ask about pests, specific crops, fertilizers, water needs, or soil. 🌾";
        }

        private String getGreeting() {
            LocalTime now = LocalTime.now();
            if (now.isBefore(LocalTime.NOON)) return "Good morning! How can I help you with your crops today? 🌾";
            if (now.isBefore(LocalTime.of(18, 0))) return "Good afternoon! What crop questions do you have?";
            return "Good evening! Let's solve your farming queries.";
        }
    }

    static class ImageAnalyzer {
        
        public static String analyze(File imageFile) {
            try {
                Thread.sleep(2500);
                String fileName = imageFile.getName().toLowerCase();
                for (Chatbot.Disease disease : Chatbot.DISEASE_DATABASE) {
                    for (String keyword : disease.keywords) {
                        if (fileName.contains(keyword)) {
                            return "Analysis complete. This appears to be **" + disease.name + "**.\n\n" + disease.recommendation;
                        }
                    }
                }
                return "Analysis complete. I couldn't identify a specific disease from this image. Please ensure the photo is clear and well-lit.";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Analysis was interrupted.";
            }
        }
    }
    
    static class BubbleBorder extends EmptyBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        BubbleBorder(Color color, int thickness, int radius, int pointerSize) {
            super(thickness, thickness, thickness, thickness);
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(this.color);
            g2d.setStroke(new BasicStroke(this.thickness));
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }
    }
}

class FeedbackDialog extends JDialog {
    private final JLabel[] starLabels = new JLabel[5];
    private final JTextArea descriptionArea;
    private final SmartAgriChat parentFrame;
    private final String loggedInUser;
    private final JButton submitButton;
    private int currentRating = 0;
    
    // NOTE: Update these with your actual database credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/farmer_db";
    private static final String USER = "root";
    private static final String PASS = "sugow@0309";

    private final ImageIcon emptyStar;
    private final ImageIcon filledStar;

    public FeedbackDialog(SmartAgriChat parent, String username) {
        super(parent, "Submit Feedback", true);
        this.parentFrame = parent;
        this.loggedInUser = username;

        emptyStar = createStarIcon(false);
        filledStar = createStarIcon(true);

        setSize(400, 350);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel ratingPanel = new JPanel();
        ratingPanel.setBorder(BorderFactory.createTitledBorder("Your Rating"));
        for (int i = 0; i < 5; i++) {
            starLabels[i] = new JLabel(emptyStar);
            starLabels[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            final int rating = i;
            starLabels[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { updateStars(rating); }
                @Override public void mouseExited(MouseEvent e) { updateStars(currentRating - 1); }
                @Override public void mouseClicked(MouseEvent e) { currentRating = rating + 1; }
            });
            ratingPanel.add(starLabels[i]);
        }

        JPanel descriptionPanel = new JPanel(new BorderLayout(0, 5));
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description (Optional)"));
        descriptionArea = new JTextArea(5, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> submit());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(submitButton);

        mainPanel.add(ratingPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(descriptionPanel);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateStars(int selectedIndex) {
        for (int i = 0; i < 5; i++) {
            starLabels[i].setIcon(i <= selectedIndex ? filledStar : emptyStar);
        }
    }

    private void submit() {
        if (currentRating == 0) {
            JOptionPane.showMessageDialog(this, "Please select a star rating to submit.", "Rating Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final String description = descriptionArea.getText().trim(); // Made final for use in worker
        submitButton.setEnabled(false);
        submitButton.setText("Submitting...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String sql = "INSERT INTO feedback (name, rating, description) VALUES (?, ?, ?)";
                try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, loggedInUser);
                    pstmt.setInt(2, currentRating);
                    pstmt.setString(3, description.isEmpty() ? null : description);
                    pstmt.executeUpdate();
                    return true;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        StringBuilder stars = new StringBuilder();
                        for (int i = 0; i < 5; i++) stars.append(i < currentRating ? "★" : "☆");
                        String feedbackMessage = "Feedback Submitted:\nRating: " + stars;
                        if (!description.isEmpty()) feedbackMessage += "\n\nDescription:\n" + description;
                        parentFrame.postFeedbackMessage(feedbackMessage);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(FeedbackDialog.this, "Could not submit feedback due to a database error. Check console for details.", "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    submitButton.setEnabled(true);
                    submitButton.setText("Submit");
                }
            }
        };
        worker.execute();
    }
    
    private ImageIcon createStarIcon(boolean filled) {
        int size = 28;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 215, 0));
        int[] xPoints = {14, 17, 27, 19, 22, 14, 6, 9, 1, 11};
        int[] yPoints = {1, 9, 9, 15, 25, 19, 25, 15, 9, 9};
        Polygon star = new Polygon(xPoints, yPoints, 10);
        if (filled) g2.fill(star);
        else {
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(star);
        }
        g2.dispose();
        return new ImageIcon(image);
    }
}