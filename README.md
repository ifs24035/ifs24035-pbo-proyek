🐾 Nuel’s Pawtopia – Sistem Manajemen Penitipan Hewan
Overview

Nuel’s Pawtopia adalah aplikasi berbasis web yang dibangun menggunakan Java Spring Boot untuk membantu pengelolaan operasional penitipan hewan. Sistem ini dirancang dengan antarmuka modern, user-friendly, serta dilengkapi fitur otomatisasi perhitungan biaya dan manajemen data hewan yang efisien.

Proyek ini dikembangkan sebagai pemenuhan tugas Pemrograman Berorientasi Objek (PBO) dengan fokus pada:

Object-Oriented Programming

MVC Architecture

Interactive User Interface

✨ Fitur Utama
🔐 1. Autentikasi & Keamanan

Login & Register dengan BCrypt Encryption

Spring Security untuk pembatasan hak akses

Fitur Remember Me dan Toggle Password untuk kenyamanan pengguna

🐕 2. Manajemen Data Hewan (CRUD)

Form pencatatan detail hewan (jenis, kriteria, jumlah, deskripsi, pemilik)

Kategori dinamis berdasarkan jenis hewan

Input “Jenis Lainnya”

Upload foto hewan sebagai identitas visual

💰 3. Kalkulasi Biaya Otomatis

Tarif otomatis berdasarkan Jenis & Kriteria

Perhitungan real-time:
(Harga per ekor per jam) × (Jumlah) × (Durasi)

Menampilkan rumus perhitungan biaya secara transparan

⏱️ 4. Manajemen Waktu & Kode Unik

Billing mulai dari waktu check-in

Pembulatan otomatis (>30 menit → dibulatkan jadi 1 jam)

Kode kandang unik (misal: ANJ-K-001)

Status warna: Merah = Belum diambil, Hijau = Sudah diambil

📊 5. Dashboard & Statistik

Donut chart (Chart.js) untuk visualisasi data hewan

Desain empty state jika data belum tersedia

🛠️ Teknologi yang Digunakan

Java (JDK 17+)

Spring Boot 3.x

Thymeleaf

PostgreSQL

Bootstrap 5

Chart.js

FontAwesome

Google Fonts (Poppins & Fredoka)

SweetAlert2

Maven

💵 Daftar Tarif Layanan
Anjing
Kriteria	Tarif / Jam	Prefix
Kecil	Rp 2.700	ANJ-K
Sedang	Rp 4.200	ANJ-S
Besar	Rp 7.700	ANJ-B
Kucing
Kriteria	Tarif / Jam	Prefix
Standar	Rp 1.700	KUC-ST
Premium	Rp 3.800	KUC-PR
VIP	Rp 9.400	KUC-VP
Hewan Lainnya
Hewan	Tarif / Jam	Prefix
Burung	Rp 700	BRG
Kelinci	Rp 1.000	KLN
Hamster	Rp 400	HMR
Reptil	Rp 1.100	RPT

🚀 Cara Menjalankan Proyek
1. Clone Repository
git clone https://github.com/username-anda/nuels-pawtopia.git
cd nuels-pawtopia

2. Konfigurasi Database

Buat database baru (misal: petshop_db)

Atur username & password PostgreSQL di:

src/main/resources/application.properties

3. Jalankan Aplikasi
mvn spring-boot:run

4. Akses Web

URL: http://localhost:8080

🧪 Test Coverage & Build
Instal ulang kebutuhan paket
mvn clean install

Build + Open Jacoco Report
Windows
mvn clean test; start target\site\jacoco\index.html

MacOS
mvn clean test && open target\site\jacoco\index.html

Linux
mvn clean test && xdg-open target\site\jacoco\index.html

Menjalankan Test Coverage
./mvnw test jacoco:report
./mvnw clean test jacoco:check

👨‍💻 Pengembang

Nama: [Nama Lengkap Kamu]
NIM: [NIM Kamu]
Mata Kuliah: Pemrograman Berorientasi Objek

📘 Purpose

Proyek ini dibuat untuk tujuan Pendidikan.