# Architecture Overview

## Introduction
This document describes the architecture of S7 OPC UA Android application.

## Architecture Diagram
```mermaid
graph TD
    A[UI Layer - Compose] --> B[Presentation Layer - ViewModels]
    B --> C[Domain Layer - Use Cases]
    C --> D[Data Layer - Repositories]
    D --> E[Framework Layer - OPC UA/Database]