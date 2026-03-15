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
2. Set the required database environment variables:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/unimart"
$env:DB_USERNAME="your_postgres_username"
$env:DB_PASSWORD="your_postgres_password"
```

3. Run:

```bash
.\gradlew.bat bootRun
```

This project includes a repo-local Gradle bootstrap script, so you do not need a global Gradle install. The first run downloads Gradle `8.9`.

### Frontend

1. In [`frontend/package.json`](/C:/Users/priya/OneDrive/Desktop/UniMart/frontend/package.json), install dependencies:

```bash
npm install
npm run dev
```

## Demo flow

- Seeded admin user: `admin@school.edu`
- Seeded community: `Campus Market`
- Request a login code on the auth screen, then verify with the displayed dev code.
- Join the seeded community by organization email or use moderation/invite flows from the UI.

## Notes

- Uploads are stubbed with generated storage keys and placeholder URLs so the API shape is ready for real object storage integration.
- OTP delivery currently returns the code in the API response for local development only.
