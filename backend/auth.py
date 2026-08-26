"""Firebase token verification and role-based access dependencies."""

from __future__ import annotations

from functools import wraps
from typing import Any, Callable

import firebase_admin
from firebase_admin import auth
from flask import jsonify, request


def current_user() -> dict[str, Any] | None:
    try:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
    except Exception:
        return None
    header = request.headers.get("Authorization", "")
    if not header.startswith("Bearer "):
        return None
    try:
        return auth.verify_id_token(header.removeprefix("Bearer "))
    except Exception:
        return None


def require_roles(*allowed_roles: str) -> Callable[..., Any]:
    def decorator(function: Callable[..., Any]) -> Callable[..., Any]:
        @wraps(function)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            user = current_user()
            if user is None:
                return jsonify({"error": "Authentication required"}), 401
            if user.get("role", "Citizen") not in allowed_roles:
                return jsonify({"error": "Permission denied"}), 403
            return function(*args, **kwargs)

        return wrapper

    return decorator
