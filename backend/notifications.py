"""Simulated notification records for future delivery integrations."""

from __future__ import annotations

from typing import Any, Mapping


CHANNELS = ("app", "sms", "email", "ivr")
SUPPORTED_LANGUAGES = ("en", "hi", "as", "mni", "kha")
ALERT_TEMPLATES = {
    "en": "{level} risk at {location}. {action}",
    "hi": "{location} में {level} जोखिम। {action}",
    "as": "{location}ত {level} বিপদ। {action}",
    "mni": "{location}দা {level} risk. {action}",
    "kha": "{location} ha ka jingma {level}. {action}",
}


def build_notification(alert: Mapping[str, Any], channel: str, language: str = "en") -> dict[str, Any]:
    """Build a queued prototype notification without contacting a provider."""
    if channel not in CHANNELS:
        raise ValueError(f"unsupported notification channel: {channel}")
    if language not in SUPPORTED_LANGUAGES:
        raise ValueError(f"unsupported notification language: {language}")
    return {
        "alert_id": alert.get("id"),
        "zone_id": alert["zone_id"],
        "channel": channel,
        "language": language,
        "template_key": f"landslide_{str(alert['level']).lower()}_{language}",
        "message": ALERT_TEMPLATES[language].format(
            level=alert["level"],
            location=alert.get("location", alert["zone_id"]),
            action=alert["recommended_action"],
        ),
        "status": "Simulated",
        "provider": "prototype",
    }


def notifications_for_alert(alert: Mapping[str, Any]) -> list[dict[str, Any]]:
    """Create the channels planned for an actionable alert."""
    if alert["level"] not in {"High", "Critical"}:
        return []
    return [build_notification(alert, channel) for channel in ("app", "sms")]