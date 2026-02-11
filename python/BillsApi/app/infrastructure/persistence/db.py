import os

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker


_engine = None
_session_factory: sessionmaker[Session] | None = None


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
        pool_size = int(os.getenv("PY_DB_POOL_SIZE", "10"))
        max_overflow = int(os.getenv("PY_DB_MAX_OVERFLOW", "5"))
        pool_timeout = int(os.getenv("PY_DB_POOL_TIMEOUT_SEC", "30"))
        pool_recycle = int(os.getenv("PY_DB_POOL_RECYCLE_SEC", "1800"))
        _engine = create_engine(
            get_connection_url(),
            pool_pre_ping=True,
            pool_size=pool_size,
            max_overflow=max_overflow,
            pool_timeout=pool_timeout,
            pool_recycle=pool_recycle,
        )
    return _engine


def create_session() -> Session:
    global _session_factory
    if _session_factory is None:
        _session_factory = sessionmaker(
            bind=get_engine(),
            autoflush=False,
            autocommit=False,
            expire_on_commit=False,
        )
    return _session_factory()
