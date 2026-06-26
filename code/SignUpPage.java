import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SignUpPage extends JFrame {
    private JTextField userTextField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/farmer_db";
    private static final String USER = "root";
    private static final String PASS = "sugow@0309";

    /**
     * The main entry point to run this page directly for testing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Load the database driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                // Create and show the sign-up window
                new SignUpPage().setVisible(true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!", "Driver Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public SignUpPage() {
        setTitle("Farmer Sign Up");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(createBackgroundPanel("G:/CropDiseasePrediction/signup.png"));
    }
    
    private void performSignUp() {
        String username = userTextField.getText().trim();
        String password = new String(passField.getPassword());
        String confirmPassword = new String(confirmPassField.getPassword());

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "INSERT INTO farmer (name, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Sign up successful! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            // Ensure a FarmerLoginApp class exists in your project to return to
            new FarmerLoginApp().setVisible(true);
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this, "Username already exists. Please choose another.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error during sign up.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private JPanel createSignUpPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0, 0, 0, 100));
        panel.setOpaque(true);

        // Define the primary font
        Font romanFont = new Font("Times New Roman", Font.BOLD, 24);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Username ---
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(romanFont);
        userLabel.setForeground(Color.BLACK);
        panel.add(userLabel, gbc);

        gbc.gridx = 1; 
        userTextField = new JTextField(15);
        userTextField.setFont(romanFont.deriveFont(Font.PLAIN));
        panel.add(userTextField, gbc);

        // --- Password ---
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(romanFont);
        passLabel.setForeground(Color.BLACK);
        panel.add(passLabel, gbc);
        
        gbc.gridx = 1; 
        passField = new JPasswordField(15);
        passField.setFont(romanFont.deriveFont(Font.PLAIN));
        panel.add(passField, gbc);

        // --- Confirm Password ---
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel confirmPassLabel = new JLabel("Confirm Pass:");
        confirmPassLabel.setFont(romanFont);
        confirmPassLabel.setForeground(Color.BLACK);
        panel.add(confirmPassLabel, gbc);

        gbc.gridx = 1; 
        confirmPassField = new JPasswordField(15);
        confirmPassField.setFont(romanFont.deriveFont(Font.PLAIN));
        panel.add(confirmPassField, gbc);

        // --- Sign Up Button ---
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton signUpButton = new JButton("Create Account");
        signUpButton.setFont(romanFont);
        signUpButton.addActionListener(e -> performSignUp());
        panel.add(signUpButton, gbc);

        return panel;
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
        backgroundPanel.add(createSignUpPanel(), new GridBagConstraints());
        return backgroundPanel;
    }
}