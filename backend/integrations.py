"""Provider interfaces and mock fallbacks for external data sources."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class ProviderStatus:
    key: str
    name: str
    category: str
    provider: str = "mock"
    status: str = "Ready"


PROVIDERS = (
    ProviderStatus("imd_weather", "IMD weather data", "Weather"),
    ProviderStatus("satellite", "Satellite imagery and analysis", "Terrain"),
    ProviderStatus("iot_rain_gauge", "IoT rain gauges", "Sensors"),
    ProviderStatus("soil_moisture", "Soil-moisture sensors", "Sensors"),
    ProviderStatus("inclinometer", "Inclinometers", "Sensors"),
    ProviderStatus("historical_landslides", "Historical landslide datasets", "History"),
)


def provider_statuses() -> list[dict[str, Any]]:
    """Return integration readiness without contacting external services."""
    return [status.__dict__.copy() for status in PROVIDERS]


def mock_reading(provider_key: str, zone_id: str) -> dict[str, Any]:
    """Return a predictable placeholder reading until a real adapter is configured."""
    if provider_key not in {status.key for status in PROVIDERS}:
        raise KeyError(provider_key)
    return {
        "provider": provider_key,
        "zone_id": zone_id,
        "source": "mock",
        "status": "Simulated",
        "data": {},
    }
