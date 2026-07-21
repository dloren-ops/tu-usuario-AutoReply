#!/usr/bin/env python3
"""
Generador OFFLINE de codigos de activacion para AutoReply.

Uso tipico (el "ID de este telefono" te lo pasa quien alquila la app, se ve
en la pantalla Inicio de la app, seccion "Licencia"):

    python3 tools/generate_license.py --device-id 9774d56d682e549c --demo
    python3 tools/generate_license.py --device-id 9774d56d682e549c --rental-months 1
    python3 tools/generate_license.py --device-id 9774d56d682e549c --rental-months 3

El codigo resultante se le pasa al usuario (por WhatsApp, SMS, etc.) para que
lo escriba en la app. No requiere conexion a internet ni servidor: el codigo
lleva su propia firma y fecha de vencimiento.

IMPORTANTE: SECRET_HEX debe coincidir EXACTAMENTE con
`LicenseSecret.HEX` en app/src/main/java/com/autoreply/bot/license/LicenseSecret.kt.
Si cambias uno, cambia el otro y recompila la app. Guarda este script en un
lugar privado: quien lo tenga puede generar codigos validos para cualquier
telefono.
"""

import argparse
import datetime
import hashlib
import hmac
import sys

# Debe coincidir con LicenseSecret.HEX en la app.
SECRET_HEX = "8430ee64d95fc201eec30053e36a007919ff236a16e4733c0c80d11351aad6b6"

CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

PLAN_DEMO = 0
PLAN_RENTAL = 1

EPOCH = datetime.date(1970, 1, 1)


def today_epoch_day() -> int:
    return (datetime.date.today() - EPOCH).days


def epoch_day_to_date(epoch_day: int) -> datetime.date:
    return EPOCH + datetime.timedelta(days=epoch_day)


def device_hash(device_id: str) -> bytes:
    return hashlib.sha256(device_id.encode("utf-8")).digest()[:4]


def crockford_encode(data: bytes) -> str:
    bit_buffer = 0
    bit_count = 0
    out = []
    for byte in data:
        bit_buffer = (bit_buffer << 8) | byte
        bit_count += 8
        while bit_count >= 5:
            bit_count -= 5
            index = (bit_buffer >> bit_count) & 0x1F
            out.append(CROCKFORD_ALPHABET[index])
    if bit_count > 0:
        index = (bit_buffer << (5 - bit_count)) & 0x1F
        out.append(CROCKFORD_ALPHABET[index])
    return "".join(out)


def build_code(device_id: str, expires_epoch_day: int, plan: int) -> str:
    if not (0 <= expires_epoch_day <= 0xFFFF):
        raise ValueError("expires_epoch_day fuera de rango (16 bits)")

    payload = bytearray()
    payload += device_hash(device_id)
    payload += expires_epoch_day.to_bytes(2, "big")
    payload.append(plan)

    secret = bytes.fromhex(SECRET_HEX)
    mac = hmac.new(secret, bytes(payload), hashlib.sha256).digest()[:5]

    raw = crockford_encode(bytes(payload) + mac)
    groups = [raw[i:i + 5] for i in range(0, len(raw), 5)]
    return "-".join(groups)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Genera codigos de activacion para AutoReply.")
    parser.add_argument(
        "--device-id",
        required=True,
        help='ID del telefono, tal como aparece en la app (pantalla Inicio, "ID de este telefono").',
    )
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--demo", action="store_true", help="Demo de 1 dia.")
    group.add_argument("--rental-months", type=int, help="Alquiler de N meses (cada mes = 30 dias).")
    group.add_argument("--days", type=int, help="Numero de dias exacto (para casos especiales/pruebas).")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    if args.demo:
        plan = PLAN_DEMO
        days = 1
    elif args.rental_months is not None:
        if args.rental_months <= 0:
            sys.exit("--rental-months debe ser mayor que 0")
        plan = PLAN_RENTAL
        days = args.rental_months * 30
    else:
        if args.days <= 0:
            sys.exit("--days debe ser mayor que 0")
        plan = PLAN_RENTAL
        days = args.days

    expires_epoch_day = today_epoch_day() + days - 1  # el dia de hoy cuenta como dia 1
    code = build_code(args.device_id, expires_epoch_day, plan)

    print(f"Dispositivo:  {args.device_id}")
    print(f"Plan:         {'Demo' if plan == PLAN_DEMO else 'Alquiler'}")
    print(f"Vence:        {epoch_day_to_date(expires_epoch_day).isoformat()} ({days} dia(s))")
    print(f"Codigo:       {code}")


if __name__ == "__main__":
    main()
