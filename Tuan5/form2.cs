using System;
using System.Drawing;
using System.Windows.Forms;

namespace WindowsFormsApp1
{
    public partial class Form1 : Form
    {
        private long p, q, n, phi, eKey, dKey;

        private Button btnGenerate;
        private Button btnSign;
        private Button btnVerify;

        private TextBox txtP;
        private TextBox txtQ;

        private TextBox txtPublicKey;
        private TextBox txtPrivateKey;
        private RichTextBox txtMessage;
        private TextBox txtSignature;
        private Label lblResult;

        public Form1()
        {
            InitializeComponent();
            CreateUI();
        }

        // ================= UI =================
        private void CreateUI()
        {
            this.Text = "Chữ ký điện tử RSA";
            this.Size = new Size(1000, 700);
            this.StartPosition = FormStartPosition.CenterScreen;

            Label title = new Label();
            title.Text = "CHỮ KÝ ĐIỆN TỬ RSA";
            title.Font = new Font("Segoe UI", 20, FontStyle.Bold);
            title.Location = new Point(300, 20);
            title.AutoSize = true;
            this.Controls.Add(title);

            // ===== KEY =====
            GroupBox gbKey = new GroupBox();
            gbKey.Text = "Sinh khóa";
            gbKey.Size = new Size(920, 180);
            gbKey.Location = new Point(30, 70);
            this.Controls.Add(gbKey);

            Label lblP = new Label();
            lblP.Text = "p:";
            lblP.Location = new Point(20, 40);
            lblP.AutoSize = true;
            gbKey.Controls.Add(lblP);

            txtP = new TextBox();
            txtP.Location = new Point(50, 38);
            txtP.Width = 80;
            gbKey.Controls.Add(txtP);

            Label lblQ = new Label();
            lblQ.Text = "q:";
            lblQ.Location = new Point(150, 40);
            lblQ.AutoSize = true;
            gbKey.Controls.Add(lblQ);

            txtQ = new TextBox();
            txtQ.Location = new Point(180, 38);
            txtQ.Width = 80;
            gbKey.Controls.Add(txtQ);

            btnGenerate = new Button();
            btnGenerate.Text = "Tạo khóa";
            btnGenerate.Location = new Point(300, 35);
            btnGenerate.Click += BtnGenerate_Click;
            gbKey.Controls.Add(btnGenerate);

            Label lblPub = new Label();
            lblPub.Text = "Khóa công khai:";
            lblPub.Location = new Point(20, 90);
            gbKey.Controls.Add(lblPub);

            txtPublicKey = new TextBox();
            txtPublicKey.Location = new Point(120, 85);
            txtPublicKey.Width = 300;
            gbKey.Controls.Add(txtPublicKey);

            Label lblPri = new Label();
            lblPri.Text = "Khóa bí mật:";
            lblPri.Location = new Point(440, 90);
            gbKey.Controls.Add(lblPri);

            txtPrivateKey = new TextBox();
            txtPrivateKey.Location = new Point(540, 85);
            txtPrivateKey.Width = 300;
            gbKey.Controls.Add(txtPrivateKey);

            // ===== MESSAGE =====
            GroupBox gbSign = new GroupBox();
            gbSign.Text = "Ký số";
            gbSign.Size = new Size(920, 220);
            gbSign.Location = new Point(30, 270);
            this.Controls.Add(gbSign);

            txtMessage = new RichTextBox();
            txtMessage.Location = new Point(20, 40);
            txtMessage.Size = new Size(860, 80);
            gbSign.Controls.Add(txtMessage);

            btnSign = new Button();
            btnSign.Text = "Tạo chữ ký";
            btnSign.Location = new Point(20, 140);
            btnSign.Click += BtnSign_Click;
            gbSign.Controls.Add(btnSign);

            txtSignature = new TextBox();
            txtSignature.Location = new Point(200, 145);
            txtSignature.Width = 500;
            gbSign.Controls.Add(txtSignature);

            // ===== VERIFY =====
            GroupBox gbVerify = new GroupBox();
            gbVerify.Text = "Xác thực";
            gbVerify.Size = new Size(920, 120);
            gbVerify.Location = new Point(30, 510);
            this.Controls.Add(gbVerify);

            btnVerify = new Button();
            btnVerify.Text = "Xác thực";
            btnVerify.Location = new Point(20, 40);
            btnVerify.Click += BtnVerify_Click;
            gbVerify.Controls.Add(btnVerify);

            lblResult = new Label();
            lblResult.Text = "Kết quả:";
            lblResult.Location = new Point(200, 45);
            lblResult.AutoSize = true;
            gbVerify.Controls.Add(lblResult);
        }

        // ================= TẠO KHÓA =================
        private void BtnGenerate_Click(object sender, EventArgs e)
        {
            if (!long.TryParse(txtP.Text, out p) ||
                !long.TryParse(txtQ.Text, out q))
            {
                MessageBox.Show("Vui lòng nhập p và q hợp lệ!");
                return;
            }

            if (!IsPrime(p) || !IsPrime(q))
            {
                MessageBox.Show("p và q phải là số nguyên tố!");
                return;
            }

            n = p * q;
            phi = (p - 1) * (q - 1);

            eKey = 2;
            long x, y;

            while (eKey < phi)
            {
                if (ExtendedGCD(eKey, phi, out x, out y) == 1)
                    break;
                eKey++;
            }

            dKey = ModInverse(eKey, phi);

            txtPublicKey.Text = $"Public (e={eKey}, n={n})";
            txtPrivateKey.Text = $"Private (d={dKey}, n={n})";
        }

        // ================= KÝ =================
        private void BtnSign_Click(object sender, EventArgs e)
        {
            if (string.IsNullOrWhiteSpace(txtMessage.Text))
            {
                MessageBox.Show("Nhập thông điệp cần ký!");
                return;
            }

            long m = Hash(txtMessage.Text);

            long signature = Power(m, dKey, n);

            txtSignature.Text = signature.ToString();
        }

        // ================= XÁC THỰC =================
        private void BtnVerify_Click(object sender, EventArgs e)
        {
            if (string.IsNullOrWhiteSpace(txtSignature.Text))
            {
                MessageBox.Show("Chưa có chữ ký!");
                return;
            }

            long m = Hash(txtMessage.Text);
            long signature = long.Parse(txtSignature.Text);

            long verify = Power(signature, eKey, n);

            if (verify == m)
            {
                lblResult.Text = "Kết quả: HỢP LỆ ✔";
                lblResult.ForeColor = Color.Green;
            }
            else
            {
                lblResult.Text = "Kết quả: KHÔNG HỢP LỆ ✘";
                lblResult.ForeColor = Color.Red;
            }
        }

        // ================= HASH =================
        private long Hash(string msg)
        {
            long h = 0;

            foreach (char c in msg)
                h += c;

            return h % n;
        }

        // ================= KIỂM TRA NGUYÊN TỐ =================
        private bool IsPrime(long num)
        {
            if (num < 2) return false;

            for (long i = 2; i * i <= num; i++)
            {
                if (num % i == 0)
                    return false;
            }

            return true;
        }

        // ================= LŨY THỪA MOD =================
        public static long Power(long baseVal, long exp, long mod)
        {
            long res = 1;
            baseVal %= mod;

            while (exp > 0)
            {
                if ((exp & 1) == 1)
                    res = (res * baseVal) % mod;

                baseVal = (baseVal * baseVal) % mod;
                exp >>= 1;
            }

            return res;
        }

        // ================= EUCLID MỞ RỘNG =================
        public static long ExtendedGCD(long a, long b, out long x, out long y)
        {
            if (b == 0)
            {
                x = 1;
                y = 0;
                return a;
            }

            long x1 = 0, x2 = 1;
            long y1 = 1, y2 = 0;

            while (b > 0)
            {
                long q = a / b;
                long r = a % b;

                long xt = x2 - q * x1;
                long yt = y2 - q * y1;

                a = b;
                b = r;

                x2 = x1;
                x1 = xt;

                y2 = y1;
                y1 = yt;
            }

            x = x2;
            y = y2;

            return a;
        }

        // ================= NGHỊCH ĐẢO MOD =================
        public static long ModInverse(long a, long n)
        {
            long x, y;

            if (ExtendedGCD(a, n, out x, out y) != 1)
                return -1;

            return (x % n + n) % n;
        }
    }
}