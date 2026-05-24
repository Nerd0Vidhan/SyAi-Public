# AI Image Generation Flow

This document explains how image generation moves through SyAi Notes, starting from the Notes Editor UI and ending with the generated image being inserted back into the app.

## Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Editor as "Notes Editor Screen"
    participant Sheet as "AI Tool Bottom Sheet"
    participant VM as "NoteEditorViewModel"
    participant Repo as "LocalImageGeneratorRepository"
    participant Spring as "Spring Boot AI Server"
    participant ImageService as "ImageGenerationService"
    participant Python as "Python Image Runner"
    participant Model as "DreamShaper / Stable Diffusion"
    participant Storage as "Generated Image Storage"
    participant AppStorage as "Android App Storage"
    participant Canvas as "Note Canvas"

    User->>Editor: Opens AI tool
    Editor->>Sheet: Shows prompt input and image-generation option
    User->>Sheet: Enters image prompt
    Sheet->>VM: onGenerateImage(prompt)

    VM->>VM: Collect current note/page context
    VM->>VM: Mark page as generating
    VM->>Repo: submit(LocalImageGenerationRequest)

    Repo->>Spring: POST /api/v1/images/generate
    Spring->>ImageService: create generation job
    ImageService->>ImageService: Store job as QUEUED/RUNNING
    ImageService->>Python: Start stable_diffusion_runner.py
    Python->>Model: Load/run DreamShaper with prompt
    Model-->>Python: Generated image bitmap
    Python-->>ImageService: Image file path/result

    ImageService->>Storage: Save generated PNG/JPEG
    ImageService->>ImageService: Mark job COMPLETED
    Spring-->>Repo: Accepted response with jobId/statusUrl

    loop Poll While Editor Is Open
        VM->>Repo: status(statusUrl)
        Repo->>Spring: GET job status
        Spring-->>Repo: QUEUED/RUNNING/COMPLETED
        Repo-->>VM: Job status
    end

    VM->>Repo: downloadToAppStorage(jobId, imageUrl)
    Repo->>Spring: GET /api/v1/images/files/{fileName}
    Spring->>Storage: Read generated image
    Spring-->>Repo: Image bytes
    Repo->>AppStorage: Save image in app files
    Repo-->>VM: Local image file

    VM->>VM: Create IMAGE NoteObject
    VM->>Canvas: Insert image on current page
    VM->>VM: Persist note content
    VM->>Editor: Clear generating state
    Canvas-->>User: Generated image appears on page
```

## Component Diagram

```mermaid
flowchart LR
    subgraph Android["Android App"]
        Editor["Notes Editor Screen"]
        Sheet["AI Tool Bottom Sheet"]
        VM["NoteEditorViewModel"]
        Repo["LocalImageGeneratorRepository"]
        DB["Local Note Storage"]
        Canvas["Point-Based Note Canvas"]
    end

    subgraph Backend["Laptop / Local Server"]
        Spring["Spring Boot Server :8088"]
        Controller["ImageGenerationController"]
        Service["ImageGenerationService"]
        Jobs["In-Memory Job Registry"]
        Files["Generated Image Files"]
    end

    subgraph PythonAI["Python AI Runtime"]
        Runner["stable_diffusion_runner.py"]
        FastAPI["Optional FastAPI Runtime :8000"]
        Model["DreamShaper / Stable Diffusion"]
    end

    Editor --> Sheet
    Sheet --> VM
    VM --> Repo
    VM --> DB
    Repo --> Spring
    Spring --> Controller
    Controller --> Service
    Service --> Jobs
    Service --> Runner
    Runner --> Model
    Model --> Runner
    Runner --> Files
    Service --> Files
    Repo --> Files
    Repo --> VM
    VM --> Canvas
```

## High-Level Workflow

1. The user opens the AI tool from the Notes Editor.
2. The AI Tool Bottom Sheet collects the image prompt.
3. `NoteEditorViewModel` gathers the current note/page context and starts the image-generation request asynchronously.
4. `LocalImageGeneratorRepository` sends the request to the Spring Boot server.
5. Spring Boot creates a generation job and delegates image generation to the Python runner.
6. The Python runner loads/runs DreamShaper or Stable Diffusion and saves the generated image.
7. Spring Boot marks the job as completed and exposes the generated image through a file endpoint.
8. The Android app polls the job status while the editor is open.
9. Once completed, Android downloads the generated image into app storage.
10. `NoteEditorViewModel` creates an `IMAGE` object and inserts it into the current note page.
11. The note content is persisted and the generated image appears on the canvas.

## Important Design Points

- The Android UI stays responsive because generation is asynchronous.
- The Spring Boot server acts as a LAN bridge between the phone and the AI runtime.
- The Python runner owns the heavy model execution so Android does not need to run Stable Diffusion locally.
- Generated images are stored as files, then referenced by the note model.
- The editor inserts the final result as a normal image object, so it can be moved, resized, selected, exported, and persisted like other note content.

