# REDICAP

Registro, Digitalización, Captura y Publicación de Actas — sistema para el IMPEPAC Morelos.

Este repositorio unifica los tres componentes del sistema, cada uno conservando su historial completo de commits (fusionado con `git subtree`):

- [`backend/`](backend) — API REST en Java 21 / Spring Boot 3.3 (auth JWT + RBAC, digitalización, verificación por mesa de deliberación, publicación de cortes, Redis, PostgreSQL). Las migraciones y el esquema de la base de datos viven en `backend/src/main/resources/db/migration` (Flyway) — no hay un repositorio de BD separado.
- [`frontend/`](frontend) — Aplicación web en React 19 / Vite / TypeScript / Tailwind CSS v4, con pantallas para los 5 roles (Capturista, Digitalizador, Verificador, Administrador, Consultor Público).
- [`movil/`](movil) — App Android en Kotlin / Jetpack Compose para el rol Digitalizador, con captura de fotos, cola de subida offline y reintento automático vía WorkManager.

Cada subcarpeta conserva su propio `README`/instrucciones de build si aplican. Ver el historial de commits de cada una con `git log -- backend/`, `git log -- frontend/`, `git log -- movil/`.
