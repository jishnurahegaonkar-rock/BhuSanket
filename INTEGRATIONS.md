# External Integrations

The application currently uses mock providers so the risk workflow remains usable when external services are unavailable.

The provider registry is available at `GET /api/integrations` and currently reports these planned adapters:

| Key | Future source | Current provider |
|---|---|---|
| `imd_weather` | IMD weather data | `mock` |
| `satellite` | Satellite imagery or analysis | `mock` |
| `iot_rain_gauge` | IoT rain gauges | `mock` |
| `soil_moisture` | Soil-moisture sensors | `mock` |
| `inclinometer` | Inclinometers | `mock` |
| `historical_landslides` | Historical landslide datasets | `mock` |

Each provider should eventually implement the same adapter contract: accept a zone identifier, return normalized data, include a source timestamp, and report its health/status. The risk engine should consume normalized data rather than provider-specific response formats.

## Offline reports

The browser registers `frontend/sw.js` and stores unsent reports in IndexedDB database `bhusanket-offline`, object store `pending-reports`. When connectivity returns, queued reports are submitted in order to `POST /api/reports` and removed only after a successful response.

The dashboard status feed shows `Online · sync ready` or `Offline · reports will queue`. This is prototype offline support; real background sync and conflict resolution should be added before operational deployment.
