import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;

public class MainApp {

    // --- Centralized Database Credentials ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/farmer_db";
    private static final String USER = "root";
    private static final String PASS = "vishwa@67";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                new FarmerLoginApp().setVisible(true);
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!", "Driver Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // =================================================================================
    // FARMER LOGIN SYSTEM CLASSES
    // =================================================================================

    static class FarmerLoginApp extends JFrame {
        private JTextField userTextField;
        private JPasswordField passField;

        public FarmerLoginApp() {
            setTitle("Farmer Login");
            setSize(800, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setContentPane(createBackgroundPanel("D:/GOWTHAM SE/loginimage.jpg"));
        }

        private void validateLogin() {
            String username = userTextField.getText();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username/Password cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "SELECT * FROM farmer WHERE name = ? AND password = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "Login Successful! Welcome, " + username + ".", "Success", JOptionPane.INFORMATION_MESSAGE);
                        this.dispose();
                        // --- MODIFICATION: Pass the username to the chatbot frame ---
                        new SmartAgriChat(username).setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error during login.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private JPanel createLoginPanel() { /* ... UI code for login panel ... */ 
            JPanel loginPanel = new JPanel(new GridBagLayout());
            loginPanel.setBackground(new Color(0, 0, 0, 80));
            loginPanel.setOpaque(true);
            Font romanFont = new Font("Times New Roman", Font.BOLD, 24);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 15, 10, 15);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;
            JLabel userLabel = new JLabel("Username:");
            userLabel.setForeground(Color.WHITE);
            userLabel.setFont(romanFont);
            loginPanel.add(userLabel, gbc);
            gbc.gridx = 1; userTextField = new JTextField(15);
            userTextField.setFont(romanFont.deriveFont(Font.PLAIN));
            loginPanel.add(userTextField, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            JLabel passLabel = new JLabel("Password:");
            passLabel.setForeground(Color.WHITE);
            passLabel.setFont(romanFont);
            loginPanel.add(passLabel, gbc);
            gbc.gridx = 1; passField = new JPasswordField(15);
            passField.setFont(romanFont.deriveFont(Font.PLAIN));
            loginPanel.add(passField, gbc);
            gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 2;
            JButton loginButton = createStyledButton("Login", new Color(76, 175, 80), romanFont);
            loginButton.addActionListener(e -> validateLogin());
            loginPanel.add(loginButton, gbc);
            return loginPanel;
        }

        private JButton createStyledButton(String text, Color color, Font font) { /* ... UI code for button ... */ 
            JButton button = new JButton(text);
            button.setFont(font);
            button.setForeground(Color.WHITE);
            button.setBackground(color);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setOpaque(true);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            Color hoverColor = color.darker();
            button.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) { button.setBackground(hoverColor); }
                public void mouseExited(MouseEvent evt) { button.setBackground(color); }
            });
            return button;
        }
        
        private JPanel createBackgroundPanel(String imagePath) { /* ... UI code for background ... */ 
            JPanel backgroundPanel = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    ImageIcon imageIcon = new ImageIcon(imagePath);
                    if (imageIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
                        g.setColor(Color.DARK_GRAY);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    } else {
                        g.drawImage(imageIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
                    }
                }
            };
            backgroundPanel.add(createLoginPanel(), new GridBagConstraints());
            return backgroundPanel;
        }
    }

    // =================================================================================
    // SMART AGRI CHATBOT CLASSES
    // =================================================================================
    public boolean check(String s,String p) throws SQLException
    {
        
        String sql = "SELECT * FROM farmer WHERE name = ? AND password = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, s);
                pstmt.setString(2, p);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) 
                        return true;
                    else
                       return false;
                }
                    
    }
    }
    static class SmartAgriChat extends JFrame {
        private final JPanel chatPanel;
        private final JTextField userInputField;
        private final Chatbot chatbot = new Chatbot();
        // --- MODIFICATION: Store the logged-in user's name ---
        private final String loggedInUser;

        public SmartAgriChat(String username) {
            // --- MODIFICATION: Store the username passed from the login screen ---
            this.loggedInUser = username;

            setTitle("🌾 SmartAgriChat — Welcome, " + username);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(800, 700);
            setLocationRelativeTo(null);

            // ... The rest of the ChatFrame UI setup remains the same ...
            JPanel backgroundPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    String imagePath = "D:/GOWTHAM SE/chatbot.png";
                    ImageIcon imageIcon = new ImageIcon(imagePath);
                    if (imageIcon.getImageLoadStatus() == MediaTracker.ERRORED) {
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
            JScrollPane scrollPane = new JScrollPane(chatPanel);
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
            uploadButton.addActionListener(e -> uploadImage());
            inputPanel.add(emojiPanel, BorderLayout.WEST);
            inputPanel.add(userInputField, BorderLayout.CENTER);
            inputPanel.add(uploadButton, BorderLayout.EAST);
            JPanel feedbackPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            feedbackPanel.setOpaque(false);
            JButton giveFeedbackButton = new JButton("Give Feedback");
            giveFeedbackButton.addActionListener(e -> {
                // --- MODIFICATION: Pass the username to the feedback dialog ---
                FeedbackDialog feedbackDialog = new FeedbackDialog(this, loggedInUser);
                feedbackDialog.setVisible(true);
            });
            feedbackPanel.add(giveFeedbackButton);
            bottomPanel.add(inputPanel, BorderLayout.NORTH);
            bottomPanel.add(feedbackPanel, BorderLayout.SOUTH);
            backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);
            addBotMessage("Hello! I'm SmartAgriChat. How can I assist you with your crops today?");
        }
        
        public void postFeedbackMessage(String formattedFeedback) {
            addUserMessage(formattedFeedback);
            simulateTypingAndReply("Thank you for your valuable feedback!");
        }

        private void sendMessage() { /* ... unchanged ... */ 
            String userText = userInputField.getText().trim();
            if (!userText.isEmpty()) {
                addUserMessage(userText);
                userInputField.setText("");
                String botResponse = chatbot.getResponse(userText);
                simulateTypingAndReply(botResponse);
            }
        }
        
        private void uploadImage() { /* ... unchanged ... */ 
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "png", "jpeg"));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                ImageIcon imageIcon = new ImageIcon(selectedFile.getAbsolutePath());
                addUserMessage("Here is my crop image.", imageIcon);
                simulateTypingAndReply("Analyzing image...");
                Timer timer = new Timer(2000, e -> addBotMessage("Possible disease detected: Leaf Blight."));
                timer.setRepeats(false);
                timer.start();
            }
        }

        private void simulateTypingAndReply(String message) { /* ... unchanged ... */
            Timer timer = new Timer(1000, e -> addBotMessage(message));
            timer.setRepeats(false);
            timer.start();
        }

        private void addMessage(String message, ImageIcon image, boolean isUser) { /* ... unchanged ... */
            JPanel alignPanel = new JPanel(new BorderLayout());
            alignPanel.setOpaque(false);
            JPanel bubble = new JPanel(new BorderLayout(5, 5));
            bubble.setBackground(isUser ? new Color(220, 248, 198) : Color.WHITE);
            bubble.setBorder(new BubbleBorder(isUser ? new Color(200, 230, 180) : Color.LIGHT_GRAY, 2, 16, 0));
            JTextPane messagePane = new JTextPane();
            messagePane.setEditable(false);
            messagePane.setFont(new Font("Times New Roman", Font.PLAIN, 18));
            messagePane.setText(message);
            messagePane.setOpaque(false);
            messagePane.setBorder(new EmptyBorder(5, 10, 5, 10));
            bubble.add(messagePane, BorderLayout.CENTER);
            JLabel timestampLabel = new JLabel(new SimpleDateFormat("hh:mm a").format(new Date()));
            timestampLabel.setFont(new Font("Arial", Font.BOLD, 11));
            timestampLabel.setForeground(Color.BLACK);
            timestampLabel.setBorder(new EmptyBorder(0, 10, 5, 10));
            bubble.add(timestampLabel, BorderLayout.SOUTH);
            if (image != null) {
                Image scaledImage = image.getImage().getScaledInstance(150, -1, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                imageLabel.setBorder(new EmptyBorder(10, 10, 0, 10));
                bubble.add(imageLabel, BorderLayout.NORTH);
            }
            if (isUser) { alignPanel.add(bubble, BorderLayout.EAST); } else { alignPanel.add(bubble, BorderLayout.WEST); }
            chatPanel.add(alignPanel);
            chatPanel.add(Box.createVerticalStrut(10));
            chatPanel.revalidate();
            JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        }

        private void addUserMessage(String message) { addMessage(message, null, true); }
        private void addUserMessage(String message, ImageIcon image) { addMessage(message, image, true); }
        private void addBotMessage(String message) { addMessage(message, null, false); }
    }

    static class Chatbot { /* ... unchanged ... */
        public String getResponse(String userInput) {
            String lowerCaseInput = userInput.toLowerCase();
            if (lowerCaseInput.contains("hello") || lowerCaseInput.contains("hi")) { return getGreeting(); }
            if (lowerCaseInput.contains("leaf spot")) { return "To treat leaf spot, ensure proper air circulation, avoid watering leaves directly, and consider a fungicide."; }
            if (lowerCaseInput.contains("leaves yellow")) { return "Yellow leaves can indicate a nutrient deficiency, overwatering, or underwatering. Check soil moisture first."; }
            if (lowerCaseInput.contains("aphids")) { return "Aphids can be controlled by spraying with soapy water or introducing natural predators like ladybugs. 🐞"; }
            if (lowerCaseInput.contains("bye") || lowerCaseInput.contains("thank you")) { return "You're welcome! Happy farming! 🌱"; }
            return "I'm sorry, I don't have information on that. You can ask about pests, fertilizer, or watering. 🌾";
        }
        private String getGreeting() {
            LocalTime now = LocalTime.now();
            if (now.isBefore(LocalTime.NOON)) { return "Good morning! How can I help you with your crops today? 🌾"; }
            if (now.isBefore(LocalTime.of(18, 0))) { return "Good afternoon! What crop questions do you have?"; }
            return "Good evening! Let's solve your farming queries.";
        }
    }
    
    static class BubbleBorder extends EmptyBorder { /* ... unchanged ... */
        private final Color color; private final int thickness; private final int radius;
        BubbleBorder(Color color, int thickness, int radius, int pointerSize) {
            super(thickness, thickness, thickness, thickness);
            this.color = color; this.thickness = thickness; this.radius = radius;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(this.color); g2d.setStroke(new BasicStroke(this.thickness));
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius); g2d.dispose();
        }
    }

    static class FeedbackDialog extends JDialog {
        private final JLabel[] starLabels = new JLabel[5];
        private final JTextArea descriptionArea;
        private final SmartAgriChat parentFrame;
        private int currentRating = 0;
        // --- MODIFICATION: Store the logged-in user's name ---
        private final String loggedInUser;
        
        private final ImageIcon emptyStar;
        private final ImageIcon filledStar;

        public FeedbackDialog(SmartAgriChat parent, String username) {
            super(parent, "Submit Feedback", true);
            this.parentFrame = parent;
            // --- MODIFICATION: Receive and store the username ---
            this.loggedInUser = username;
            
            emptyStar = createStarIcon(false);
            filledStar = createStarIcon(true);

            setSize(400, 350);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));
            
            // ... The rest of the FeedbackDialog UI setup remains the same ...
            JPanel mainPanel = new JPanel(); mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            JPanel ratingPanel = new JPanel(); ratingPanel.setBorder(BorderFactory.createTitledBorder("Your Rating"));
            for (int i = 0; i < 5; i++) {
                starLabels[i] = new JLabel(emptyStar); starLabels[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
                final int rating = i;
                starLabels[i].addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { updateStars(rating); }
                    public void mouseExited(MouseEvent e) { updateStars(currentRating - 1); }
                    public void mouseClicked(MouseEvent e) { currentRating = rating + 1; }
                });
                ratingPanel.add(starLabels[i]);
            }
            JPanel descriptionPanel = new JPanel(new BorderLayout(0, 5)); descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description (Optional)"));
            descriptionArea = new JTextArea(5, 30); descriptionArea.setLineWrap(true); descriptionArea.setWrapStyleWord(true);
            descriptionPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);
            JButton submitButton = new JButton("Submit"); submitButton.addActionListener(e -> submit());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); buttonPanel.add(submitButton);
            mainPanel.add(ratingPanel); mainPanel.add(Box.createVerticalStrut(10)); mainPanel.add(descriptionPanel);
            add(mainPanel, BorderLayout.CENTER); add(buttonPanel, BorderLayout.SOUTH);
        }

        private void updateStars(int selectedIndex) { /* ... unchanged ... */ 
            for (int i = 0; i < 5; i++) {
                starLabels[i].setIcon(i <= selectedIndex ? filledStar : emptyStar);
            }
        }

        private void submit() {
            if (currentRating == 0) {
                JOptionPane.showMessageDialog(this, "Please select a star rating to submit.", "Rating Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String description = descriptionArea.getText().trim();
            
            // --- MODIFICATION: Insert feedback into the database ---
            String sql = "INSERT INTO feedback (name, rating, description) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, loggedInUser);
                pstmt.setInt(2, currentRating);
                pstmt.setString(3, description.isEmpty() ? null : description);
                pstmt.executeUpdate();
                
                System.out.println("Feedback successfully stored in the database for user: " + loggedInUser);

            } catch (SQLException ex) {
                System.err.println("Database error while submitting feedback.");
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Could not submit feedback due to a database error.", "Database Error", JOptionPane.ERROR_MESSAGE);
                return; // Stop if DB insertion fails
            }

            // Build the formatted feedback string to show in the chat
            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stars.append(i < currentRating ? "★" : "☆");
            }
            String feedbackMessage = "Feedback Submitted:\nRating: " + stars.toString();
            if (!description.isEmpty()) {
                feedbackMessage += "\n\nDescription:\n" + description;
            }
            parentFrame.postFeedbackMessage(feedbackMessage);
            
            dispose();
        }
        
        private ImageIcon createStarIcon(boolean filled) { /* ... unchanged ... */
            int size = 28;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 215, 0)); // Gold color
            int[] xPoints = {14, 17, 27, 19, 22, 14, 6, 9, 1, 11};
            int[] yPoints = {1, 9, 9, 15, 25, 19, 25, 15, 9, 9};
            Polygon star = new Polygon(xPoints, yPoints, 10);
            if (filled) { g2.fill(star); } else { g2.setStroke(new BasicStroke(1.5f)); g2.draw(star); }
            g2.dispose();
            return new ImageIcon(image);
        }
    }
}