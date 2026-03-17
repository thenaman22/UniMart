from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfgen import canvas


PAGE_WIDTH, PAGE_HEIGHT = letter
MARGIN = 34
GUTTER = 16
COLUMN_WIDTH = (PAGE_WIDTH - (MARGIN * 2) - GUTTER) / 2

BG = colors.HexColor("#f6f8fb")
INK = colors.HexColor("#142033")
MUTED = colors.HexColor("#5d6a7e")
ACCENT = colors.HexColor("#1f6aa5")
ACCENT_SOFT = colors.HexColor("#d9ebf8")
CARD = colors.HexColor("#ffffff")
BORDER = colors.HexColor("#d4dce7")

TITLE_FONT = "Helvetica-Bold"
BODY_FONT = "Helvetica"


def wrap_text(text, width, font_name, font_size):
    words = text.split()
    lines = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if pdfmetrics.stringWidth(candidate, font_name, font_size) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines or [""]


def draw_heading(c, x, y, text):
    c.setFont(TITLE_FONT, 11)
    c.setFillColor(ACCENT)
    c.drawString(x, y, text.upper())
    c.setStrokeColor(ACCENT_SOFT)
    c.setLineWidth(1)
    c.line(x, y - 4, x + 72, y - 4)
    return y - 18


def draw_paragraph(c, x, y, width, text, font_size=9.4, leading=11.4, color=INK):
    c.setFont(BODY_FONT, font_size)
    c.setFillColor(color)
    for line in wrap_text(text, width, BODY_FONT, font_size):
        c.drawString(x, y, line)
        y -= leading
    return y


def draw_bullets(c, x, y, width, items, font_size=9.1, leading=11.0, bullet_indent=10):
    c.setFont(BODY_FONT, font_size)
    c.setFillColor(INK)
    for item in items:
        lines = wrap_text(item, width - bullet_indent, BODY_FONT, font_size)
        c.drawString(x, y, "-")
        c.drawString(x + bullet_indent, y, lines[0])
        y -= leading
        for line in lines[1:]:
            c.drawString(x + bullet_indent, y, line)
            y -= leading
        y -= 2
    return y


def draw_numbered_steps(c, x, y, width, steps, font_size=9.0, leading=10.8, label_gap=13):
    c.setFont(BODY_FONT, font_size)
    c.setFillColor(INK)
    for index, step in enumerate(steps, start=1):
        label = f"{index}."
        lines = wrap_text(step, width - label_gap, BODY_FONT, font_size)
        c.drawString(x, y, label)
        c.drawString(x + label_gap, y, lines[0])
        y -= leading
        for line in lines[1:]:
            c.drawString(x + label_gap, y, line)
            y -= leading
        y -= 2
    return y


def draw_card(c, x, y, width, height):
    c.setFillColor(CARD)
    c.setStrokeColor(BORDER)
    c.setLineWidth(1)
    c.roundRect(x, y - height, width, height, 14, stroke=1, fill=1)


def build_pdf(output_path: Path):
    output_path.parent.mkdir(parents=True, exist_ok=True)

    c = canvas.Canvas(str(output_path), pagesize=letter)
    c.setTitle("UniMart App Summary")

    c.setFillColor(BG)
    c.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, fill=1, stroke=0)

    header_x = MARGIN
    header_y = PAGE_HEIGHT - MARGIN
    header_h = 86
    header_w = PAGE_WIDTH - (MARGIN * 2)

    c.setFillColor(ACCENT)
    c.roundRect(header_x, header_y - header_h, header_w, header_h, 18, fill=1, stroke=0)
    c.setFillColor(colors.white)
    c.setFont(TITLE_FONT, 24)
    c.drawString(header_x + 20, header_y - 30, "UniMart")
    c.setFont(BODY_FONT, 11.2)
    c.drawString(header_x + 20, header_y - 50, "One-page repo summary")
    c.setFillColor(colors.HexColor("#d7ebff"))
    c.setFont(BODY_FONT, 9.2)
    c.drawRightString(header_x + header_w - 20, header_y - 30, "Spring Boot + React marketplace")
    c.drawRightString(header_x + header_w - 20, header_y - 46, "Evidence taken only from repo files")

    cards_top = header_y - header_h - 16
    left_x = MARGIN
    right_x = MARGIN + COLUMN_WIDTH + GUTTER
    card_height = 610

    draw_card(c, left_x, cards_top, COLUMN_WIDTH, card_height)
    draw_card(c, right_x, cards_top, COLUMN_WIDTH, card_height)

    left_y = cards_top - 20
    left_y = draw_heading(c, left_x + 16, left_y, "What it is")
    left_y = draw_paragraph(
        c,
        left_x + 16,
        left_y,
        COLUMN_WIDTH - 32,
        "UniMart is a private, community-first marketplace for schools or organizations. Members sign in with email codes, browse only the communities they can access, and buy or sell listings inside those trusted groups.",
    )
    left_y -= 10

    left_y = draw_heading(c, left_x + 16, left_y, "Who it's for")
    left_y = draw_paragraph(
        c,
        left_x + 16,
        left_y,
        COLUMN_WIDTH - 32,
        "Primary persona: students or organization members who want a safer way to trade with people in their own campus, club, or community.",
    )
    left_y -= 10

    left_y = draw_heading(c, left_x + 16, left_y, "What it does")
    left_y = draw_bullets(
        c,
        left_x + 16,
        left_y,
        COLUMN_WIDTH - 32,
        [
            "Email code sign-up and sign-in with token-based sessions.",
            "Private community discovery plus join flows by email domain, invite link, or membership request.",
            "Community-scoped listing search, browsing, and detail views.",
            "Create, edit, update status, delete, and report listings.",
            "Image and video upload support for listings and profile pictures.",
            "Listing-based messaging with buyer and seller inboxes plus unread counts.",
            "Moderator tools for requests, members, role changes, and unresolved reports.",
        ],
    )

    right_y = cards_top - 20
    right_y = draw_heading(c, right_x + 16, right_y, "How it works")
    right_y = draw_bullets(
        c,
        right_x + 16,
        right_y,
        COLUMN_WIDTH - 32,
        [
            "Frontend: a React 18 + Vite single-page app in frontend/src with routes for auth, dashboard, communities, sell, messages, moderation, and profiles.",
            "Client API flow: frontend/src/api.js calls http://localhost:8080 and sends X-Auth-Token from localStorage on authenticated requests.",
            "Backend: a Spring Boot 3 REST API exposes auth, communities, listings, messaging, profile, uploads, media, and moderation endpoints.",
            "Auth and data: SessionAuthenticationFilter resolves tokens to AuthContext, services apply marketplace rules, and Spring Data JPA persists entities to PostgreSQL.",
            "Media: uploads are written to uploads/ and served from /media/{storageKey}; demo seed data also copies bundled images from ImagesData/.",
        ],
        font_size=8.9,
        leading=10.7,
    )
    right_y -= 4

    right_y = draw_heading(c, right_x + 16, right_y, "How to run")
    right_y = draw_numbered_steps(
        c,
        right_x + 16,
        right_y,
        COLUMN_WIDTH - 32,
        [
            "Install prerequisites: Java 17, PostgreSQL, Node.js/npm. Exact Node/PostgreSQL versions: Not found in repo.",
            "Create a PostgreSQL database named unimart.",
            "Create a root .env.properties file with DB_URL, DB_USERNAME, DB_PASSWORD, spring.profiles.active=dev, and optional app.seed.refresh-demo-data=true. Template file: Not found in repo.",
            "Start the backend with .\\gradlew.bat bootRun.",
            "From frontend/, run npm install and npm run dev, then open http://localhost:5173.",
            "For demo access, use emails from demo-users.md and the OTP code returned by the auth API in local development.",
        ],
    )
    right_y -= 2

    right_y = draw_heading(c, right_x + 16, right_y, "Repo gaps")
    right_y = draw_bullets(
        c,
        right_x + 16,
        right_y,
        COLUMN_WIDTH - 32,
        [
            ".env.properties.example is referenced in README.md but Not found in repo.",
            "A real email delivery integration is Not found in repo; AuthService returns the login code in the API response for local use.",
        ],
        font_size=8.8,
        leading=10.6,
    )

    footer_y = 26
    c.setFillColor(MUTED)
    c.setFont(BODY_FONT, 7.4)
    footer = (
        "Evidence used: README.md, build.gradle, frontend/package.json, frontend/src/App.jsx, frontend/src/api.js, "
        "application.yml, AuthService.java, SecurityConfig.java, controller classes, SeedDataService.java."
    )
    for line in wrap_text(footer, PAGE_WIDTH - (MARGIN * 2), BODY_FONT, 7.4):
        c.drawString(MARGIN, footer_y, line)
        footer_y += 9

    c.showPage()
    c.save()


if __name__ == "__main__":
    repo_root = Path(__file__).resolve().parents[2]
    output_pdf = repo_root / "output" / "pdf" / "unimart-app-summary.pdf"
    build_pdf(output_pdf)
    print(output_pdf)
