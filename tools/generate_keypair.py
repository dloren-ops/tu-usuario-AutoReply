#!/usr/bin/env python3
"""
Genera el par de claves ECDSA (P-256) para firmar codigos de activacion.
Se corre UNA sola vez (o cuando quieras rotar la clave).

Requiere: pip install cryptography

Uso:
    python3 tools/generate_keypair.py

Salida:
    - tools/owner_private_key.pem  -> PRIVADA. Nunca la subas a git ni la
      compartas. La usa `generate_license.py` y hay que pegarla (ver mas
      abajo) en la variante "owner" de la app (app/src/owner/.../LicensePrivateKey.kt).
    - Por pantalla: el hex de la clave publica, para pegar en
      app/src/main/.../license/LicensePublicKey.kt (esa SI es publica, no
      pasa nada si la ve cualquiera).
"""

import pathlib

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

OUT_DIR = pathlib.Path(__file__).parent
PRIVATE_KEY_PATH = OUT_DIR / "owner_private_key.pem"


def main() -> None:
    if PRIVATE_KEY_PATH.exists():
        raise SystemExit(
            f"{PRIVATE_KEY_PATH} ya existe. Si de verdad queres reemplazar la clave "
            "(vas a invalidar todos los codigos ya emitidos), borrala a mano primero."
        )

    private_key = ec.generate_private_key(ec.SECP256R1())
    public_key = private_key.public_key()

    private_pem = private_key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    PRIVATE_KEY_PATH.write_bytes(private_pem)
    PRIVATE_KEY_PATH.chmod(0o600)

    private_pkcs8_der = private_key.private_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    public_point_uncompressed = public_key.public_bytes(
        encoding=serialization.Encoding.X962,
        format=serialization.PublicFormat.UncompressedPoint,
    )

    print(f"Clave privada guardada en: {PRIVATE_KEY_PATH} (permisos 600, NO subir a git)")
    print()
    print("Pega esto en app/src/main/java/com/autoreply/bot/license/LicensePublicKey.kt (HEX, publica):")
    print(public_point_uncompressed.hex())
    print()
    print("Pega esto en app/src/owner/java/com/autoreply/bot/license/LicensePrivateKey.kt (HEX, PRIVADA):")
    print(private_pkcs8_der.hex())


if __name__ == "__main__":
    main()
