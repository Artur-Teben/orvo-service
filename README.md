# Orvo – AI-Powered Email Generator & Verifier

Orvo is a lightweight email lead generation tool that:
- Generates likely business email addresses based on name + domain
- Verifies them using low-level SMTP handshake (no third-party services)
- Returns only potentially deliverable leads

🚀 Built with Java, Spring Boot, and Jakarta Mail  
📫 Hosted on Railway  
🌐 Domain: `orvoro.site`

---

## ⚙️ Features

- Upload `.csv` or `.xlsx` files with lead data
- Generate business emails using multiple patterns (e.g. `first.last@domain.com`)
- Validate emails via real-time SMTP handshake
- Returns list of valid emails + statuses
- Built-in dashboard-ready JSON response structure
- Custom `noreply@orvoro.site` domain for SMTP `MAIL FROM`
- No third-party APIs – full control

---

## 📦 Tech Stack

- **Backend**: Java 17, Spring Boot 3
- **SMTP**: Jakarta Mail (manual RCPT TO handshake)
- **Database**: PostgreSQL
- **Build Tool**: Gradle
- **Deployment**: Railway
- **Email Domain**: Namecheap (Private Email)
- **Input**: `.csv`, `.xlsx` file support via Apache POI

---

## 📁 Example File Format

Upload files with this structure:

```csv
first_name,last_name,company_name,company_domain
Tim,Cook,Apple,apple.com
Jane,Doe,Acme,acme.io
```

---

## 🧠 Email Generation Patterns

Orvo tries multiple common patterns:
- `{first}.{last}@domain.com`
- `{first}{last}@domain.com`
- `{f}{last}@domain.com`
- `{first}@domain.com`
- `{first}.{l}@domain.com`

Each is tested via RCPT TO against the domain’s mail server.

---

## 🔐 Email Verification (How It Works)

- DNS MX lookup for domain (e.g. `apple.com`)
- SMTP handshake:
  - `MAIL FROM: noreply@orvoro.site`
  - `RCPT TO: user@company.com`
- Accepts if server responds with `250` or `251`
- No `DATA` sent — email is **not** actually delivered

---

## 🔧 How to Run Locally

1. Clone the repo
2. Start the app:
```bash
./mvnw clean spring-boot:run
```

---

## 🧪 Testing

To run unit tests:

```bash
./mvnw test
```

---

## 📄 License

Licensed under the MIT License.


---

👨‍💻 Built with ❤️ by Artur T
