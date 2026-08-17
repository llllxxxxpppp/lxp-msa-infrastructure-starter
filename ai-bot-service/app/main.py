from fastapi import FastAPI

app = FastAPI(title="LXP AI Bot Service")


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}