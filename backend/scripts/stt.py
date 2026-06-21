"""Speech-to-text script using Baidu ASR (free tier, 50k calls/day).
Usage: python stt.py <wav_file_path>
Output: recognized text to stdout, or error to stderr
"""
import sys
import base64
import json
import urllib.request
import urllib.parse

API_KEY = "JWakQfpUUk2giA6Blqje4T5H"
SECRET_KEY = "8sMaKsDGKmSOyuFsGd5FaJQUm2gaTq7U"

TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
ASR_URL = "https://vop.baidu.com/server_api"


def get_access_token():
    params = {
        "grant_type": "client_credentials",
        "client_id": API_KEY,
        "client_secret": SECRET_KEY,
    }
    url = TOKEN_URL + "?" + urllib.parse.urlencode(params)
    print("[stt.py] getting Baidu OAuth token...", file=sys.stderr)
    with urllib.request.urlopen(url, timeout=10) as resp:
        data = json.loads(resp.read())
    token = data.get("access_token")
    if not token:
        raise Exception("Failed to get token: " + str(data))
    print("[stt.py] token obtained successfully", file=sys.stderr)
    return token


def recognize(audio_path, token):
    with open(audio_path, "rb") as f:
        audio_data = f.read()
    print(f"[stt.py] audio file read: {audio_path}, size={len(audio_data)}bytes", file=sys.stderr)

    audio_base64 = base64.b64encode(audio_data).decode("utf-8")
    audio_len = len(audio_data)
    print(f"[stt.py] calling Baidu ASR API...", file=sys.stderr)

    payload = json.dumps({
        "format": "wav",
        "rate": 16000,
        "channel": 1,
        "cuid": "kaoyan-peipao",
        "token": token,
        "speech": audio_base64,
        "len": audio_len,
    }).encode("utf-8")

    req = urllib.request.Request(ASR_URL, data=payload)
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=15) as resp:
        result = json.loads(resp.read())

    if result.get("err_no") != 0:
        raise Exception("ASR error {}: {}".format(
            result.get("err_no"), result.get("err_msg")))

    text = " ".join(result.get("result", []))
    print(f"[stt.py] Baidu ASR result: \"{text}\"", file=sys.stderr)
    return text


def main():
    if len(sys.argv) < 2:
        print("ERROR: Missing audio file path", file=sys.stderr)
        sys.exit(1)

    audio_path = sys.argv[1]

    try:
        token = get_access_token()
        text = recognize(audio_path, token)
        print(text)
    except Exception as e:
        print("ERROR: " + str(e), file=sys.stderr)
        sys.exit(3)


if __name__ == "__main__":
    main()