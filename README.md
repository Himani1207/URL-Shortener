# 🔗 URL Shortener

A full-stack URL Shortener application that allows users to create, manage, and track shortened URLs. The platform provides secure authentication, custom short links, QR code generation, and click analytics through a modern dashboard.

## 🚀 Features

- User Authentication (JWT)
- Create and manage shortened URLs
- Custom short aliases
- QR Code generation for every short link
- Click analytics and visit tracking
- Dashboard with URL statistics
- Responsive and modern UI
- RESTful API architecture

## 🛠️ Tech Stack

### Frontend
- React.js
- Vite
- Tailwind CSS
- Axios
- React Router

### Backend
- Spring Boot
- Java 21
- Spring Security (JWT)
- Spring Data JPA

### Database & Cache
- PostgreSQL
- Redis

## 📂 Project Structure

```
URL-Shortener/
├── backend/      # Spring Boot API
├── frontend/     # React + Vite Application
├── docker-compose.yml
└── README.md
```

## ⚙️ Getting Started

### Clone the repository

```bash
git clone https://github.com/Himani1207/URL-Shortener.git
cd URL-Shortener
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

## 🌐 API Endpoints

| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | User login |
| GET | `/api/auth/me` | Get current user |
| POST | `/api/urls` | Create short URL |
| GET | `/api/urls` | Get all URLs |
| PUT | `/api/urls/{id}` | Update URL |
| DELETE | `/api/urls/{id}` | Delete URL |
| GET | `/{shortCode}` | Redirect to original URL |
| GET | `/api/urls/{shortCode}/analytics` | URL analytics |
| GET | `/api/urls/{shortCode}/qr` | Generate QR Code |

## 📊 Key Features

- Secure JWT Authentication
- URL shortening with custom aliases
- QR code generation
- Click tracking and analytics
- Dashboard with URL statistics
- Redis caching for improved performance
- Responsive design for desktop and mobile

## 🔮 Future Enhancements

- Password-protected URLs
- URL expiration scheduling
- Custom domains
- User profile management
- Advanced analytics dashboard
- Email notifications

