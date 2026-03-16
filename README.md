# UniMart

UniMart is a community-first marketplace app where only approved members of a school or organization can browse and create listings for that community.

## Stack

- Spring Boot REST API
- PostgreSQL via Spring Data JPA
- React + Vite frontend
- Email OTP-style sign-in
- Community-scoped access control for listings and search

## Local run

### Backend

1. Create a PostgreSQL database named `unimart`.
2. Create a local `.env.properties` file in the project root using [.env.properties.example](/C:/Users/priya/OneDrive/Desktop/UniMart/.env.properties.example) as a template.
3. Put your local config in it:

```properties
DB_URL=jdbc:postgresql://localhost:5432/unimart
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
spring.profiles.active=dev
app.seed.refresh-demo-data=true
```

4. Run:

```bash
.\gradlew.bat bootRun
```

This project includes a repo-local Gradle bootstrap script, so you do not need a global Gradle install. The first run downloads Gradle `8.9`.

With `app.seed.refresh-demo-data=true`, startup clears and reseeds the demo communities, users, listings, invites, and reports.

### Frontend

1. In [frontend/package.json](/C:/Users/priya/OneDrive/Desktop/UniMart/frontend/package.json), install dependencies:

```bash
npm install
npm run dev
```

## Demo flow

- Start backend with demo refresh enabled.
- Start frontend.
- Sign in with any seeded email from [demo-users.md](/C:/Users/priya/OneDrive/Desktop/UniMart/demo-users.md).
- Request a code, then verify with the displayed dev code.

## Notes

- Uploads are stubbed with generated storage keys and placeholder URLs so the API shape is ready for real object storage integration.
- OTP delivery currently returns the code in the API response for local development only.
