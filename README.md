# Enterprise Product Sales Management System

A desktop Product Sales Management application built with **Java 21, JavaFX, MySQL/JDBC, Maven**, following an MVC-style architecture (`model` / `dao` / `service` / `controller` + FXML views).

---

## What's implemented

- **First-launch Setup Wizard** — store info, Owner account, MySQL connection test, Gmail SMTP config. Never shown again once complete.
- **Public Viewer Dashboard** — opens by default, no login, shows the catalog with selling price + stock only (no wholesale price, no profit). Two buttons: Admin Login / Salesperson Login.
- **Three roles**: Owner (full control), Salesperson (sales entry only), Viewer (public, read-only, no account).
- **Mandatory Email OTP 2FA for the Owner** — 6-digit code, 5-minute expiry, single-use, rate-limited resend, sent via Gmail SMTP (Jakarta Mail).
- **Product management** (Owner): full CRUD, image upload, barcode/SKU field, automatic profit-per-item and available-quantity calculation, low-stock threshold.
- **Sales system**: cart-based sale entry, atomic stock deduction (DB transaction — a sale either fully succeeds or fully rolls back), invoice numbering, void/refund with stock restoration, printable PDF invoices.
- **Dashboard analytics** with live JavaFX charts: revenue/profit trend (line), sales mix (pie), top products (bar), low-stock watchlist, KPI cards.
- **Reports**: Product and Sales reports exportable as CSV, Excel (Apache POI), and PDF (OpenPDF).
- **Database backup/restore** via `mysqldump` / `mysql` CLI (must be on PATH).
- **User profiles**: avatar initials, editable details, password change.
- **Audit Log screen** (Owner-only): searchable/filterable view over every logged action (logins, sales, product/user changes, password resets) with user/action/date-range filters and CSV export.
- **Security**: BCrypt password hashing, role-based access control, idle-timeout auto-logout, activity/audit logging, login history.
- **Dark / Light theme** toggle, animated splash screen, fade-in scene transitions.
- **Product images are optional** — the Add/Edit Product form has a "Choose Image…" / "Remove" pair with a live thumbnail preview; leaving it blank is completely fine and the product saves with `image_path = NULL`.

## What's intentionally simplified

Given the scope of the original spec, a few items are implemented at a practical rather than maximal level — you can extend these further:

- **Barcode/QR**: the schema and forms support a SKU/barcode string field; a physical barcode-scanner integration or QR image renderer (ZXing is already a dependency, not yet wired into a screen) can be added easily.
- **Invoice/receipt printing**: currently exports a PDF you can print from any PDF viewer, rather than driving a receipt printer directly via `javax.print`.
- **Notifications**: low-stock/out-of-stock are computed and shown on the Dashboard; a background tray/toast notifier isn't wired up yet.
- **Reports date-range filtering**: the Reports screen exports full product/sales data; hook the `fromDatePicker`/`toDatePicker` values into `SaleDAO`'s date-ranged queries (`revenueByDay`, `topProducts`, etc. already accept a range) to filter exports by date.

## Important — please read before running

This project was written in a sandboxed environment **without access to Maven Central, a running MySQL server, JavaFX runtime, or Gmail SMTP** — so it could not be compiled or run end-to-end here. Every `.java` file was compiled with the local JDK wherever possible; the only errors that remain in isolation are "package does not exist" for JavaFX / MySQL / jBCrypt / Jakarta Mail / Apache POI / OpenPDF — i.e. the external libraries themselves, which Maven will fetch for you. All 15 FXML files were validated as well-formed XML. Please run a full `mvn clean javafx:run` locally and treat this as a solid, working first build to test and refine — not a guaranteed zero-bug final product.

## Prerequisites

1. **JDK 21+**
2. **Maven 3.9+**
3. **MySQL Server 8+** running locally (or reachable)
4. **mysqldump / mysql CLI** on PATH (for backup/restore — ships with MySQL)
5. A **Gmail account** with an **App Password** generated (Google Account → Security → 2-Step Verification → App Passwords) for the Owner 2FA emails

## Setup

```bash
# 1. Create the database schema
mysql -u root -p < src/main/resources/db/schema.sql

# 2. Build
mvn clean package

# 3. Run
mvn javafx:run
```

On first launch, the Setup Wizard will:
1. Collect store info (name, logo, address, contact, email)
2. Create the Owner account (name, username, Gmail, password)
3. Ask for your MySQL credentials (test the connection before continuing) and optionally your Gmail App Password for OTP emails

If you skip SMTP setup, the Owner can still log in — `OtpService` will report `SMTP_NOT_CONFIGURED` and the login screen will tell you to configure it under **Settings** first.

## Project layout

```
src/main/java/com/ledger/salesmanager/
  MainApp.java            entry point, splash screen, routing, idle-timeout watcher
  config/                 AppConfig (local settings file), DatabaseConnection (JDBC)
  model/                  POJOs: User, Product, Sale, SaleItem, StoreInfo, etc.
  dao/                    JDBC data access — one class per table/aggregate
  service/                business logic: AuthService, OtpService, ProductService,
                           SalesService, DashboardService, ReportService, BackupService
  controller/              JavaFX FXML controllers, one per screen/dialog
src/main/resources/
  fxml/                   one .fxml per screen
  css/                    theme-dark.css / theme-light.css
  db/schema.sql           full MySQL schema
```

## Configuration file

Local settings (DB credentials, SMTP credentials, theme, setup-completed flag) are stored at:

```
~/.enterprise-sales-manager/config.properties
```

**Do not commit this file** — it contains your DB password and Gmail App Password in plain text on disk (fine for a single local desktop deployment; encrypt-at-rest if you deploy more broadly).

## Building a distributable jar

```bash
mvn clean package
java -jar target/enterprise-sales-manager.jar
```
