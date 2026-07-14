import json
from io import BytesIO

from minio import Minio
from minio.error import S3Error

from app.config import settings

BUCKET_AVATARS = "avatars"
BUCKET_CATALOG_COVERS = "catalog-covers"
BUCKET_ARTIST_IMAGES = "artist-images"
BUCKET_LIST_COVERS = "list-covers"

ALL_BUCKETS = (
    BUCKET_AVATARS,
    BUCKET_CATALOG_COVERS,
    BUCKET_ARTIST_IMAGES,
    BUCKET_LIST_COVERS,
)

ALLOWED_IMAGE_TYPES = frozenset({"image/jpeg", "image/png", "image/webp"})

EXTENSION_BY_CONTENT_TYPE = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
}

BUCKET_MAX_SIZE = {
    BUCKET_AVATARS: 5 * 1024 * 1024,
    BUCKET_CATALOG_COVERS: 10 * 1024 * 1024,
    BUCKET_ARTIST_IMAGES: 10 * 1024 * 1024,
    BUCKET_LIST_COVERS: 10 * 1024 * 1024,
}


class StorageError(Exception):
    pass


def get_minio_client() -> Minio:
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )


def _public_read_policy(bucket: str) -> str:
    return json.dumps(
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": [f"arn:aws:s3:::{bucket}/*"],
                }
            ],
        }
    )


def ensure_buckets() -> None:
    client = get_minio_client()
    for bucket in ALL_BUCKETS:
        if not client.bucket_exists(bucket):
            client.make_bucket(bucket)
        client.set_bucket_policy(bucket, _public_read_policy(bucket))


LEGACY_MEDIA_BASES = (
    "http://localhost:9000",
    "http://127.0.0.1:9000",
    "http://minio:9000",
)


def normalize_stored_media_url(url: str | None) -> str | None:
    if not url:
        return None

    desired_base = settings.minio_public_url.rstrip("/")
    for legacy_base in LEGACY_MEDIA_BASES:
        prefix = f"{legacy_base}/"
        if url.startswith(prefix):
            return f"{desired_base}/{url[len(prefix):]}"
    return url


def build_public_url(bucket: str, object_key: str) -> str:
    base = settings.minio_public_url.rstrip("/")
    return f"{base}/{bucket}/{object_key}"


def extension_for_content_type(content_type: str) -> str:
    extension = EXTENSION_BY_CONTENT_TYPE.get(content_type)
    if extension is None:
        raise StorageError("Tipo de imagen no permitido. Usa JPEG, PNG o WebP.")
    return extension


def validate_image_upload(bucket: str, content_type: str | None, size: int) -> str:
    if content_type not in ALLOWED_IMAGE_TYPES:
        raise StorageError("Tipo de imagen no permitido. Usa JPEG, PNG o WebP.")

    max_size = BUCKET_MAX_SIZE[bucket]
    if size <= 0:
        raise StorageError("El archivo está vacío")
    if size > max_size:
        max_mb = max_size // (1024 * 1024)
        raise StorageError(f"El archivo supera el límite de {max_mb} MB")

    return extension_for_content_type(content_type)


def upload_bytes(
    bucket: str,
    object_key: str,
    data: bytes,
    content_type: str,
) -> str:
    client = get_minio_client()
    try:
        client.put_object(
            bucket,
            object_key,
            BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
    except S3Error as exc:
        raise StorageError("No se pudo subir el archivo") from exc
    return build_public_url(bucket, object_key)


def delete_object(bucket: str, object_key: str) -> None:
    client = get_minio_client()
    try:
        client.remove_object(bucket, object_key)
    except S3Error:
        return


def delete_public_url(url: str | None) -> None:
    if not url:
        return

    base = settings.minio_public_url.rstrip("/")
    if not url.startswith(f"{base}/"):
        return

    remainder = url[len(base) + 1 :]
    parts = remainder.split("/", 1)
    if len(parts) != 2:
        return

    bucket, object_key = parts
    if bucket in ALL_BUCKETS:
        delete_object(bucket, object_key)
