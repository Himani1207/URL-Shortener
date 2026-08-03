# 🔗 Linkly – URL Shortener & Analytics Platform

Linkly is a full-stack URL shortening platform that enables users to create, manage, and monitor shortened URLs through a secure and responsive web application. The platform provides custom aliases, password-protected links, QR code generation, link expiration, Redis caching, and detailed click analytics using a Spring Boot backend and React frontend.

---

## ✨ Features

- 🔐 JWT-based Authentication & Authorization
- 🔗 Generate short URLs with custom aliases
- 🔒 Password-protected links
- ⏳ Link expiration support
- 📱 QR code generation with customization
- 📊 Click analytics (browser, operating system, device & total clicks)
- 📈 Analytics dashboard
- ⚡ Redis caching for faster URL redirection
- 📖 Interactive API documentation with Swagger/OpenAPI
- 📱 Fully responsive UI for desktop, tablet, and mobile

---

## 🛠 Tech Stack

### Frontend

- React.js
- Vite
- JavaScript
- Axios
- React Router
- CSS

### Backend

- Java 21
- Spring Boot
- Spring Security
- JWT Authentication
- Spring Data JPA
- Maven

### Database

- PostgreSQL

### Cache

- Redis

### Tools

- Swagger / OpenAPI
- Docker
- Git & GitHub
- Postman

---

## 📂 Project Structure

```text
URL-Shortener
│
├── backend
│   ├── Controller
│   ├── Service
│   ├── Repository
│   ├── Entity
│   ├── Security
│   ├── DTO
│   └── Config
│
├── frontend
│   ├── components
│   ├── pages
│   ├── services
│   ├── context
│   └── assets
│
└── README.md
```

---

## 🚀 Core Functionalities

### Authentication

- User Registration
- User Login
- JWT Authentication
- Protected APIs

### URL Management

- Create Short URL
- Custom Alias
- Update Link
- Delete Link
- Activate / Deactivate Link
- Password Protection
- Expiration Date

### QR Code

- Generate QR Code
- Download QR Code
- Customize QR Color
- Customize QR Size

### Analytics

- Total Clicks
- Browser Analytics
- Device Analytics
- Operating System Analytics
- Click History

### Performance

- Redis Cache
- Optimized URL Redirection
- RESTful API Architecture

---

## ⚙️ Local Setup

### Clone Repository

```bash
git clone https://github.com/Himani1207/URL-Shortener.git
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## 🎯 Project Highlights

- Built a scalable full-stack URL shortening platform using Spring Boot and React.
- Implemented JWT-based authentication and secure REST APIs.
- Designed PostgreSQL database schema for users, URLs, and click analytics.
- Integrated Redis caching to improve URL redirection performance.
- Developed QR code generation with customization options.
- Built a responsive dashboard for link management and analytics.
- Documented REST APIs using Swagger/OpenAPI.
---


## 📄 License

This project is licensed under the MIT License.
