import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FarmerLoginApp extends JFrame {

    // --- UI Components ---
    private JTextField userTextField;
    private JPasswordField passField;

    // --- Database Credentials ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/farmer_db";
    private static final String USER = "root";
    private static final String PASS = "sugow@0309";

    /**
     * The main entry point for the entire application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                new FarmerLoginApp().setVisible(true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!", "Driver Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public FarmerLoginApp() {
        setTitle("Farmer Login");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(createBackgroundPanel("G:/CropDiseasePrediction/login.jpg"));
    }

    private void validateLogin() {
        String username = userTextField.getText().trim();
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
                    // Pass the logged-in username to the chatbot frame
                    // Make sure a SmartAgriChat class exists in your project that accepts a username
                    new SmartAgriChat(username).setVisible(true); 
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during login.", "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

 
    private JPanel createLoginPanel() {
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

        gbc.gridx = 1;
        userTextField = new JTextField(15);
        userTextField.setFont(romanFont.deriveFont(Font.PLAIN)); // Keep input text plain
        loginPanel.add(userTextField, gbc);

      
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        passLabel.setFont(romanFont);
        loginPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passField = new JPasswordField(15);
        passField.setFont(romanFont.deriveFont(Font.PLAIN)); // Keep input text plain
        loginPanel.add(passField, gbc);

        // --- Login Button ---
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 2;
        JButton loginButton = createStyledButton("Login", new Color(76, 175, 80), romanFont);
        loginButton.addActionListener(e -> validateLogin());
        loginPanel.add(loginButton, gbc);

        // --- Panel for Sign Up and Forgot Password buttons ---
        JPanel extraButtonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        extraButtonsPanel.setOpaque(false);

        // --- MODIFICATION IS HERE ---
        // Create a plain version of the font for secondary buttons, but keep the size at 24.
        Font secondaryButtonFont = new Font("Times New Roman", Font.PLAIN, 24);

        JButton signUpButton = createStyledButton("Sign Up", new Color(0, 123, 255), secondaryButtonFont);
        signUpButton.addActionListener(e -> {
            this.dispose();
            // Ensure you have a SignUpPage class in your project
             new SignUpPage().setVisible(true); 
        });

        JButton forgotPassButton = createStyledButton("Forgot Password", new Color(220, 53, 69), secondaryButtonFont);
        forgotPassButton.addActionListener(e -> {
            this.dispose();
            // Ensure you have a ForgotPasswordPage class in your project
             new ForgotPasswordPage().setVisible(true);
        });

        extraButtonsPanel.add(signUpButton);
        extraButtonsPanel.add(forgotPassButton);

        gbc.gridy = 3;
        loginPanel.add(extraButtonsPanel, gbc);

        return loginPanel;
    }

    private JButton createStyledButton(String text, Color color, Font font) {
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
    
    private JPanel createBackgroundPanel(String imagePath) {
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