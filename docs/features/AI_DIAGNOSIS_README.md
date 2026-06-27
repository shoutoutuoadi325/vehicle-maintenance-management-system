# AI Diagnosis Configuration

The backend reads AI diagnosis settings from environment variables. Do not commit real API keys.

## Environment Variables

```bash
export AI_DIAGNOSIS_API_KEY=your-api-key-here
export AI_DIAGNOSIS_API_BASE_URL=https://api.apiyi.com/v1
export AI_DIAGNOSIS_API_MODEL=deepseek-chat
```

Windows PowerShell:

```powershell
$env:AI_DIAGNOSIS_API_KEY = "your-api-key-here"
$env:AI_DIAGNOSIS_API_BASE_URL = "https://api.apiyi.com/v1"
$env:AI_DIAGNOSIS_API_MODEL = "deepseek-chat"
```

## application.properties

The committed `application.properties` must only contain safe defaults:

```properties
ai.diagnosis.api.key=${AI_DIAGNOSIS_API_KEY:}
ai.diagnosis.api.base-url=${AI_DIAGNOSIS_API_BASE_URL:https://api.apiyi.com/v1}
ai.diagnosis.api.model=${AI_DIAGNOSIS_API_MODEL:deepseek-chat}
```

If `AI_DIAGNOSIS_API_KEY` is empty, the service should fall back to local diagnosis behavior instead of relying on a committed key.

## Local Overrides

Use an ignored local file or shell environment variables for personal credentials:

```properties
# backend/src/main/resources/application-local.properties
ai.diagnosis.api.key=your-real-api-key
spring.datasource.password=your-local-db-password
security.jwt.secret=your-local-jwt-secret-at-least-32-chars
```

Local override files are ignored by `.gitignore`.
