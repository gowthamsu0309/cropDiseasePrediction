import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ForgotPasswordPage extends JFrame {
    private JTextField userTextField;
    private JPasswordField newPassField;
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
                // Create and show the forgot password window
                new ForgotPasswordPage().setVisible(true);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "MySQL JDBC Driver not found!", "Driver Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public ForgotPasswordPage() {
        setTitle("Reset Password");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setContentPane(createBackgroundPanel("G:/CropDiseasePrediction/password.png"));
    }

    private void performPasswordUpdate() {
        String username = userTextField.getText().trim();
        String newPassword = new String(newPassField.getPassword());
        String confirmPassword = new String(confirmPassField.getPassword());

        if (username.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "UPDATE farmer SET password = ? WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Password updated successfully! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                this.dispose();
                // Ensure a FarmerLoginApp class exists in your project to return to
                new FarmerLoginApp().setVisible(true); 
            } else {
                JOptionPane.showMessageDialog(this, "Username not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error during password update.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createResetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0, 0, 0, 80));
        panel.setOpaque(true);

        // Define the primary font
        Font romanFont = new Font("Times New Roman", Font.BOLD, 30);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // --- Username ---
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(romanFont);
        userLabel.setForeground(Color.WHITE);
        panel.add(userLabel, gbc);

        gbc.gridx = 1; 
        userTextField = new JTextField(15);
        userTextField.setFont(romanFont.deriveFont(Font.BOLD));
        panel.add(userTextField, gbc);

        // --- New Password ---
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel newPassLabel = new JLabel("New Password:");
        newPassLabel.setFont(romanFont);
        newPassLabel.setForeground(Color.WHITE);
        panel.add(newPassLabel, gbc);

        gbc.gridx = 1; 
        newPassField = new JPasswordField(15);
        newPassField.setFont(romanFont.deriveFont(Font.BOLD));
        panel.add(newPassField, gbc);

        // --- Confirm Password ---
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel confirmPassLabel = new JLabel("Confirm Pass:");
        confirmPassLabel.setFont(romanFont);
        confirmPassLabel.setForeground(Color.WHITE);
        panel.add(confirmPassLabel, gbc);

        gbc.gridx = 1; 
        confirmPassField = new JPasswordField(15);
        confirmPassField.setFont(romanFont.deriveFont(Font.BOLD));
        panel.add(confirmPassField, gbc);

        // --- Update Button ---
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JButton updateButton = new JButton("Update Password");
        updateButton.setFont(romanFont);
        updateButton.setForeground(Color.WHITE);
        updateButton.setBackground(Color.RED);
        
        updateButton.addActionListener(e -> performPasswordUpdate());
        panel.add(updateButton, gbc);
        
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
        backgroundPanel.add(createResetPanel(), new GridBagConstraints());
        return backgroundPanel;
    }
}