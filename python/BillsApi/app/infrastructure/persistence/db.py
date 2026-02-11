import os

from sqlalchemy import create_engine
from sqlalchemy.orm import Session


_engine = None


def get_connection_url() -> str:
    host = os.getenv("POSTGRES_HOST", "localhost")
    port = os.getenv("POSTGRES_PORT", "5440")
    db = os.getenv("POSTGRES_DB", "api_lang_arena")
    user = os.getenv("POSTGRES_USER", "api_lang_user")
    password = os.getenv("POSTGRES_PASSWORD", "api_lang_password")
    return f"postgresql+psycopg://{user}:{password}@{host}:{port}/{db}"


def get_engine():
    global _engine
    if _engine is None:
        _engine = create_engine(get_connection_url(), pool_pre_ping=True)
    return _engine


def create_session() -> Session:
    return Session(get_engine())
