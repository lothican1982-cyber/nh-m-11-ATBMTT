import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSADigitalSignatureApp extends JFrame {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private byte[] documentBytes;
    private byte[] signatureBytes;

    private JTextArea txtPublicKey = new JTextArea(4, 45);
    private JTextArea txtPrivateKey = new JTextArea(4, 45);
    private JTextArea txtInputText = new JTextArea(6, 45);
    private JTextField txtKeySize = new JTextField("2048", 8);
    private JLabel lblDocStatus = new JLabel("[-] Chưa tải văn bản");
    private JLabel lblSigStatus = new JLabel("[-] Chưa tải chữ ký");

    public RSADigitalSignatureApp() {

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }

        setTitle("Hệ Thống Chữ Ký Điện Tử RSA ");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("1. Quản Lý Khóa", createKeyTab());
        tabbedPane.addTab("2. Thực Hiện Ký", createSignTab());
        tabbedPane.addTab("3. Kiểm Tra Toàn Vẹn", createVerifyTab());

        add(tabbedPane);
    }

    private JPanel createKeyTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Kích thước khóa:"));
        topPanel.add(txtKeySize);

        JButton btnAutoKey = new JButton("Tạo khóa Tự động");
        JButton btnManualKey = new JButton("Cập nhật Thủ công");
        topPanel.add(btnAutoKey);
        topPanel.add(btnManualKey);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        JPanel pubPanel = new JPanel(new BorderLayout());
        pubPanel.add(new JLabel("Public Key (Khóa Công Khai - Base64):"), BorderLayout.NORTH);
        txtPublicKey.setLineWrap(true);
        pubPanel.add(new JScrollPane(txtPublicKey), BorderLayout.CENTER);

        JPanel privPanel = new JPanel(new BorderLayout());
        privPanel.add(new JLabel("Private Key (Khóa Bí Mật - Base64):"), BorderLayout.NORTH);
        txtPrivateKey.setLineWrap(true);
        privPanel.add(new JScrollPane(txtPrivateKey), BorderLayout.CENTER);

        centerPanel.add(pubPanel);
        centerPanel.add(privPanel);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        btnAutoKey.addActionListener(e -> {
            try {
                int keySize = Integer.parseInt(txtKeySize.getText().trim());
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(keySize);
                KeyPair pair = keyGen.generateKeyPair();
                privateKey = pair.getPrivate();
                publicKey = pair.getPublic();

                txtPublicKey.setText(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
                txtPrivateKey.setText(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
                JOptionPane.showMessageDialog(this, "Đã tạo khóa tự động thành công!", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "LỖI NHẬP SỐ: Kích thước khóa phải là số nguyên (vd: 1024, 2048)!",
                        "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi tạo khóa: " + ex.getMessage());
            }
        });

        btnManualKey.addActionListener(e -> {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                byte[] publicBytes = Base64.getDecoder().decode(txtPublicKey.getText().trim());
                byte[] privateBytes = Base64.getDecoder().decode(txtPrivateKey.getText().trim());

                publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicBytes));
                privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
                JOptionPane.showMessageDialog(this, "Đã cập nhật khóa thủ công thành công!", "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: Chuỗi khóa thủ công không đúng định dạng Base64!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createSignTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(new JLabel("Nhập nội dung văn bản (Nếu không dùng file có sẵn):"), BorderLayout.NORTH);
        txtInputText.setLineWrap(true);
        centerPanel.add(new JScrollPane(txtInputText), BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnSignKeyboard = new JButton("Nhập Text -> Lưu File & Ký");
        JButton btnLoadFileAndSign = new JButton("Tải File (txt, doc, pdf) & Ký");
        JButton btnSaveSignature = new JButton("Lưu file Chữ Ký (.sig)");

        btnSignKeyboard.setBackground(new Color(173, 216, 230));
        btnSaveSignature.setBackground(new Color(144, 238, 144));

        bottomPanel.add(btnSignKeyboard);
        bottomPanel.add(btnLoadFileAndSign);
        bottomPanel.add(btnSaveSignature);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        btnSignKeyboard.addActionListener(e -> {
            try {
                if (privateKey == null)
                    throw new Exception("Vui lòng tạo Private Key ở Tab 1 trước!");
                String text = txtInputText.getText();
                if (text.isEmpty())
                    throw new Exception("Văn bản đang trống!");

                JOptionPane.showMessageDialog(this, "Vui lòng chọn nơi lưu file văn bản vừa nhập.");
                File file = chooseFile(true, "Lưu file văn bản (vd: vanban.txt)");
                if (file != null) {
                    Files.write(file.toPath(), text.getBytes("UTF-8"));
                    documentBytes = text.getBytes("UTF-8");
                    signatureBytes = signData(documentBytes, privateKey);
                    JOptionPane.showMessageDialog(this, "Đã lưu file văn bản và tạo chữ ký xong!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnLoadFileAndSign.addActionListener(e -> {
            try {
                if (privateKey == null)
                    throw new Exception("Vui lòng tạo Private Key trước!");
                File file = chooseFile(false, "Chọn file văn bản cần ký");
                if (file != null) {
                    documentBytes = Files.readAllBytes(file.toPath());
                    signatureBytes = signData(documentBytes, privateKey);
                    JOptionPane.showMessageDialog(this, "Đã tải file " + file.getName() + " và tạo chữ ký xong!",
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSaveSignature.addActionListener(e -> {
            try {
                if (signatureBytes == null)
                    throw new Exception("Chưa có chữ ký nào được tạo!");
                File file = chooseFile(true, "Lưu file chữ ký (vd: chuky.sig)");
                if (file != null) {
                    Files.write(file.toPath(), signatureBytes);
                    JOptionPane.showMessageDialog(this, "Đã lưu file chữ ký thành công!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createVerifyTab() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(new EmptyBorder(40, 80, 40, 80));

        JButton btnLoadDoc = new JButton("1. Tải file Văn Bản cần kiểm tra");
        JButton btnLoadSig = new JButton("2. Tải file Chữ Ký");
        JButton btnVerify = new JButton("3. KIỂM TRA TÍNH TOÀN VẸN");

        btnVerify.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnVerify.setForeground(new Color(0, 51, 153));

        panel.add(btnLoadDoc);
        panel.add(lblDocStatus);
        panel.add(btnLoadSig);
        panel.add(lblSigStatus);

        JPanel verifyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        verifyPanel.add(btnVerify);
        panel.add(new JLabel(""));
        panel.add(verifyPanel);

        btnLoadDoc.addActionListener(e -> {
            File file = chooseFile(false, "Chọn file văn bản");
            if (file != null) {
                try {
                    documentBytes = Files.readAllBytes(file.toPath());
                    lblDocStatus.setText("[OK] Đã tải Văn Bản: " + file.getName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnLoadSig.addActionListener(e -> {
            File file = chooseFile(false, "Chọn file chữ ký");
            if (file != null) {
                try {
                    signatureBytes = Files.readAllBytes(file.toPath());
                    lblSigStatus.setText("[OK] Đã tải Chữ Ký: " + file.getName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnVerify.addActionListener(e -> {
            if (publicKey == null || documentBytes == null || signatureBytes == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng tải đủ: Public Key (Tab 1), File Văn Bản, File Chữ Ký!",
                        "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update(documentBytes);

                boolean isValid = sig.verify(signatureBytes);

                if (isValid) {
                    JOptionPane.showMessageDialog(this, "Chữ ký hợp lệ. Văn bản nguyên vẹn!", "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {

                    JOptionPane.showMessageDialog(this, "văn bản không toàn vẹn!", "Cảnh báo",
                            JOptionPane.WARNING_MESSAGE);
                }
            } catch (SignatureException ex) {

                JOptionPane.showMessageDialog(this,
                        "chữ ký không hợp lệ!\n(Hoặc: văn bản không toàn vẹn và chữ ký không hợp lệ!)",
                        "Lỗi Nghiêm Trọng", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi kiểm tra hệ thống: " + ex.getMessage());
            }
        });

        return panel;
    }

    private byte[] signData(byte[] data, PrivateKey key) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(data);
        return sig.sign();
    }

    private File chooseFile(boolean isSave, String title) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        int result = isSave ? fileChooser.showSaveDialog(this) : fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new RSADigitalSignatureApp().setVisible(true);
        });
    }
}