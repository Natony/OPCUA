# ADR-002: Coroutine-based Architecture

## Status
Accepted

## Context
PLC communication involves:
- Long-running connections
- Real-time data updates
- Multiple concurrent operations
- UI responsiveness requirements

## Decision
Use Kotlin Coroutines with:
- Structured concurrency (SupervisorJob)
- Flow for reactive data streams
- Proper cancellation handling
- Thread confinement with Dispatchers

## Consequences
**Positive:**
- Better performance than callbacks/RxJava
- Native Kotlin support
- Easier to understand and maintain
- Built-in cancellation

**Negative:**
- Team needs coroutine expertise
- Careful scope management required