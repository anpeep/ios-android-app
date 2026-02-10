# GPS SportMap — Advanced Outdoor Training & Navigation Platform

GPS SportMap is a full-stack mobile sports tracking and navigation application inspired by Strava and Nike Run, designed for real-world outdoor use in unknown terrain. The project focuses on high-reliability GPS tracking, background services, real-time map visualization, and backend synchronization — built with production-style architecture and performance considerations.

This project demonstrates end-to-end mobile engineering: location services, mapping systems, sensor integration, offline resilience, REST synchronization, database design, and background execution reliability.

## Project Overview

GPS SportMap enables athletes and outdoor explorers to:

- Track training sessions in real time

- Navigate unfamiliar terrain

- Mark checkpoints and waypoints

- Analyze pace and distance metrics

- Export GPX tracks for further analysis

- Sync sessions to a central backend API

- Operate reliably in background and lock-screen conditions

The application is engineered to remain stable during long multi-hour sessions, even with intermittent connectivity.

## Core Features
Real-Time GPS Tracking

- Continuous fused GPS tracking

- Live map position updates

- Trail drawing from session start

- Direction-up / North-up / user-selected map orientation modes

- GPS filtering to remove impossible coordinate jumps

Training Metrics Engine

Distance calculations:

- Total session distance

- Distance from last checkpoint

- Distance from last waypoint

- Direct-line vs travelled distance

- Time tracking with section breakdowns

- Automatic pace calculation (min/km)

- Gradient color track visualization based on speed ranges

Navigation Markers

- Checkpoints (CP) — permanent terrain markers saved to database

- Waypoints (WP) — temporary navigation markers (auto-replace previous)

- Marker-based segment statistics

Map & Sensor Integration

- Google Maps / OpenStreetMap support

- On-screen real compass (sensor based, not map indicator)

- Device rotation support with adaptive UI layouts

- Tablet & multi-screen support

## Background Execution & Lock Screen Control

Built with proper Android foreground services and broadcast listeners:

- Persistent foreground GPS tracking service

- Sticky custom notification UI

- Live metric updates inside notification

- Lock-screen session monitoring

- Safe start/stop confirmation handling

- Background-safe GPS listener architecture

## Local Persistence Layer

Structured local database for:

- Sessions

- GPS locations

- Checkpoints

- Full session history management:

- View past sessions on map

- Rename & delete sessions

- Zoomable track replay

- State restoration across rotation & process recreation

## Backend & Sync Integration

Connected to a RESTful Web API backend:

- JWT authentication

- Account registration & login

- Real-time session sync

- Configurable sync interval

- Offline-first design — deferred sync supported

- Backend-calculated metrics (distance, speed, duration)

API domains covered:

- Sessions

- Session types

- Locations

- Location types

## GPX Export

Full session export in GPX format

Includes:

- All GPS track points

- Checkpoints as GPX waypoints

- Waypoint markers

- Accurate timestamps

- Ready for external tools and map analysis

- Email attachment export supported

## Engineering Highlights

This project emphasizes real engineering challenges:

- Background location tracking reliability

- Sensor fusion usage

- Battery vs accuracy tuning

- GPS noise filtering algorithms

- Foreground service lifecycle management

- Map rendering performance

- Offline-first sync strategy

- Defensive state restoration

- Configurable pace visualization ranges

- Long-session stability testing (2–3h sessions)

## Tech Stack 

Mobile

- Kotlin

- Android SDK

- Google Maps SDK / OSM

- Fused Location Provider

- Foreground Services

- SensorManager (Compass)

- Room / SQLite

- Coroutines / Flow

Backend Integration

- REST API

- JWT Authentication

- JSON serialization

- Real-time sync endpoints

Data Formats

- GPX export

- JSON REST payloads

## Why This Project Matters

This is not a demo app — it is a field-tested GPS tracking platform designed around:

- Real outdoor usage scenarios

- Background execution constraints

- Sensor integration

- Data reliability

- Performance under long-running sessions

- Clean architecture & modular design

It demonstrates readiness to build production-grade mobile systems that interact with hardware sensors, maps, and remote APIs.
