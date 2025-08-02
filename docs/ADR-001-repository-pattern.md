# ADR-001: Use Repository Pattern for PLC Communication

## Status
Accepted

## Context
We need an abstraction layer between OPC UA implementation and business logic to:
- Support different PLC communication protocols in the future
- Enable easier testing with fake implementations
- Separate concerns between data access and business logic

## Decision
Use Repository pattern with:
- `S7Repository` interface defining common operations
- `OptimizedOPCUARepositoryImpl` for OPC UA implementation
- Potential for `ModbusRepositoryImpl` or `S7CommRepositoryImpl` in future

## Consequences
**Positive:**
- Easy to mock for testing
- Can switch communication protocols without changing business logic
- Clear separation of concerns

**Negative:**
- Additional abstraction layer
- Need to maintain interface consistency