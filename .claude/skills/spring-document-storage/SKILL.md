---
name: spring-document-storage
description: Store binary files such as images, videos, PDFs, and generic uploads in Java Spring entities with database-specific JPA mappings. Use when Codex needs a `Document` entity or equivalent binary storage model and must choose the correct mapping for PostgreSQL versus SQL Server or other databases.
---

# Spring Document Storage

Use this skill when storing uploaded binary content directly in the database.

## Rules

- Do not use Lombok.
- Use the entity name `Document`.
- Use `@Table(name = "tbDocument")`.
- Keep Java field names as `id`, `size`, `name`, `contentType`, and `document`.
- Preserve the existing foreign-key naming convention elsewhere; this skill only covers the document entity itself.

## PostgreSQL pattern

Use this mapping for PostgreSQL:

```java
@Entity
@Table(name = "tbDocument")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long size;
    private String name;
    private String contentType;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "document", columnDefinition = "bytea")
    private byte[] document;

    public Document() {}

    public Document(MultipartFile file) throws IOException {
        this.name = file.getOriginalFilename();
        this.contentType = file.getContentType();
        this.document = file.getBytes();
        this.size = file.getSize();
    }

    // getters/setters
}
```

## SQL Server and other databases

Use this mapping for SQL Server and databases other than PostgreSQL:

```java
@Entity
@Table(name = "tbDocument")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long size;
    private String name;
    private String contentType;

    @Lob
    private byte[] document;

    public Document() {}

    public Document(MultipartFile file) throws IOException {
        this.name = file.getOriginalFilename();
        this.contentType = file.getContentType();
        this.document = file.getBytes();
        this.size = file.getSize();
    }

    // getters/setters
}
```

## Constructor and accessor guidance

- Keep the empty constructor.
- When building from `MultipartFile`, copy original filename, content type, byte array, and size.
- Generate explicit getters and setters. Do not replace them with Lombok.
